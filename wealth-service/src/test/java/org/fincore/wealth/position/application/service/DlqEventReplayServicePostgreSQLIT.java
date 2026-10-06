package org.fincore.wealth.position.application.service;

import org.fincore.wealth.position.application.port.DlqReplayOutboxRepository;
import org.fincore.wealth.position.infrastructure.persistence.DlqReplayOutboxRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest
class DlqEventReplayServicePostgreSQLIT {

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
    private DlqEventReplayService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FailingOnceDlqReplayOutboxRepository
            failingOutboxRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update(
                "DELETE FROM outbox_events"
        );

        jdbcTemplate.update(
                "DELETE FROM dlq_replay_attempts"
        );

        failingOutboxRepository.reset();
    }

    @Test
    void shouldRollbackReplayAttemptWhenOutboxFails() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        String payload =
                createPayload(
                        eventId,
                        portfolioId
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.replay(
                        eventId,
                        payload,
                        3
                )
        );

        assertEquals(
                0,
                countReplayAttempts(eventId)
        );

        assertEquals(
                0,
                countOutboxEvents(eventId)
        );
    }

    @Test
    void shouldAllowReplayAgainAfterRollback() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        String payload =
                createPayload(
                        eventId,
                        portfolioId
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.replay(
                        eventId,
                        payload,
                        3
                )
        );

        assertEquals(
                0,
                countReplayAttempts(eventId)
        );

        assertEquals(
                0,
                countOutboxEvents(eventId)
        );

        DlqReplayResult result =
                service.replay(
                        eventId,
                        payload,
                        3
                );

        assertEquals(
                DlqReplayResult.REPLAYED,
                result
        );

        assertEquals(
                1,
                countReplayAttempts(eventId)
        );

        assertEquals(
                1,
                countOutboxEvents(eventId)
        );
    }

    @Test
    void shouldPersistEventIdAndPortfolioIdSeparately() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        String payload =
                createPayload(
                        eventId,
                        portfolioId
                );

        DlqReplayResult result =
                service.replay(
                        eventId,
                        payload,
                        3
                );

        assertEquals(
                DlqReplayResult.REPLAYED,
                result
        );

        OutboxRow row =
                findOutboxEvent(eventId);

        assertEquals(
                eventId,
                row.eventId()
        );

        assertEquals(
                portfolioId,
                row.aggregateId()
        );

        assertEquals(
                "PositionChanged",
                row.eventType()
        );

        assertEquals(
                payload,
                row.payload()
        );
    }

    @Test
    void shouldPersistReplayAttemptAndOutboxAtomically() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        String payload =
                createPayload(
                        eventId,
                        portfolioId
                );

        DlqReplayResult result =
                service.replay(
                        eventId,
                        payload,
                        3
                );

        assertEquals(
                DlqReplayResult.REPLAYED,
                result
        );

        assertEquals(
                1,
                countReplayAttempts(eventId)
        );

        assertEquals(
                1,
                countOutboxEvents(eventId)
        );

        Integer replayCount =
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
                1,
                replayCount
        );
    }

    private int countReplayAttempts(
            UUID eventId
    ) {
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM dlq_replay_attempts
                        WHERE event_id = ?
                        """,
                        Integer.class,
                        eventId
                );

        return count == null ? 0 : count;
    }

    private int countOutboxEvents(
            UUID eventId
    ) {
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM outbox_events
                        WHERE id = ?
                        """,
                        Integer.class,
                        eventId
                );

        return count == null ? 0 : count;
    }

    private OutboxRow findOutboxEvent(
            UUID eventId
    ) {
        return jdbcTemplate.queryForObject(
                """
                SELECT
                    id,
                    aggregate_id,
                    event_type,
                    payload
                FROM outbox_events
                WHERE id = ?
                """,
                (resultSet, rowNum) ->
                        new OutboxRow(
                                resultSet.getObject(
                                        "id",
                                        UUID.class
                                ),
                                resultSet.getObject(
                                        "aggregate_id",
                                        UUID.class
                                ),
                                resultSet.getString(
                                        "event_type"
                                ),
                                resultSet.getString(
                                        "payload"
                                )
                        ),
                eventId
        );
    }

    private String createPayload(
            UUID eventId,
            UUID portfolioId
    ) {
        return """
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
    }

    private record OutboxRow(
            UUID eventId,
            UUID aggregateId,
            String eventType,
            String payload
    ) {
    }

    @TestConfiguration
    static class FailureConfiguration {

        @Bean
        @Primary
        FailingOnceDlqReplayOutboxRepository
        failingOnceDlqReplayOutboxRepository(
                DlqReplayOutboxRepositoryAdapter delegate
        ) {
            return new FailingOnceDlqReplayOutboxRepository(
                    delegate
            );
        }
    }

    static class FailingOnceDlqReplayOutboxRepository
            implements DlqReplayOutboxRepository {

        private final DlqReplayOutboxRepository delegate;

        private final AtomicBoolean fail =
                new AtomicBoolean(true);

        FailingOnceDlqReplayOutboxRepository(
                DlqReplayOutboxRepository delegate
        ) {
            this.delegate = delegate;
        }

        @Override
        public void save(
                UUID eventId,
                UUID aggregateId,
                String payload,
                Instant occurredAt
        ) {
            if (fail.compareAndSet(true, false)) {
                throw new IllegalStateException(
                        "Simulated outbox persistence failure"
                );
            }

            delegate.save(
                    eventId,
                    aggregateId,
                    payload,
                    occurredAt
            );
        }

        void reset() {
            fail.set(true);
        }
    }
}