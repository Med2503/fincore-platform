package org.fincore.wealth.position.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class PositionOutboxEventFactory {

    private static final String EVENT_TYPE =
            "PositionChanged";

    private final ObjectMapper objectMapper;

    public PositionOutboxEventFactory(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    public OutboxEvent create(
            Position position,
            long executionSequence,
            Instant occurredAt
    ) {
        UUID eventId = UUID.randomUUID();

        PositionChangedEvent event =
                new PositionChangedEvent(
                        eventId,
                        position.portfolioId(),
                        position.assetId(),
                        position.quantity(),
                        position.averageCost(),
                        position.currency(),
                        executionSequence,
                        occurredAt
                );

        return OutboxEvent.pending(
                eventId,
                position.portfolioId(),
                EVENT_TYPE,
                serialize(event),
                occurredAt
        );
    }

    public OutboxEvent createClosed(
            UUID portfolioId,
            UUID assetId,
            String currency,
            long executionSequence,
            Instant occurredAt
    ) {
        UUID eventId = UUID.randomUUID();

        PositionChangedEvent event =
                new PositionChangedEvent(
                        eventId,
                        portfolioId,
                        assetId,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        currency,
                        executionSequence,
                        occurredAt
                );

        return OutboxEvent.pending(
                eventId,
                portfolioId,
                EVENT_TYPE,
                serialize(event),
                occurredAt
        );
    }

    private String serialize(
            PositionChangedEvent event
    ) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to serialize PositionChanged event",
                    exception
            );
        }
    }
}