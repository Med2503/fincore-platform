package org.fincore.wealth.position.outbox.application.service;

import org.fincore.wealth.position.outbox.application.port.EventPublisher;
import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.infrastructure.config.OutboxPublisherProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;


@Service
public class PublishOutboxEvents {

    private final OutboxEventRepository repository;
    private final EventPublisher publisher;
    private final OutboxPublisherProperties properties;
    private final Clock clock;

    public PublishOutboxEvents(
            OutboxEventRepository repository,
            EventPublisher publisher,
            OutboxPublisherProperties properties,
            Clock clock
    ) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void publish(OutboxEvent event) {
        boolean published =
                publisher.publish(
                        event.eventType(),
                        event.payload()
                );

        if (published) {
            repository.update(
                    event.markPublished(
                            clock.instant()
                    )
            );
            return;
        }

        handleFailure(event);
    }

    private void handleFailure(OutboxEvent event) {
        int nextRetry = event.retryCount() + 1;

        boolean permanentlyFailed =
                nextRetry >= properties.getMaxRetries();

        Instant nextAttempt =
                permanentlyFailed
                        ? null
                        : clock.instant()
                        .plus(calculateBackoff(nextRetry));

        repository.update(
                event.markFailed(
                        nextAttempt,
                        permanentlyFailed
                )
        );
    }

    private java.time.Duration calculateBackoff(
            int retryCount
    ) {
        long seconds =
                Math.min(
                        60,
                        1L << Math.min(retryCount, 6)
                );

        return java.time.Duration.ofSeconds(seconds);
    }
}