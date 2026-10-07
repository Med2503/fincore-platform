package org.fincore.wealth.position.outbox.infrastructure.persistence;

import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

        mapper.updateEntity(
                entity,
                event
        );

        repository.save(entity);
    }

    @Override
    @Transactional
    public List<OutboxEvent> claimBatch(
            Instant now,
            Instant lockedUntil,
            UUID workerId,
            int batchSize
    ) {
        List<OutboxEventJpaEntity> entities =
                repository.findClaimable(
                        now,
                        batchSize
                );

        List<OutboxEvent> claimed =
                entities.stream()
                        .map(mapper::toDomain)
                        .map(event ->
                                event.claim(
                                        workerId,
                                        lockedUntil
                                )
                        )
                        .toList();

        for (OutboxEvent event : claimed) {
            OutboxEventJpaEntity entity =
                    repository.findById(event.id())
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Outbox event disappeared: "
                                                    + event.id()
                                    )
                            );

            mapper.updateEntity(
                    entity,
                    event
            );
        }

        repository.flush();

        return claimed;
    }
}