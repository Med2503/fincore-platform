CREATE TABLE dlq_replay_attempts (
    event_id UUID PRIMARY KEY,

    replay_count INTEGER NOT NULL,

    first_replayed_at TIMESTAMPTZ NOT NULL,

    last_replayed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_dlq_replay_count
        CHECK (replay_count >= 0)
);