CREATE TABLE users (
    id UUID PRIMARY KEY,

    username VARCHAR(100) NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    status VARCHAR(20) NOT NULL,

    failed_login_attempts INTEGER NOT NULL DEFAULT 0,

    locked_until TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_users_username
        UNIQUE (username),

    CONSTRAINT chk_users_status
        CHECK (
            status IN (
                'ACTIVE',
                'LOCKED',
                'DISABLED',
                'PENDING'
            )
        ),

    CONSTRAINT chk_users_failed_attempts
        CHECK (failed_login_attempts >= 0)
);

CREATE INDEX idx_users_status
    ON users(status);

CREATE INDEX idx_users_created_at
    ON users(created_at);