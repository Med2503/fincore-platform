package org.fincore.wealth.position.infrastructure.persistence;


import org.fincore.wealth.position.application.port.ProcessedEventRepository;
import org.fincore.wealth.position.infrastructure.persistence.ProcessedWealthEventJpaEntity;
import org.fincore.wealth.position.infrastructure.persistence.SpringDataProcessedEventRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class ProcessedEventRepositoryAdapter implements ProcessedEventRepository {
    private final SpringDataProcessedEventRepository repository;

    public ProcessedEventRepositoryAdapter(
            SpringDataProcessedEventRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public boolean alreadyProcessed(UUID eventId) {
        return repository.existsById(eventId);
    }

    @Override
    public void markProcessed(UUID eventId) {
        repository.save(new ProcessedWealthEventJpaEntity(eventId, Instant.now()));
    }
}