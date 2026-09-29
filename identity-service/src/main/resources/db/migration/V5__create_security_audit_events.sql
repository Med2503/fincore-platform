CREATE TABLE security_audit_events (
    id UUID PRIMARY KEY,
    user_id UUID,
    event_type VARCHAR(80) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    correlation_id VARCHAR(100),
    source_ip VARCHAR(64),
    user_agent VARCHAR(512),
    details JSONB
);

CREATE INDEX idx_security_audit_user_time
    ON security_audit_events(user_id, occurred_at DESC);

CREATE INDEX idx_security_audit_type_time
    ON security_audit_events(event_type, occurred_at DESC);