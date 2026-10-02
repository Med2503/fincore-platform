package org.fincore.wealth.position.application.port;


import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.application.port.ProcessedEventRepository;
import org.fincore.wealth.position.domain.ExecutionEvent;
import org.fincore.wealth.position.domain.Position;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionEventProcessor {

    private final PositionRepository positions;
    private final ProcessedEventRepository processedEvents;

    public ExecutionEventProcessor(
            PositionRepository positions,
            ProcessedEventRepository processedEvents
    ) {
        this.positions = positions;
        this.processedEvents = processedEvents;
    }

    @Transactional
    public ProcessingResult process(ExecutionEvent event) {
        validateEvent(event);

        boolean claimed = processedEvents.claim(event.eventId());

        if (!claimed) {
            return ProcessingResult.DUPLICATE;
        }

        var existing = positions.findForUpdate(
                event.portfolioId(),
                event.assetId()
        );

        if (event.side() == ExecutionEvent.Side.BUY) {
            processBuy(event, existing.orElse(null));
        } else {
            processSell(event, existing.orElse(null));
        }

        return ProcessingResult.APPLIED;
    }

    private void processBuy(ExecutionEvent event, Position current) {
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

        positions.save(updated);
    }

    private void processSell(ExecutionEvent event, Position current) {
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
            positions.delete(current);
        } else {
            positions.save(result.remainingPosition());
        }
    }

    private void validateEvent(ExecutionEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event is required");
        }
        if (event.eventId() == null) {
            throw new IllegalArgumentException("Event ID is required");
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