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


        if (current != null
                && event.executionSequence()
                <= current.lastExecutionSequence()) {

            return ProcessingResult.OUT_OF_ORDER;
        }

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
        Position updated;

        if (current == null) {
            updated = Position.open(
                    event.portfolioId(),
                    event.assetId(),
                    event.quantity(),
                    event.executionPrice(),
                    event.fees(),
                    event.currency(),
                    event.occurredAt(),
                    event.executionSequence()
            );
        } else {
            updated = current.buy(
                    event.quantity(),
                    event.executionPrice(),
                    event.fees(),
                    event.currency(),
                    event.occurredAt(),
                    event.executionSequence()
            );
        }

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
                event.occurredAt(),
                event.executionSequence()
        );

        if (result.fullyClosed()) {
            positionRepository.delete(current);
            return;
        }

        positionRepository.save(
                result.remainingPosition()
        );
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

        if (event.executionId() == null) {
            throw new IllegalArgumentException(
                    "Execution ID is required"
            );
        }

        if (event.portfolioId() == null) {
            throw new IllegalArgumentException(
                    "Portfolio ID is required"
            );
        }

        if (event.assetId() == null) {
            throw new IllegalArgumentException(
                    "Asset ID is required"
            );
        }

        if (event.side() == null) {
            throw new IllegalArgumentException(
                    "Execution side is required"
            );
        }

        if (event.quantity() == null
                || event.quantity().signum() <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be positive"
            );
        }

        if (event.executionPrice() == null
                || event.executionPrice().signum() < 0) {

            throw new IllegalArgumentException(
                    "Execution price cannot be negative"
            );
        }

        if (event.fees() == null
                || event.fees().signum() < 0) {

            throw new IllegalArgumentException(
                    "Fees cannot be negative"
            );
        }

        if (event.currency() == null
                || event.currency().isBlank()) {

            throw new IllegalArgumentException(
                    "Currency is required"
            );
        }

        if (event.occurredAt() == null) {
            throw new IllegalArgumentException(
                    "Occurred at is required"
            );
        }

        if (event.executionSequence() <= 0) {
            throw new IllegalArgumentException(
                    "Execution sequence must be positive"
            );
        }
    }

    public enum ProcessingResult {
        APPLIED,
        DUPLICATE,
        OUT_OF_ORDER
    }
}
