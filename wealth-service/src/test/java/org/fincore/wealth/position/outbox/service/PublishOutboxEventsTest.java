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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublishOutboxEventsTest {

    private OutboxEventRepository repository;
    private EventPublisher publisher;
    private PublishOutboxEvents service;
    private Clock clock;

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-30T10:00:00Z"
            );

    @BeforeEach
    void setUp() {
        repository =
                mock(OutboxEventRepository.class);

        publisher =
                mock(EventPublisher.class);

        clock =
                Clock.fixed(
                        NOW,
                        ZoneOffset.UTC
                );

        OutboxPublisherProperties properties =
                new OutboxPublisherProperties(
                        Duration.ofSeconds(2),
                        50,
                        10,
                        Duration.ofSeconds(30)
                );

        service =
                new PublishOutboxEvents(
                        repository,
                        publisher,
                        properties,
                        clock
                );
    }

    @Test
    void shouldClaimAndPublishEvents() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        OutboxEvent event =
                OutboxEvent.pending(
                        eventId,
                        portfolioId,
                        "PositionChanged",
                        "{\"eventId\":\"" + eventId + "\"}",
                        NOW
                ).claim(
                        workerId,
                        NOW.plusSeconds(30)
                );

        when(
                repository.claimBatch(
                        eq(NOW),
                        eq(NOW.plusSeconds(30)),
                        any(UUID.class),
                        eq(50)
                )
        ).thenReturn(
                List.of(event)
        );

        when(
                publisher.publish(
                        "PositionChanged",
                        event.payload()
                )
        ).thenReturn(true);

        int published =
                service.publishBatch();

        assertEquals(
                1,
                published
        );

        verify(publisher).publish(
                "PositionChanged",
                event.payload()
        );

        verify(repository).update(
                eq(
                        event.markPublished(NOW)
                )
        );
    }

    @Test
    void shouldReturnZeroWhenNoEventsAreClaimed() {
        when(
                repository.claimBatch(
                        eq(NOW),
                        eq(NOW.plusSeconds(30)),
                        any(UUID.class),
                        eq(50)
                )
        ).thenReturn(
                List.of()
        );

        int published =
                service.publishBatch();

        assertEquals(
                0,
                published
        );

        verify(
                publisher,
                never()
        ).publish(
                any(),
                any()
        );

        verify(
                repository,
                never()
        ).update(
                any()
        );
    }

    @Test
    void shouldMarkEventAsFailedWhenPublisherReturnsFalse() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        OutboxEvent event =
                OutboxEvent.pending(
                        eventId,
                        portfolioId,
                        "PositionChanged",
                        "{}",
                        NOW
                ).claim(
                        UUID.randomUUID(),
                        NOW.plusSeconds(30)
                );

        when(
                repository.claimBatch(
                        eq(NOW),
                        eq(NOW.plusSeconds(30)),
                        any(UUID.class),
                        eq(50)
                )
        ).thenReturn(
                List.of(event)
        );

        when(
                publisher.publish(
                        "PositionChanged",
                        "{}"
                )
        ).thenReturn(false);

        int published =
                service.publishBatch();

        assertEquals(
                0,
                published
        );

        verify(repository).update(
                eq(
                        event.markFailed(
                                NOW.plusSeconds(1),
                                false
                        )
                )
        );
    }

    @Test
    void shouldMarkEventAsFailedWhenPublisherThrows() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        OutboxEvent event =
                OutboxEvent.pending(
                        eventId,
                        portfolioId,
                        "PositionChanged",
                        "{}",
                        NOW
                ).claim(
                        UUID.randomUUID(),
                        NOW.plusSeconds(30)
                );

        when(
                repository.claimBatch(
                        eq(NOW),
                        eq(NOW.plusSeconds(30)),
                        any(UUID.class),
                        eq(50)
                )
        ).thenReturn(
                List.of(event)
        );

        when(
                publisher.publish(
                        "PositionChanged",
                        "{}"
                )
        ).thenThrow(
                new IllegalStateException(
                        "Kafka unavailable"
                )
        );

        int published =
                service.publishBatch();

        assertEquals(
                0,
                published
        );

        verify(repository).update(
                eq(
                        event.markFailed(
                                NOW.plusSeconds(1),
                                false
                        )
                )
        );
    }

    @Test
    void shouldMarkEventAsPermanentlyFailedOnLastRetry() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        OutboxEvent event =
                new OutboxEvent(
                        eventId,
                        portfolioId,
                        "PositionChanged",
                        "{}",
                        NOW,
                        null,
                        OutboxEventStatus.PROCESSING,
                        9,
                        NOW,
                        UUID.randomUUID(),
                        NOW.plusSeconds(30)
                );

        when(
                repository.claimBatch(
                        eq(NOW),
                        eq(NOW.plusSeconds(30)),
                        any(UUID.class),
                        eq(50)
                )
        ).thenReturn(
                List.of(event)
        );

        when(
                publisher.publish(
                        "PositionChanged",
                        "{}"
                )
        ).thenReturn(false);

        service.publishBatch();

        verify(repository).update(
                eq(
                        event.markFailed(
                                NOW.plusSeconds(60),
                                true
                        )
                )
        );
    }

    @Test
    void shouldPublishMultipleEvents() {
        OutboxEvent first =
                createClaimedEvent();

        OutboxEvent second =
                createClaimedEvent();

        when(
                repository.claimBatch(
                        eq(NOW),
                        eq(NOW.plusSeconds(30)),
                        any(UUID.class),
                        eq(50)
                )
        ).thenReturn(
                List.of(first, second)
        );

        when(
                publisher.publish(
                        any(),
                        any()
                )
        ).thenReturn(true);

        int published =
                service.publishBatch();

        assertEquals(
                2,
                published
        );

        verify(repository).update(
                eq(
                        first.markPublished(NOW)
                )
        );

        verify(repository).update(
                eq(
                        second.markPublished(NOW)
                )
        );
    }

    @Test
    void shouldUseLeaseDurationWhenClaiming() {
        when(
                repository.claimBatch(
                        eq(NOW),
                        eq(NOW.plusSeconds(30)),
                        any(UUID.class),
                        eq(50)
                )
        ).thenReturn(
                List.of()
        );

        service.publishBatch();

        verify(repository).claimBatch(
                eq(NOW),
                eq(NOW.plusSeconds(30)),
                any(UUID.class),
                eq(50)
        );
    }

    private OutboxEvent createClaimedEvent() {
        return OutboxEvent.pending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PositionChanged",
                "{}",
                NOW
        ).claim(
                UUID.randomUUID(),
                NOW.plusSeconds(30)
        );
    }
}