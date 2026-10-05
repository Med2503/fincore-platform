package org.fincore.wealth.position.outbox.infrastructure.persistence;

import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.domain.OutboxEventStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public class OutboxEventRepositoryAdapter
        implements OutboxEventRepository {

    private final SpringDataOutboxEventRepository repository;
    private final OutboxEventPersistenceMapper mapper;

    public OutboxEventRepositoryAdapter(
            SpringDataOutboxEventRepository repository,
            OutboxEventPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OutboxEvent save(
            OutboxEvent event
    ) {
        OutboxEventJpaEntity entity =
                mapper.toEntity(event);

        OutboxEventJpaEntity saved =
                repository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void update(
            OutboxEvent event
    ) {
        OutboxEventJpaEntity entity =
                repository.findById(event.id())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Outbox event not found: "
                                                + event.id()
                                )
                        );

        mapper.updateEntity(entity, event);

        repository.save(entity);
    }

    @Override
    @Transactional
    public List<OutboxEvent> findPending(
            Instant now,
            int batchSize
    ) {
        return repository.findPendingForUpdate(
                        OutboxEventStatus.PENDING.name(),
                        now,
                        batchSize
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}