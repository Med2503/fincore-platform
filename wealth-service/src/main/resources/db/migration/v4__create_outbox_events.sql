CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    aggregate_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload TEXT NOT NULL,

    occurred_at TIMESTAMPTZ NOT NULL,

    published_at TIMESTAMPTZ,

    status VARCHAR(20) NOT NULL,

    retry_count INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_outbox_status
        CHECK (
            status IN (
                'PENDING',
                'PUBLISHED',
                'FAILED'
            )
        )
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events(status, occurred_at);

CREATE INDEX idx_outbox_events_aggregate
    ON outbox_events(aggregate_id);