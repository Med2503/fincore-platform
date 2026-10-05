package org.fincore.wealth.position.infrastructure.persistence;

import org.fincore.wealth.position.application.port.ProcessedPositionChangedEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JdbcProcessedPositionChangedEventRepository
        implements ProcessedPositionChangedEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProcessedPositionChangedEventRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean claim(UUID eventId) {
        int inserted = jdbcTemplate.update(
                """
                INSERT INTO processed_position_changed_events (
                    event_id,
                    processed_at
                )
                VALUES (?, CURRENT_TIMESTAMP)
                ON CONFLICT (event_id) DO NOTHING
                """,
                eventId
        );

        return inserted == 1;
    }
}