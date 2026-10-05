package org.fincore.wealth.position.outbox.infrastructure.persistence;

import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.infrastructure.persistence.OutboxEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPersistenceMapper {

    public OutboxEvent toDomain(OutboxEventJpaEntity entity) {
        return new OutboxEvent(
                entity.getId(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getOccurredAt(),
                entity.getPublishedAt(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getNextAttemptAt()
        );
    }

    public OutboxEventJpaEntity toEntity(OutboxEvent domain) {
        OutboxEventJpaEntity entity =
                new OutboxEventJpaEntity();

        entity.setId(domain.id());
        entity.setAggregateId(domain.aggregateId());
        entity.setEventType(domain.eventType());
        entity.setPayload(domain.payload());
        entity.setOccurredAt(domain.occurredAt());
        entity.setPublishedAt(domain.publishedAt());
        entity.setStatus(domain.status());
        entity.setRetryCount(domain.retryCount());
        entity.setNextAttemptAt(domain.nextAttemptAt());

        return entity;
    }

    public void updateEntity(
            OutboxEventJpaEntity entity,
            OutboxEvent domain
    ) {
        entity.setPublishedAt(domain.publishedAt());
        entity.setStatus(domain.status());
        entity.setRetryCount(domain.retryCount());
        entity.setNextAttemptAt(domain.nextAttemptAt());
    }
}