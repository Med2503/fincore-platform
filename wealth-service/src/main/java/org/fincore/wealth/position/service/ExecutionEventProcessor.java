package org.fincore.wealth.position.service;

import org.fincore.wealth.portfolio.application.port.PortfolioPositionLock;
import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.application.port.ProcessedEventRepository;
import org.fincore.wealth.position.domain.ExecutionEvent;
import org.fincore.wealth.position.domain.Position;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ExecutionEventProcessor {

    private final PositionRepository positionRepository;
    private final ProcessedEventRepository processedEvents;
    private final PortfolioPositionLock portfolioPositionLock;

    public ExecutionEventProcessor(
            PositionRepository positionRepository,
            ProcessedEventRepository processedEvents,
            PortfolioPositionLock portfolioPositionLock
    ) {
        this.positionRepository = positionRepository;
        this.processedEvents = processedEvents;
        this.portfolioPositionLock = portfolioPositionLock;
    }

    @Transactional
    public ProcessingResult process(ExecutionEvent event) {
        validateEvent(event);

        if (!processedEvents.claim(event.eventId())) {
            return ProcessingResult.DUPLICATE;
        }

        portfolioPositionLock.lockForPositionUpdate(
                event.portfolioId()
        );

        Optional<Position> existingPosition =
                positionRepository.findForUpdate(
                        event.portfolioId(),
                        event.assetId()
                );

        Position current = existingPosition.orElse(null);

        if (event.side() == ExecutionEvent.Side.BUY) {
            processBuy(event, current);
        } else {
            processSell(event, current);
        }

        return ProcessingResult.APPLIED;
    }

    private void processBuy(
            ExecutionEvent event,
            Position current
    ) {
        Position updated = current == null
                ? Position.open(
                event.portfolioId(),
                event.assetId(),
                event.quantity(),
                event.executionPrice(),
                event.fees(),
                event.currency(),
                event.occurredAt()
        )
                : current.buy(
                event.quantity(),
                event.executionPrice(),
                event.fees(),
                event.currency(),
                event.occurredAt()
        );

        positionRepository.save(updated);
    }

    private void processSell(
            ExecutionEvent event,
            Position current
    ) {
        if (current == null) {
            throw new IllegalStateException(
                    "Cannot sell an absent position"
            );
        }

        Position.SaleResult result = current.sell(
                event.quantity(),
                event.executionPrice(),
                event.fees(),
                event.currency(),
                event.occurredAt()
        );

        if (result.fullyClosed()) {
            positionRepository.delete(current);
        } else {
            positionRepository.save(result.remainingPosition());
        }
    }

    private void validateEvent(ExecutionEvent event) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "Event is required"
            );
        }

        if (event.eventId() == null) {
            throw new IllegalArgumentException(
                    "Event ID is required"
            );
        }

        if (event.portfolioId() == null || event.assetId() == null) {
            throw new IllegalArgumentException(
                    "Portfolio ID and asset ID are required"
            );
        }
    }

    public enum ProcessingResult {
        APPLIED,
        DUPLICATE
    }
}