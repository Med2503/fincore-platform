package org.fincore.wealth.position.outbox.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.fincore.wealth.position.domain.OutboxEventStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {

    @Id
    private UUID id;

    @Column(
            name = "aggregate_id",
            nullable = false
    )
    private UUID aggregateId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 100
    )
    private String eventType;

    @Column(
            name = "payload",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Column(
            name = "occurred_at",
            nullable = false
    )
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private OutboxEventStatus status;

    @Column(
            name = "retry_count",
            nullable = false
    )
    private int retryCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "claimed_by")
    private UUID claimedBy;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected OutboxEventJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(UUID aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxEventStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public UUID getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(UUID claimedBy) {
        this.claimedBy = claimedBy;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
}