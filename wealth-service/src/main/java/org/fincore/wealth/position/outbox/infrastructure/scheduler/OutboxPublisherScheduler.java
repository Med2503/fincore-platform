package org.fincore.wealth.position.outbox.infrastructure.scheduler;

import org.fincore.wealth.position.outbox.application.service.PublishOutboxEvents;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisherScheduler {

    private final PublishOutboxEvents publisher;

    public OutboxPublisherScheduler(
            PublishOutboxEvents publisher
    ) {
        this.publisher = publisher;
    }

    @Scheduled(
            fixedDelayString =
                    "${wealth.outbox.publisher.fixed-delay}"
    )
    public void publish() {
        publisher.publishBatch();
    }
}