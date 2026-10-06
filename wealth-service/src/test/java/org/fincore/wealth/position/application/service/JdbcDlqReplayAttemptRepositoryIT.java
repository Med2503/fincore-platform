package org.fincore.wealth.position.application.service;

import org.fincore.wealth.position.application.port.DlqReplayAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
class JdbcDlqReplayAttemptRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void properties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.flyway.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.flyway.user",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.flyway.password",
                POSTGRES::getPassword
        );
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DlqReplayAttemptRepository repository;

    @BeforeEach
    void clean() {
        jdbcTemplate.update(
                "DELETE FROM dlq_replay_attempts"
        );
    }

    @Test
    void shouldAllowExactlyThreeReplays() {

        UUID eventId = UUID.randomUUID();

        Instant now =
                Instant.parse(
                        "2026-09-30T10:00:00Z"
                );

        assertEquals(
                1,
                repository.claimNextReplay(
                        eventId,
                        now,
                        3
                )
        );

        assertEquals(
                1,
                repository.claimNextReplay(
                        eventId,
                        now,
                        3
                )
        );

        assertEquals(
                1,
                repository.claimNextReplay(
                        eventId,
                        now,
                        3
                )
        );

        assertEquals(
                0,
                repository.claimNextReplay(
                        eventId,
                        now,
                        3
                )
        );

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT replay_count
                        FROM dlq_replay_attempts
                        WHERE event_id = ?
                        """,
                        Integer.class,
                        eventId
                );

        assertEquals(
                3,
                count
        );
    }
}