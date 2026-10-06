package org.fincore.wealth.position.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fincore.wealth.position.application.port.DlqReplayOutboxRepository;

import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class DlqReplayOutboxRepositoryAdapter
        implements DlqReplayOutboxRepository {

    private static final String EVENT_TYPE =
            "PositionChanged";

    private final OutboxEventRepository outboxRepository;

    public DlqReplayOutboxRepositoryAdapter(
            OutboxEventRepository outboxRepository
    ) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void save(
            UUID eventId,
            String payload,
            Instant occurredAt
    ) {
        OutboxEvent event =
                OutboxEvent.pending(
                        eventId,
                        EVENT_TYPE,
                        payload,
                        occurredAt
                );

        outboxRepository.save(event);
    }
}