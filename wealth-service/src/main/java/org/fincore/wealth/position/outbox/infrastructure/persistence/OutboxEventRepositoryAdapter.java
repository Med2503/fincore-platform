package org.fincore.wealth.position.outbox.infrastructure.persistence;


import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.springframework.stereotype.Repository;

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
}