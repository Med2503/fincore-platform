
package org.fincore.wealth.position.outbox.service;

import org.fincore.wealth.position.outbox.application.port.EventPublisher;
import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.application.service.PublishOutboxEvents;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.domain.OutboxEventStatus;
import org.fincore.wealth.position.outbox.infrastructure.config.OutboxPublisherProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublishOutboxEventsTest {

    private static final Instant NOW =
            Instant.parse("2026-09-30T10:00:00Z");

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 10;
    private static final Duration LEASE_DURATION =
            Duration.ofSeconds(30);

    private OutboxEventRepository repository;
    private EventPublisher publisher;
    private PublishOutboxEvents service;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        publisher = mock(EventPublisher.class);

        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        OutboxPublisherProperties properties =
                new OutboxPublisherProperties(
                        Duration.ofSeconds(2),
                        BATCH_SIZE,
                        MAX_RETRIES,
                        LEASE_DURATION
                );

        service = new PublishOutboxEvents(
                repository,
                publisher,
                properties,
                clock
        );
    }

    @Test
    void shouldClaimAndPublishEvents() {
        OutboxEvent event = createClaimedEvent();

        stubClaimBatch(List.of(event));
        when(publisher.publish(
                event.eventType(),
                event.payload()
        )).thenReturn(true);

        int published = service.publishBatch();

        assertEquals(1, published);

        verify(publisher).publish(
                event.eventType(),
                event.payload()
        );

        verify(repository).update(
                eq(event.markPublished(NOW))
        );
    }

    @Test
    void shouldReleaseLeaseAfterSuccessfulPublication() {
        OutboxEvent event = createClaimedEvent();

        stubClaimBatch(List.of(event));
        when(publisher.publish(
                event.eventType(),
                event.payload()
        )).thenReturn(true);

        service.publishBatch();

        OutboxEvent expected = event.markPublished(NOW);

        assertEquals(OutboxEventStatus.PUBLISHED, expected.status());
        assertEquals(NOW, expected.publishedAt());
        assertEquals(null, expected.claimedBy());
        assertEquals(null, expected.lockedUntil());

        verify(repository).update(expected);
    }

    @Test
    void shouldReturnZeroWhenNoEventsAreClaimed() {
        stubClaimBatch(List.of());

        int published = service.publishBatch();

        assertEquals(0, published);

        verify(publisher, never()).publish(any(), any());
        verify(repository, never()).update(any(OutboxEvent.class));
    }

    @Test
    void shouldScheduleRetryWhenPublisherReturnsFalse() {
        OutboxEvent event = createClaimedEvent();

        stubClaimBatch(List.of(event));
        when(publisher.publish(
                event.eventType(),
                event.payload()
        )).thenReturn(false);

        int published = service.publishBatch();

        assertEquals(0, published);

        OutboxEvent expected = event.markFailed(
                NOW.plusSeconds(1),
                false
        );

        assertEquals(OutboxEventStatus.PENDING, expected.status());
        assertEquals(1, expected.retryCount());
        assertEquals(NOW.plusSeconds(1), expected.nextAttemptAt());
        assertEquals(null, expected.claimedBy());
        assertEquals(null, expected.lockedUntil());

        verify(repository).update(expected);
    }

    @Test
    void shouldScheduleRetryWhenPublisherThrows() {
        OutboxEvent event = createClaimedEvent();

        stubClaimBatch(List.of(event));
        when(publisher.publish(
                event.eventType(),
                event.payload()
        )).thenThrow(
                new IllegalStateException("Kafka unavailable")
        );

        int published = service.publishBatch();

        assertEquals(0, published);

        OutboxEvent expected = event.markFailed(
                NOW.plusSeconds(1),
                false
        );

        assertEquals(OutboxEventStatus.PENDING, expected.status());
        assertEquals(1, expected.retryCount());
        assertEquals(null, expected.claimedBy());
        assertEquals(null, expected.lockedUntil());

        verify(repository).update(expected);
    }

    @Test
    void shouldMarkEventAsPermanentlyFailedOnLastRetry() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PositionChanged",
                "{}",
                NOW,
                null,
                OutboxEventStatus.PROCESSING,
                MAX_RETRIES - 1,
                NOW,
                UUID.randomUUID(),
                NOW.plus(LEASE_DURATION)
        );

        stubClaimBatch(List.of(event));
        when(publisher.publish(
                event.eventType(),
                event.payload()
        )).thenReturn(false);

        int published = service.publishBatch();

        assertEquals(0, published);

        OutboxEvent expected = event.markFailed(
                NOW.plusSeconds(60),
                true
        );

        assertEquals(OutboxEventStatus.FAILED, expected.status());
        assertEquals(MAX_RETRIES, expected.retryCount());
        assertEquals(null, expected.nextAttemptAt());
        assertEquals(null, expected.claimedBy());
        assertEquals(null, expected.lockedUntil());

        verify(repository).update(expected);
    }

    @Test
    void shouldPublishMultipleEvents() {
        OutboxEvent first = createClaimedEvent();
        OutboxEvent second = createClaimedEvent();

        stubClaimBatch(List.of(first, second));
        when(publisher.publish(any(), any())).thenReturn(true);

        int published = service.publishBatch();

        assertEquals(2, published);

        verify(publisher).publish(
                first.eventType(),
                first.payload()
        );

        verify(publisher).publish(
                second.eventType(),
                second.payload()
        );

        verify(repository).update(first.markPublished(NOW));
        verify(repository).update(second.markPublished(NOW));
    }

    @Test
    void shouldUseConfiguredLeaseDurationWhenClaiming() {
        stubClaimBatch(List.of());

        service.publishBatch();

        verify(repository).claimBatch(
                eq(NOW),
                eq(NOW.plus(LEASE_DURATION)),
                any(UUID.class),
                eq(BATCH_SIZE)
        );
    }

    @Test
    void shouldUseConfiguredBatchSize() {
        stubClaimBatch(List.of());

        service.publishBatch();

        verify(repository).claimBatch(
                eq(NOW),
                eq(NOW.plus(LEASE_DURATION)),
                any(UUID.class),
                eq(BATCH_SIZE)
        );
    }

    private void stubClaimBatch(List<OutboxEvent> events) {
        when(repository.claimBatch(
                eq(NOW),
                eq(NOW.plus(LEASE_DURATION)),
                any(UUID.class),
                eq(BATCH_SIZE)
        )).thenReturn(events);
    }

    private OutboxEvent createClaimedEvent() {
        return OutboxEvent.pending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PositionChanged",
                "{\"quantity\":10}",
                NOW
        ).claim(
                UUID.randomUUID(),
                NOW.plus(LEASE_DURATION)
        );
    }
}
