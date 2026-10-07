
ALTER TABLE outbox_events
DROP CONSTRAINT chk_outbox_status;

ALTER TABLE outbox_events
ADD CONSTRAINT chk_outbox_status
CHECK (
    status IN (
        'PENDING',
        'PROCESSING',
        'PUBLISHED',
        'FAILED'
    )
);

ALTER TABLE outbox_events
ADD COLUMN claimed_by UUID;

ALTER TABLE outbox_events
ADD COLUMN locked_until TIMESTAMPTZ;

CREATE INDEX idx_outbox_events_claim
    ON outbox_events(
        status,
        next_attempt_at,
        locked_until,
        occurred_at
    );