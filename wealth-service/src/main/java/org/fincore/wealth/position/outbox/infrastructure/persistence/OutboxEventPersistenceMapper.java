package org.fincore.wealth.position.outbox.infrastructure.persistence;

import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.infrastructure.persistence.OutboxEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPersistenceMapper {

    public OutboxEventJpaEntity toEntity(
            OutboxEvent event
    ) {
        OutboxEventJpaEntity entity =
                new OutboxEventJpaEntity();

        entity.setId(event.id());
        entity.setAggregateId(event.aggregateId());
        entity.setEventType(event.eventType());
        entity.setPayload(event.payload());
        entity.setOccurredAt(event.occurredAt());
        entity.setPublishedAt(event.publishedAt());
        entity.setStatus(event.status());
        entity.setRetryCount(event.retryCount());
        entity.setNextAttemptAt(event.nextAttemptAt());
        entity.setClaimedBy(event.claimedBy());
        entity.setLockedUntil(event.lockedUntil());

        return entity;
    }

    public OutboxEvent toDomain(
            OutboxEventJpaEntity entity
    ) {
        return new OutboxEvent(
                entity.getId(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getOccurredAt(),
                entity.getPublishedAt(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getNextAttemptAt(),
                entity.getClaimedBy(),
                entity.getLockedUntil()
        );
    }

    public void updateEntity(
            OutboxEventJpaEntity entity,
            OutboxEvent event
    ) {
        entity.setPublishedAt(
                event.publishedAt()
        );

        entity.setStatus(
                event.status()
        );

        entity.setRetryCount(
                event.retryCount()
        );

        entity.setNextAttemptAt(
                event.nextAttemptAt()
        );

        entity.setClaimedBy(
                event.claimedBy()
        );

        entity.setLockedUntil(
                event.lockedUntil()
        );
    }
}