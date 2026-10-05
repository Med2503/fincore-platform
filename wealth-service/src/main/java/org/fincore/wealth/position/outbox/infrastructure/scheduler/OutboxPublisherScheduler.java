package org.fincore.wealth.position.outbox.infrastructure.scheduler;

import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.application.service.PublishOutboxEvents;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.infrastructure.config.OutboxPublisherProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisherScheduler {

    private final OutboxEventRepository repository;
    private final PublishOutboxEvents publisher;
    private final OutboxPublisherProperties properties;
    private final Clock clock;

    public OutboxPublisherScheduler(
            OutboxEventRepository repository,
            PublishOutboxEvents publisher,
            OutboxPublisherProperties properties,
            Clock clock
    ) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString =
                    "${wealth.outbox.publisher.fixed-delay:2000}"
    )
    public void publishPendingEvents() {
        Instant now = clock.instant();

        List<OutboxEvent> events =
                repository.findPending(
                        now,
                        properties.getBatchSize()
                );

        events.forEach(publisher::publish);
    }
}
