package org.fincore.wealth.position.outbox;

import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.domain.OutboxEventStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventTest {

    private static final UUID AGGREGATE_ID =
            UUID.randomUUID();

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-10-05T10:00:00Z");

    @Test
    void shouldCreatePendingEvent() {
        OutboxEvent event = OutboxEvent.pending(
                AGGREGATE_ID,
                "PositionChanged",
                "{\"quantity\":10}",
                OCCURRED_AT
        );

        assertThat(event.id()).isNotNull();
        assertThat(event.aggregateId())
                .isEqualTo(AGGREGATE_ID);
        assertThat(event.eventType())
                .isEqualTo("PositionChanged");
        assertThat(event.payload())
                .isEqualTo("{\"quantity\":10}");
        assertThat(event.occurredAt())
                .isEqualTo(OCCURRED_AT);
        assertThat(event.publishedAt())
                .isNull();
        assertThat(event.status())
                .isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.retryCount())
                .isZero();
        assertThat(event.nextAttemptAt())
                .isEqualTo(OCCURRED_AT);
    }

    @Test
    void shouldMarkEventAsPublished() {
        OutboxEvent event = OutboxEvent.pending(
                AGGREGATE_ID,
                "PositionChanged",
                "{}",
                OCCURRED_AT
        );

        Instant publishedAt =
                Instant.parse("2026-10-05T10:00:05Z");

        OutboxEvent published =
                event.markPublished(publishedAt);

        assertThat(published.id())
                .isEqualTo(event.id());
        assertThat(published.aggregateId())
                .isEqualTo(event.aggregateId());
        assertThat(published.eventType())
                .isEqualTo(event.eventType());
        assertThat(published.payload())
                .isEqualTo(event.payload());
        assertThat(published.occurredAt())
                .isEqualTo(event.occurredAt());

        assertThat(published.status())
                .isEqualTo(OutboxEventStatus.PUBLISHED);

        assertThat(published.publishedAt())
                .isEqualTo(publishedAt);

        assertThat(published.retryCount())
                .isZero();

        assertThat(published.nextAttemptAt())
                .isNull();
    }

    @Test
    void shouldScheduleRetryWhenEventFails() {
        OutboxEvent event = OutboxEvent.pending(
                AGGREGATE_ID,
                "PositionChanged",
                "{}",
                OCCURRED_AT
        );

        Instant nextAttempt =
                Instant.parse("2026-10-05T10:00:10Z");

        OutboxEvent failed =
                event.markFailed(
                        nextAttempt,
                        false
                );

        assertThat(failed.id())
                .isEqualTo(event.id());

        assertThat(failed.status())
                .isEqualTo(OutboxEventStatus.PENDING);

        assertThat(failed.retryCount())
                .isEqualTo(1);

        assertThat(failed.nextAttemptAt())
                .isEqualTo(nextAttempt);

        assertThat(failed.publishedAt())
                .isNull();
    }

    @Test
    void shouldMarkEventAsPermanentlyFailed() {
        OutboxEvent event = OutboxEvent.pending(
                AGGREGATE_ID,
                "PositionChanged",
                "{}",
                OCCURRED_AT
        );

        OutboxEvent failed =
                event.markFailed(
                        null,
                        true
                );

        assertThat(failed.status())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(failed.retryCount())
                .isEqualTo(1);

        assertThat(failed.nextAttemptAt())
                .isNull();

        assertThat(failed.publishedAt())
                .isNull();
    }

    @Test
    void shouldIncrementRetryCountOnEveryFailure() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                AGGREGATE_ID,
                "PositionChanged",
                "{}",
                OCCURRED_AT,
                null,
                OutboxEventStatus.PENDING,
                3,
                OCCURRED_AT
        );

        OutboxEvent failed =
                event.markFailed(
                        OCCURRED_AT.plusSeconds(10),
                        false
                );

        assertThat(failed.retryCount())
                .isEqualTo(4);

        assertThat(failed.status())
                .isEqualTo(OutboxEventStatus.PENDING);
    }

    @Test
    void shouldRejectNullId() {
        assertThatThrownBy(() ->
                new OutboxEvent(
                        null,
                        AGGREGATE_ID,
                        "PositionChanged",
                        "{}",
                        OCCURRED_AT,
                        null,
                        OutboxEventStatus.PENDING,
                        0,
                        OCCURRED_AT
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ID is required");
    }

    @Test
    void shouldRejectNullAggregateId() {
        assertThatThrownBy(() ->
                new OutboxEvent(
                        UUID.randomUUID(),
                        null,
                        "PositionChanged",
                        "{}",
                        OCCURRED_AT,
                        null,
                        OutboxEventStatus.PENDING,
                        0,
                        OCCURRED_AT
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Aggregate ID is required");
    }

    @Test
    void shouldRejectNullEventType() {
        assertThatThrownBy(() ->
                new OutboxEvent(
                        UUID.randomUUID(),
                        AGGREGATE_ID,
                        null,
                        "{}",
                        OCCURRED_AT,
                        null,
                        OutboxEventStatus.PENDING,
                        0,
                        OCCURRED_AT
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Event type is required");
    }

    @Test
    void shouldRejectNullPayload() {
        assertThatThrownBy(() ->
                new OutboxEvent(
                        UUID.randomUUID(),
                        AGGREGATE_ID,
                        "PositionChanged",
                        null,
                        OCCURRED_AT,
                        null,
                        OutboxEventStatus.PENDING,
                        0,
                        OCCURRED_AT
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Payload is required");
    }

    @Test
    void shouldRejectNegativeRetryCount() {
        assertThatThrownBy(() ->
                new OutboxEvent(
                        UUID.randomUUID(),
                        AGGREGATE_ID,
                        "PositionChanged",
                        "{}",
                        OCCURRED_AT,
                        null,
                        OutboxEventStatus.PENDING,
                        -1,
                        OCCURRED_AT
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Retry count cannot be negative");
    }
}