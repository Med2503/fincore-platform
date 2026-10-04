package org.fincore.wealth.position.outbox.infrastructure.persistence;

import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.infrastructure.persistence.OutboxEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPersistenceMapper {

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
                entity.getRetryCount()
        );
    }

    public OutboxEventJpaEntity toEntity(
            OutboxEvent event
    ) {
        OutboxEventJpaEntity entity =
                new OutboxEventJpaEntity();

        entity.setId(event.id());
        entity.setAggregateId(
                event.aggregateId()
        );
        entity.setEventType(
                event.eventType()
        );
        entity.setPayload(
                event.payload()
        );
        entity.setOccurredAt(
                event.occurredAt()
        );
        entity.setPublishedAt(
                event.publishedAt()
        );
        entity.setStatus(
                event.status()
        );
        entity.setRetryCount(
                event.retryCount()
        );

        return entity;
    }
}