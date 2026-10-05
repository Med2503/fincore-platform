package org.fincore.wealth.position.outbox.infrastructure.persistence;

import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.domain.OutboxEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventRepositoryAdapterTest {

    @Mock
    private SpringDataOutboxEventRepository repository;

    @Mock
    private OutboxEventPersistenceMapper mapper;

    private OutboxEventRepositoryAdapter adapter;

    private UUID eventId;
    private UUID aggregateId;
    private Instant occurredAt;

    @BeforeEach
    void setUp() {
        adapter = new OutboxEventRepositoryAdapter(
                repository,
                mapper
        );

        eventId = UUID.randomUUID();
        aggregateId = UUID.randomUUID();
        occurredAt =
                Instant.parse("2026-10-05T10:00:00Z");
    }

    @Test
    void shouldSaveOutboxEvent() {
        OutboxEvent event = pendingEvent();
        OutboxEventJpaEntity entity = newEntity();
        OutboxEventJpaEntity savedEntity = newEntity();
        OutboxEvent savedEvent = pendingEvent();

        when(mapper.toEntity(event))
                .thenReturn(entity);

        when(repository.save(entity))
                .thenReturn(savedEntity);

        when(mapper.toDomain(savedEntity))
                .thenReturn(savedEvent);

        OutboxEvent result =
                adapter.save(event);

        assertThat(result)
                .isSameAs(savedEvent);

        verify(mapper).toEntity(event);
        verify(repository).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void shouldUpdateExistingOutboxEvent() {
        OutboxEvent event = pendingEvent();

        OutboxEventJpaEntity entity =
                newEntity();

        when(repository.findById(eventId))
                .thenReturn(Optional.of(entity));

        adapter.update(event);

        verify(repository)
                .findById(eventId);

        verify(mapper)
                .updateEntity(entity, event);

        verify(repository)
                .save(entity);
    }

    @Test
    void shouldFailWhenUpdatingUnknownEvent() {
        OutboxEvent event = pendingEvent();

        when(repository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> adapter.update(event)
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Outbox event not found: " + eventId
                );

        verify(repository)
                .findById(eventId);

        verify(mapper, never())
                .updateEntity(any(), any());

        verify(repository, never())
                .save(any());
    }

    @Test
    void shouldFindPendingEvents() {
        Instant now =
                Instant.parse(
                        "2026-10-05T10:00:00Z"
                );

        OutboxEventJpaEntity first =
                newEntity();

        OutboxEventJpaEntity second =
                newEntity();

        OutboxEvent firstDomain =
                pendingEvent();

        OutboxEvent secondDomain =
                pendingEvent();

        when(repository.findPendingForUpdate(
                OutboxEventStatus.PENDING.name(),
                now,
                50
        )).thenReturn(
                List.of(first, second)
        );

        when(mapper.toDomain(first))
                .thenReturn(firstDomain);

        when(mapper.toDomain(second))
                .thenReturn(secondDomain);

        List<OutboxEvent> result =
                adapter.findPending(
                        now,
                        50
                );

        assertThat(result)
                .containsExactly(
                        firstDomain,
                        secondDomain
                );

        verify(repository)
                .findPendingForUpdate(
                        OutboxEventStatus.PENDING.name(),
                        now,
                        50
                );

        verify(mapper)
                .toDomain(first);

        verify(mapper)
                .toDomain(second);
    }

    @Test
    void shouldReturnEmptyListWhenNoPendingEventsExist() {
        Instant now =
                Instant.parse(
                        "2026-10-05T10:00:00Z"
                );

        when(repository.findPendingForUpdate(
                OutboxEventStatus.PENDING.name(),
                now,
                50
        )).thenReturn(List.of());

        List<OutboxEvent> result =
                adapter.findPending(
                        now,
                        50
                );

        assertThat(result)
                .isEmpty();

        verify(mapper, never())
                .toDomain(any());
    }

    @Test
    void shouldUseRequestedBatchSize() {
        Instant now =
                Instant.parse(
                        "2026-10-05T10:00:00Z"
                );

        when(repository.findPendingForUpdate(
                OutboxEventStatus.PENDING.name(),
                now,
                10
        )).thenReturn(List.of());

        adapter.findPending(
                now,
                10
        );

        ArgumentCaptor<Integer> captor =
                ArgumentCaptor.forClass(
                        Integer.class
                );

        verify(repository)
                .findPendingForUpdate(
                        eq(
                                OutboxEventStatus.PENDING.name()
                        ),
                        eq(now),
                        captor.capture()
                );

        assertThat(captor.getValue())
                .isEqualTo(10);
    }

    private OutboxEvent pendingEvent() {
        return new OutboxEvent(
                eventId,
                aggregateId,
                "PositionChanged",
                "{\"quantity\":100}",
                occurredAt,
                null,
                OutboxEventStatus.PENDING,
                0,
                occurredAt
        );
    }

    private OutboxEventJpaEntity newEntity() {
        OutboxEventJpaEntity entity =
                new OutboxEventJpaEntity();

        entity.setId(eventId);
        entity.setAggregateId(aggregateId);
        entity.setEventType("PositionChanged");
        entity.setPayload(
                "{\"quantity\":100}"
        );
        entity.setOccurredAt(occurredAt);
        entity.setStatus(
                OutboxEventStatus.PENDING
        );
        entity.setRetryCount(0);
        entity.setNextAttemptAt(occurredAt);

        return entity;
    }
}