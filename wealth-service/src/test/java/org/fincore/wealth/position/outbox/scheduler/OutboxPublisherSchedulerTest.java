package org.fincore.wealth.position.outbox.scheduler;

import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.application.service.PublishOutboxEvents;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.domain.OutboxEventStatus;
import org.fincore.wealth.position.outbox.infrastructure.config.OutboxPublisherProperties;
import org.fincore.wealth.position.outbox.infrastructure.scheduler.OutboxPublisherScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherSchedulerTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private PublishOutboxEvents publisher;

    private OutboxPublisherProperties properties;

    private Clock clock;

    private OutboxPublisherScheduler scheduler;

    private final Instant now =
            Instant.parse("2026-10-05T10:00:00Z");

    @BeforeEach
    void setUp() {
        properties = new OutboxPublisherProperties();

        properties.setBatchSize(50);

        clock = Clock.fixed(
                now,
                ZoneOffset.UTC
        );

        scheduler =
                new OutboxPublisherScheduler(
                        repository,
                        publisher,
                        properties,
                        clock
                );
    }

    @Test
    void shouldPublishAllPendingEvents() {
        OutboxEvent first = event();
        OutboxEvent second = event();

        when(repository.findPending(
                now,
                50
        )).thenReturn(
                List.of(first, second)
        );

        scheduler.publishPendingEvents();

        verify(repository)
                .findPending(
                        now,
                        50
                );

        verify(publisher)
                .publish(first);

        verify(publisher)
                .publish(second);
    }

    @Test
    void shouldDoNothingWhenThereAreNoPendingEvents() {
        when(repository.findPending(
                now,
                50
        )).thenReturn(List.of());

        scheduler.publishPendingEvents();

        verify(repository)
                .findPending(
                        now,
                        50
                );

        verifyNoInteractions(publisher);
    }

    @Test
    void shouldUseConfiguredBatchSize() {
        properties.setBatchSize(10);

        when(repository.findPending(
                now,
                10
        )).thenReturn(List.of());

        scheduler.publishPendingEvents();

        ArgumentCaptor<Integer> captor =
                ArgumentCaptor.forClass(
                        Integer.class
                );

        verify(repository)
                .findPending(
                        eq(now),
                        captor.capture()
                );

        assertThat(captor.getValue())
                .isEqualTo(10);
    }

    @Test
    void shouldPublishEventsInRepositoryOrder() {
        OutboxEvent first = event();
        OutboxEvent second = event();
        OutboxEvent third = event();

        when(repository.findPending(
                now,
                50
        )).thenReturn(
                List.of(
                        first,
                        second,
                        third
                )
        );

        scheduler.publishPendingEvents();

        var inOrder =
                inOrder(publisher);

        inOrder.verify(publisher)
                .publish(first);

        inOrder.verify(publisher)
                .publish(second);

        inOrder.verify(publisher)
                .publish(third);
    }

    @Test
    void shouldPropagatePublisherException() {
        OutboxEvent event = event();

        when(repository.findPending(
                now,
                50
        )).thenReturn(
                List.of(event)
        );

        RuntimeException exception =
                new RuntimeException(
                        "Publisher unavailable"
                );

        doThrow(exception)
                .when(publisher)
                .publish(event);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(
                        () -> scheduler.publishPendingEvents()
                )
                .isSameAs(exception);

        verify(publisher)
                .publish(event);
    }

    private OutboxEvent event() {
        return new OutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PositionChanged",
                "{}",
                now,
                null,

                OutboxEventStatus.PENDING,
                0,
                now
        );
    }
}