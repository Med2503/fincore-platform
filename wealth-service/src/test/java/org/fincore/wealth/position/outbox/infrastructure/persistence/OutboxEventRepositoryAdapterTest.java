
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

    private static final Instant NOW =
            Instant.parse("2026-10-05T10:00:00Z");

    private static final int BATCH_SIZE = 50;

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
        occurredAt = NOW;
    }

    @Test
    void shouldSaveOutboxEvent() {
        OutboxEvent event = pendingEvent();
        OutboxEventJpaEntity entity = newEntity();
        OutboxEventJpaEntity savedEntity = newEntity();
        OutboxEvent savedEvent = pendingEvent();

        when(mapper.toEntity(event)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedEvent);

        OutboxEvent result = adapter.save(event);

        assertThat(result).isSameAs(savedEvent);

        verify(mapper).toEntity(event);
        verify(repository).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void shouldUpdateExistingOutboxEvent() {
        OutboxEvent event = pendingEvent();
        OutboxEventJpaEntity entity = newEntity();

        when(repository.findById(eventId))
                .thenReturn(Optional.of(entity));

        adapter.update(event);

        verify(repository).findById(eventId);
        verify(mapper).updateEntity(entity, event);
        verify(repository).save(entity);
    }

    @Test
    void shouldFailWhenUpdatingUnknownEvent() {
        OutboxEvent event = pendingEvent();

        when(repository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.update(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Outbox event not found: " + eventId);

        verify(repository).findById(eventId);
        verify(mapper, never()).updateEntity(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldClaimEventsAndAssignLeaseToWorker() {
        UUID workerId = UUID.randomUUID();
        Instant lockedUntil = NOW.plusSeconds(30);

        OutboxEventJpaEntity entity = newEntity();
        OutboxEvent domainEvent = pendingEvent();

        when(repository.findClaimable(NOW, BATCH_SIZE))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domainEvent);
        when(repository.save(entity)).thenReturn(entity);

        List<OutboxEvent> result = adapter.claimBatch(
                NOW,
                lockedUntil,
                workerId,
                BATCH_SIZE
        );

        assertThat(result).hasSize(1);

        OutboxEvent claimed = result.get(0);

        assertThat(claimed.id()).isEqualTo(eventId);
        assertThat(claimed.aggregateId()).isEqualTo(aggregateId);
        assertThat(claimed.eventType()).isEqualTo("PositionChanged");
        assertThat(claimed.payload()).isEqualTo("{\"quantity\":100}");
        assertThat(claimed.status()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(claimed.claimedBy()).isEqualTo(workerId);
        assertThat(claimed.lockedUntil()).isEqualTo(lockedUntil);
        assertThat(claimed.retryCount()).isZero();

        verify(repository).findClaimable(NOW, BATCH_SIZE);
        verify(mapper).toDomain(entity);
        verify(mapper).updateEntity(entity, claimed);
        verify(repository).save(entity);
        verify(repository).flush();
    }

    @Test
    void shouldReturnEmptyListWhenNoEventsCanBeClaimed() {
        UUID workerId = UUID.randomUUID();
        Instant lockedUntil = NOW.plusSeconds(30);

        when(repository.findClaimable(NOW, BATCH_SIZE))
                .thenReturn(List.of());

        List<OutboxEvent> result = adapter.claimBatch(
                NOW,
                lockedUntil,
                workerId,
                BATCH_SIZE
        );

        assertThat(result).isEmpty();

        verify(repository).findClaimable(NOW, BATCH_SIZE);
        verifyNoInteractions(mapper);
        verify(repository, never()).save(any());
        verify(repository, never()).flush();
    }

    @Test
    void shouldClaimEveryReturnedEvent() {
        UUID workerId = UUID.randomUUID();
        Instant lockedUntil = NOW.plusSeconds(30);

        UUID secondEventId = UUID.randomUUID();
        UUID secondAggregateId = UUID.randomUUID();

        OutboxEventJpaEntity firstEntity = newEntity();
        OutboxEventJpaEntity secondEntity = newEntity();
        secondEntity.setId(secondEventId);
        secondEntity.setAggregateId(secondAggregateId);

        OutboxEvent firstEvent = pendingEvent();
        OutboxEvent secondEvent = new OutboxEvent(
                secondEventId,
                secondAggregateId,
                "PositionChanged",
                "{\"quantity\":200}",
                occurredAt,
                null,
                OutboxEventStatus.PENDING,
                0,
                occurredAt,
                null,
                null
        );

        when(repository.findClaimable(NOW, 10))
                .thenReturn(List.of(firstEntity, secondEntity));
        when(mapper.toDomain(firstEntity)).thenReturn(firstEvent);
        when(mapper.toDomain(secondEntity)).thenReturn(secondEvent);
        when(repository.save(any(OutboxEventJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<OutboxEvent> result = adapter.claimBatch(
                NOW,
                lockedUntil,
                workerId,
                10
        );

        assertThat(result)
                .hasSize(2)
                .allSatisfy(event -> {
                    assertThat(event.status())
                            .isEqualTo(OutboxEventStatus.PROCESSING);
                    assertThat(event.claimedBy()).isEqualTo(workerId);
                    assertThat(event.lockedUntil()).isEqualTo(lockedUntil);
                });

        assertThat(result)
                .extracting(OutboxEvent::id)
                .containsExactly(eventId, secondEventId);

        verify(repository).save(firstEntity);
        verify(repository).save(secondEntity);
        verify(repository).flush();
    }

    @Test
    void shouldPassRequestedBatchSizeToRepository() {
        UUID workerId = UUID.randomUUID();
        Instant lockedUntil = NOW.plusSeconds(30);

        when(repository.findClaimable(NOW, 10))
                .thenReturn(List.of());

        adapter.claimBatch(NOW, lockedUntil, workerId, 10);

        verify(repository).findClaimable(NOW, 10);
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
                occurredAt,
                null,
                null
        );
    }

    private OutboxEventJpaEntity newEntity() {
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();

        entity.setId(eventId);
        entity.setAggregateId(aggregateId);
        entity.setEventType("PositionChanged");
        entity.setPayload("{\"quantity\":100}");
        entity.setOccurredAt(occurredAt);
        entity.setStatus(OutboxEventStatus.PENDING);
        entity.setRetryCount(0);
        entity.setNextAttemptAt(occurredAt);
        entity.setPublishedAt(null);
        entity.setClaimedBy(null);
        entity.setLockedUntil(null);

        return entity;
    }
}
