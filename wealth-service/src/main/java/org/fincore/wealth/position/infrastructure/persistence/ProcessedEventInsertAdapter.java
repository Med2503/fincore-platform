package org.fincore.wealth.position.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ProcessedEventInsertAdapter {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedEventInsertAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean insertIfAbsent(UUID eventId) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO processed_wealth_events (event_id, processed_at)
                VALUES (?, now())
                ON CONFLICT (event_id) DO NOTHING
                """, eventId);

        return inserted == 1;
    }
}