package org.fincore.wealth.position.outbox.application.service;

import org.fincore.wealth.position.outbox.application.port.EventPublisher;
import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.infrastructure.config.OutboxPublisherProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    public int publishBatch() {
        Instant now = clock.instant();

        UUID workerId = UUID.randomUUID();

        Instant lockedUntil =
                now.plus(
                        properties.leaseDuration()
                );

        List<OutboxEvent> events =
                repository.claimBatch(
                        now,
                        lockedUntil,
                        workerId,
                        properties.batchSize()
                );

        int published = 0;

        for (OutboxEvent event : events) {
            if (publish(event)) {
                published++;
            }
        }

        return published;
    }

    private boolean publish(
            OutboxEvent event
    ) {
        try {
            boolean sent =
                    publisher.publish(
                            event.eventType(),
                            event.payload()
                    );

            if (!sent) {
                handleFailure(event);
                return false;
            }

            repository.update(
                    event.markPublished(
                            clock.instant()
                    )
            );

            return true;

        } catch (RuntimeException exception) {
            handleFailure(event);
            return false;
        }
    }

    private void handleFailure(
            OutboxEvent event
    ) {
        int nextRetry =
                event.retryCount() + 1;

        boolean permanentlyFailed =
                nextRetry >=
                        properties.maxRetries();

        Instant nextAttemptAt =
                clock.instant()
                        .plus(
                                calculateBackoff(
                                        nextRetry
                                )
                        );

        repository.update(
                event.markFailed(
                        nextAttemptAt,
                        permanentlyFailed
                )
        );
    }

    private java.time.Duration calculateBackoff(
            int retry
    ) {
        long seconds =
                Math.min(
                        60,
                        1L << Math.min(
                                retry - 1,
                                6
                        )
                );

        return java.time.Duration.ofSeconds(
                seconds
        );
    }
}