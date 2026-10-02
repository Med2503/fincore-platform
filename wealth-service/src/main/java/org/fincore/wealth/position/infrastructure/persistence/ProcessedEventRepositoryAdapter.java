package org.fincore.wealth.position.infrastructure.persistence;


import org.fincore.wealth.position.application.port.ProcessedEventRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class ProcessedEventRepositoryAdapter implements ProcessedEventRepository {


    private final ProcessedEventInsertAdapter insertAdapter;

    public ProcessedEventRepositoryAdapter(
            ProcessedEventInsertAdapter insertAdapter
    ) {
        this.insertAdapter = insertAdapter;
    }


    @Override
    public boolean claim(UUID eventId) {
        return insertAdapter.insertIfAbsent(eventId);
    }

    /*
     * en bas la premiére methode on l'as changé car ne vérifie pas existsBy et save n'est pas
     * atomique bas un bloc de transaction donc en concurrence ca serait difficile  => on
     * vas check via postgres
     *
     * */


 /*   private final SpringDataProcessedEventRepository repository;

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
    */

}