package org.fincore.wealth.position.application.event;


import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class PositionOutboxEventFactory {

    private static final String EVENT_TYPE = "PositionChanged";

    private final ObjectMapper objectMapper;

    public PositionOutboxEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OutboxEvent create(Position position) {
        PositionChangedEvent event =
                new PositionChangedEvent(
                        UUID.randomUUID(),
                        position.portfolioId(),
                        position.assetId(),
                        position.quantity(),
                        position.averageCost(),
                        position.currency(),
                        position.lastExecutionSequence(),
                        position.updatedAt()
                );

        return toOutboxEvent(
                position.portfolioId(),
                event,
                position.updatedAt()
        );
    }

    public OutboxEvent createClosed(
            Position position,
            Instant occurredAt,
            long executionSequence
    ) {
        PositionChangedEvent event =
                new PositionChangedEvent(
                        UUID.randomUUID(),
                        position.portfolioId(),
                        position.assetId(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        position.currency(),
                        executionSequence,
                        occurredAt
                );

        return toOutboxEvent(
                position.portfolioId(),
                event,
                occurredAt
        );
    }

    private OutboxEvent toOutboxEvent(
            UUID aggregateId,
            PositionChangedEvent event,
            Instant occurredAt
    ) {
        try {
            String payload =
                    objectMapper.writeValueAsString(event);

            return OutboxEvent.pending(
                    aggregateId,
                    EVENT_TYPE,
                    payload,
                    occurredAt
            );
        } catch (JsonParseException exception) {
            throw new IllegalStateException(
                    "Cannot serialize position changed event",
                    exception
            );
        }
    }
}