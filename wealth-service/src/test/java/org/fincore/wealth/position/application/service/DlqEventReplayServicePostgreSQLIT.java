package org.fincore.wealth.position.application.service;

import org.fincore.wealth.position.application.port.DlqReplayOutboxRepository;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
class DlqReplayOutboxIdentityPostgreSQLIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("wealth_test")
                    .withUsername("wealth")
                    .withPassword("wealth");

    @DynamicPropertySource
    static void configurePostgres(
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
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );

        registry.add(
                "spring.flyway.enabled",
                () -> true
        );

        registry.add(
                "spring.cloud.stream.enabled",
                () -> false
        );
    }

    @Autowired
    private DlqReplayOutboxRepository outbox;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update(
                "DELETE FROM outbox_events"
        );
    }

    @Test
    void shouldPersistEventIdAndAggregateIdSeparately() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        String payload =
                """
                {
                  "eventId": "%s",
                  "portfolioId": "%s",
                  "assetId": "%s",
                  "quantity": 10,
                  "averageCost": 100,
                  "currency": "EUR",
                  "executionSequence": 1,
                  "occurredAt": "2026-09-30T10:00:00Z"
                }
                """.formatted(
                        eventId,
                        portfolioId,
                        UUID.randomUUID()
                );

        outbox.save(
                eventId,
                portfolioId,
                payload,
                java.time.Instant.parse(
                        "2026-09-30T10:00:00Z"
                )
        );

        OutboxIdentity identity =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            id,
                            aggregate_id
                        FROM outbox_events
                        WHERE id = ?
                        """,
                        (rs, rowNum) ->
                                new OutboxIdentity(
                                        rs.getObject(
                                                "id",
                                                UUID.class
                                        ),
                                        rs.getObject(
                                                "aggregate_id",
                                                UUID.class
                                        )
                                ),
                        eventId
                );

        assertEquals(
                eventId,
                identity.eventId()
        );

        assertEquals(
                portfolioId,
                identity.aggregateId()
        );
    }

    private record OutboxIdentity(
            UUID eventId,
            UUID aggregateId
    ) {
    }
}