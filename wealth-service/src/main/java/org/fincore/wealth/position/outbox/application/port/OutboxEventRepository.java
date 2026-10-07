package org.fincore.wealth.position.outbox.application.port;



import org.fincore.wealth.position.outbox.domain.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    void update(OutboxEvent event);

    List<OutboxEvent> claimBatch(
            Instant now,
            Instant lockedUntil,
            UUID workerId,
            int batchSize
    );
}