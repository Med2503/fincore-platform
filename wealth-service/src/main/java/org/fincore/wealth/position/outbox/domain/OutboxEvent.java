package org.fincore.wealth.position.outbox.domain;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        UUID aggregateId,
        String eventType,
        String payload,
        Instant occurredAt,
        Instant publishedAt,
        OutboxEventStatus status,
        int retryCount
) {

    public OutboxEvent {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Outbox event ID is required"
            );
        }

        if (aggregateId == null) {
            throw new IllegalArgumentException(
                    "Aggregate ID is required"
            );
        }

        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException(
                    "Event type is required"
            );
        }

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Payload is required"
            );
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "Occurred at is required"
            );
        }

        if (retryCount < 0) {
            throw new IllegalArgumentException(
                    "Retry count cannot be negative"
            );
        }
    }

    public static OutboxEvent pending(
            UUID aggregateId,
            String eventType,
            String payload,
            Instant occurredAt
    ) {
        return new OutboxEvent(
                UUID.randomUUID(),
                aggregateId,
                eventType,
                payload,
                occurredAt,
                null,
                OutboxEventStatus.PENDING,
                0
        );
    }

    public OutboxEvent markPublished(
            Instant publishedAt
    ) {
        return new OutboxEvent(
                id,
                aggregateId,
                eventType,
                payload,
                occurredAt,
                publishedAt,
                OutboxEventStatus.PUBLISHED,
                retryCount
        );
    }

    public OutboxEvent markFailed() {
        return new OutboxEvent(
                id,
                aggregateId,
                eventType,
                payload,
                occurredAt,
                publishedAt,
                OutboxEventStatus.FAILED,
                retryCount + 1
        );
    }
}