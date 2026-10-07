package org.fincore.wealth.position.outbox.infrastructure.scheduler;

import org.fincore.wealth.position.outbox.application.service.PublishOutboxEvents;
import org.fincore.wealth.position.outbox.infrastructure.config.OutboxPublisherProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisherScheduler {

    private final PublishOutboxEvents publisher;
    private final OutboxPublisherProperties properties;

    public OutboxPublisherScheduler(
            PublishOutboxEvents publisher,
            OutboxPublisherProperties properties
    ) {
        this.publisher = publisher;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString =
                    "${wealth.outbox.publisher.fixed-delay}"
    )
    public void publish() {
        publisher.publishBatch();
    }
}