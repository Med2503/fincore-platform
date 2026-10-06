package org.fincore.wealth.position.infrastructure.persistence;

import org.fincore.wealth.position.application.port.DlqReplayAttemptRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class JdbcDlqReplayAttemptRepository
        implements DlqReplayAttemptRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDlqReplayAttemptRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int claimNextReplay(
            UUID eventId,
            Instant now,
            int maxReplays
    ) {
        int inserted = jdbcTemplate.update(
                """
                INSERT INTO dlq_replay_attempts (
                    event_id,
                    replay_count,
                    first_replayed_at,
                    last_replayed_at
                )
                VALUES (?, 1, ?, ?)
                ON CONFLICT (event_id)
                DO UPDATE SET
                    replay_count =
                        dlq_replay_attempts.replay_count + 1,
                    last_replayed_at = EXCLUDED.last_replayed_at
                WHERE dlq_replay_attempts.replay_count < ?
                """,
                eventId,
                now,
                now,
                maxReplays
        );

        return inserted;
    }
}