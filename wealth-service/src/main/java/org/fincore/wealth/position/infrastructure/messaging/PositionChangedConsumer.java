package org.fincore.wealth.position.infrastructure.messaging;

import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.fincore.wealth.position.application.port.ProcessedPositionChangedEventRepository;
import org.fincore.wealth.position.application.service.PositionChangedProjectionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PositionChangedConsumer {

    private final ProcessedPositionChangedEventRepository processedEvents;
    private final PositionChangedProjectionService projectionService;

    public PositionChangedConsumer(
            ProcessedPositionChangedEventRepository processedEvents,
            PositionChangedProjectionService projectionService
    ) {
        this.processedEvents = processedEvents;
        this.projectionService = projectionService;
    }

    @Transactional
    public void consume(PositionChangedEvent event) {
        validate(event);

        if (!processedEvents.claim(event.eventId())) {
            return;
        }

        projectionService.apply(event);
    }

    private void validate(PositionChangedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "Position changed event is required"
            );
        }

        if (event.eventId() == null) {
            throw new IllegalArgumentException(
                    "Event ID is required"
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

        if (event.quantity() == null
                || event.quantity().signum() < 0) {
            throw new IllegalArgumentException(
                    "Quantity cannot be negative"
            );
        }

        if (event.averageCost() == null
                || event.averageCost().signum() < 0) {
            throw new IllegalArgumentException(
                    "Average cost cannot be negative"
            );
        }

        if (event.currency() == null
                || event.currency().isBlank()) {
            throw new IllegalArgumentException(
                    "Currency is required"
            );
        }

        if (event.executionSequence() <= 0) {
            throw new IllegalArgumentException(
                    "Execution sequence must be positive"
            );
        }

        if (event.occurredAt() == null) {
            throw new IllegalArgumentException(
                    "Occurred at is required"
            );
        }
    }
}