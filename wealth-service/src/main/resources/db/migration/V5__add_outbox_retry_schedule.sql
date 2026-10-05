ALTER TABLE outbox_events
ADD COLUMN next_attempt_at TIMESTAMPTZ;

CREATE INDEX idx_outbox_events_dispatch
    ON outbox_events(status, next_attempt_at, occurred_at);