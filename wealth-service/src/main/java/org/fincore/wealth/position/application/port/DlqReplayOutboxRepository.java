package org.fincore.wealth.position.application.port;

import java.time.Instant;
import java.util.UUID;

public interface DlqReplayOutboxRepository {

    void save(
            UUID eventId,
            String payload,
            Instant occurredAt
    );
}