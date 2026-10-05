package org.fincore.wealth.position.outbox.service;

import org.fincore.wealth.position.outbox.application.port.EventPublisher;
import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.application.service.PublishOutboxEvents;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.domain.OutboxEventStatus;
import org.fincore.wealth.position.outbox.infrastructure.config.OutboxPublisherProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishOutboxEventsTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private EventPublisher publisher;

    private OutboxPublisherProperties properties;

    private Clock clock;

    private PublishOutboxEvents service;

    private final Instant now =
            Instant.parse("2026-10-05T10:00:00Z");

    @BeforeEach
    void setUp() {
        properties = new OutboxPublisherProperties();
        properties.setMaxRetries(3);

        clock = Clock.fixed(
                now,
                ZoneOffset.UTC
        );

        service = new PublishOutboxEvents(
                repository,
                publisher,
                properties,
                clock
        );
    }

    @Test
    void shouldPublishPendingEvent() {
        OutboxEvent event = pendingEvent();

        when(publisher.publish(
                event.eventType(),
                event.payload()
        )).thenReturn(true);

        service.publish(event);

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(
                        OutboxEvent.class
                );

        verify(repository).update(captor.capture());

        OutboxEvent updated = captor.getValue();

        assertThat(updated.status())
                .isEqualTo(OutboxEventStatus.PUBLISHED);

        assertThat(updated.publishedAt())
                .isEqualTo(now);

        assertThat(updated.retryCount())
                .isZero();

        assertThat(updated.nextAttemptAt())
                .isNull();
    }

    @Test
    void shouldKeepEventPendingWhenPublishingFails() {
        OutboxEvent event = pendingEvent();

        when(publisher.publish(
                event.eventType(),
                event.payload()
        )).thenReturn(false);

        service.publish(event);

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(
                        OutboxEvent.class
                );

        verify(repository).update(captor.capture());

        OutboxEvent updated = captor.getValue();

        assertThat(updated.status())
                .isEqualTo(OutboxEventStatus.PENDING);

        assertThat(updated.retryCount())
                .isEqualTo(1);

        assertThat(updated.nextAttemptAt())
                .isEqualTo(
                        now.plusSeconds(2)
                );

        assertThat(updated.publishedAt())
                .isNull();
    }

    @Test
    void shouldMarkEventAsFailedAfterMaxRetries() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PositionChanged",
                "{}",
                now.minusSeconds(10),
                null,
                OutboxEventStatus.PENDING,
                2,
                now
        );

        when(publisher.publish(
                event.eventType(),
                event.payload()
        )).thenReturn(false);

        service.publish(event);

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(
                        OutboxEvent.class
                );

        verify(repository).update(captor.capture());

        OutboxEvent updated = captor.getValue();

        assertThat(updated.status())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(updated.retryCount())
                .isEqualTo(3);

        assertThat(updated.nextAttemptAt())
                .isNull();
    }

    @Test
    void shouldUseExponentialBackoff() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PositionChanged",
                "{}",
                now,
                null,
                OutboxEventStatus.PENDING,
                2,
                now
        );

        when(publisher.publish(
                event.eventType(),
                event.payload()
        )).thenReturn(false);

        service.publish(event);

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(
                        OutboxEvent.class
                );

        verify(repository).update(captor.capture());

        OutboxEvent updated = captor.getValue();

        assertThat(updated.retryCount())
                .isEqualTo(3);

        assertThat(updated.status())
                .isEqualTo(OutboxEventStatus.FAILED);
    }

    @Test
    void shouldNotUpdateRepositoryBeforePublishing() {
        OutboxEvent event = pendingEvent();

        when(publisher.publish(
                event.eventType(),
                event.payload()
        )).thenReturn(true);

        service.publish(event);

        var inOrder = inOrder(
                publisher,
                repository
        );

        inOrder.verify(publisher).publish(
                event.eventType(),
                event.payload()
        );

        inOrder.verify(repository).update(
                any(OutboxEvent.class)
        );
    }

    @Test
    void shouldPropagatePublisherException() {
        OutboxEvent event = pendingEvent();

        RuntimeException exception =
                new RuntimeException(
                        "Kafka unavailable"
                );

        when(publisher.publish(
                event.eventType(),
                event.payload()
        )).thenThrow(exception);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.publish(event)
                )
                .isSameAs(exception);

        verify(repository, never())
                .update(any());
    }

    private OutboxEvent pendingEvent() {
        return new OutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PositionChanged",
                "{\"quantity\":100}",
                now,
                null,
                OutboxEventStatus.PENDING,
                0,
                now
        );
    }
}