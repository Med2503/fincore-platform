package org.fincore.wealth.position.outbox.domain;

public enum OutboxEventStatus {

    PENDING,

    PROCESSING,

    PUBLISHED,

    FAILED
}