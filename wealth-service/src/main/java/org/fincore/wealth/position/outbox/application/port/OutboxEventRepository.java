package org.fincore.wealth.position.outbox.application.port;

import org.fincore.wealth.position.outbox.domain.OutboxEvent;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);
}
