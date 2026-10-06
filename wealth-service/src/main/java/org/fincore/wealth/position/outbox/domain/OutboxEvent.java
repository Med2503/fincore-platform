package org.fincore.wealth.position.outbox.domain;

import org.fincore.wealth.position.outbox.domain.OutboxEventStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        UUID aggregateId,
        String eventType,
        String payload,
        Instant occurredAt,
        Instant publishedAt,
        OutboxEventStatus status,
        int retryCount,
        Instant nextAttemptAt
) {

    public OutboxEvent {
        Objects.requireNonNull(id, "ID is required");
        Objects.requireNonNull(
                aggregateId,
                "Aggregate ID is required"
        );
        Objects.requireNonNull(
                eventType,
                "Event type is required"
        );
        Objects.requireNonNull(
                payload,
                "Payload is required"
        );
        Objects.requireNonNull(
                occurredAt,
                "Occurred at is required"
        );
        Objects.requireNonNull(
                status,
                "Status is required"
        );

        if (retryCount < 0) {
            throw new IllegalArgumentException(
                    "Retry count cannot be negative"
            );
        }
    }

    public static OutboxEvent pending(
            UUID id,
            UUID aggregateId,
            String eventType,
            String payload,
            Instant occurredAt
    ) {
        return new OutboxEvent(
                id,
                aggregateId,
                eventType,
                payload,
                occurredAt,
                null,
                OutboxEventStatus.PENDING,
                0,
                occurredAt
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
                retryCount,
                null
        );
    }

    public OutboxEvent markFailed(
            Instant nextAttemptAt,
            boolean permanentlyFailed
    ) {
        return new OutboxEvent(
                id,
                aggregateId,
                eventType,
                payload,
                occurredAt,
                null,
                permanentlyFailed
                        ? OutboxEventStatus.FAILED
                        : OutboxEventStatus.PENDING,
                retryCount + 1,
                nextAttemptAt
        );
    }
}