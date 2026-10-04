package org.fincore.wealth.position.outbox;

import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.domain.OutboxEventStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventTest {

    @Test
    void shouldCreatePendingEvent() {
        UUID aggregateId = UUID.randomUUID();
        Instant occurredAt = Instant.parse(
                "2026-10-02T10:00:00Z"
        );

        OutboxEvent event = OutboxEvent.pending(
                aggregateId,
                "PositionUpdated",
                "{\"quantity\":10}",
                occurredAt
        );

        assertThat(event.id())
                .isNotNull();

        assertThat(event.aggregateId())
                .isEqualTo(aggregateId);

        assertThat(event.eventType())
                .isEqualTo("PositionUpdated");

        assertThat(event.payload())
                .isEqualTo("{\"quantity\":10}");

        assertThat(event.occurredAt())
                .isEqualTo(occurredAt);

        assertThat(event.publishedAt())
                .isNull();

        assertThat(event.status())
                .isEqualTo(OutboxEventStatus.PENDING);

        assertThat(event.retryCount())
                .isZero();
    }

    @Test
    void shouldMarkPendingEventAsPublished() {
        Instant occurredAt = Instant.parse(
                "2026-10-02T10:00:00Z"
        );

        Instant publishedAt = Instant.parse(
                "2026-10-02T10:01:00Z"
        );

        OutboxEvent event = OutboxEvent.pending(
                UUID.randomUUID(),
                "PositionUpdated",
                "{}",
                occurredAt
        );

        OutboxEvent published = event.markPublished(
                publishedAt
        );

        assertThat(published.status())
                .isEqualTo(OutboxEventStatus.PUBLISHED);

        assertThat(published.publishedAt())
                .isEqualTo(publishedAt);

        assertThat(published.retryCount())
                .isZero();
    }

    @Test
    void shouldIncrementRetryCountWhenMarkedFailed() {
        OutboxEvent event = OutboxEvent.pending(
                UUID.randomUUID(),
                "PositionUpdated",
                "{}",
                Instant.parse(
                        "2026-10-02T10:00:00Z"
                )
        );

        OutboxEvent failed = event.markFailed();

        assertThat(failed.status())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(failed.retryCount())
                .isEqualTo(1);
    }

    @Test
    void shouldRejectNegativeRetryCount() {
        assertThatThrownBy(() ->
                new OutboxEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "PositionUpdated",
                        "{}",
                        Instant.now(),
                        null,
                        OutboxEventStatus.PENDING,
                        -1
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Retry count cannot be negative");
    }

    @Test
    void shouldRejectNullId() {
        assertThatThrownBy(() ->
                new OutboxEvent(
                        null,
                        UUID.randomUUID(),
                        "PositionUpdated",
                        "{}",
                        Instant.now(),
                        null,
                        OutboxEventStatus.PENDING,
                        0
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ID is required");
    }

    @Test
    void shouldRejectBlankEventType() {
        assertThatThrownBy(() ->
                new OutboxEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        " ",
                        "{}",
                        Instant.now(),
                        null,
                        OutboxEventStatus.PENDING,
                        0
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event type cannot be blank");
    }
}