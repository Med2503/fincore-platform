ALTER TABLE positions
ADD COLUMN last_execution_sequence BIGINT NOT NULL DEFAULT 0;