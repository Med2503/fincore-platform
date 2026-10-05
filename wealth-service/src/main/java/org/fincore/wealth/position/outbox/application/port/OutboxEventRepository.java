package org.fincore.wealth.position.outbox.application.port;

import org.fincore.wealth.position.outbox.domain.OutboxEvent;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    void update(OutboxEvent event);

    List<OutboxEvent> findPending(
            Instant now,
            int batchSize
    );
}
