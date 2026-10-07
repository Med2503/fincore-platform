package org.fincore.wealth.position.infrastructure.persistence;

import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
class OutboxClaimConcurrencyPostgreSQLIT {

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
    private OutboxEventRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldClaimEventOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        OutboxEvent event =
                OutboxEvent.pending(
                        eventId,
                        portfolioId,
                        "PositionChanged",
                        "{}",
                        Instant.parse(
                                "2026-09-30T10:00:00Z"
                        )
                );

        repository.save(event);

        Instant now =
                Instant.parse(
                        "2026-09-30T10:00:00Z"
                );

        UUID workerA = UUID.randomUUID();
        UUID workerB = UUID.randomUUID();

        List<OutboxEvent> firstClaim =
                repository.claimBatch(
                        now,
                        now.plusSeconds(30),
                        workerA,
                        10
                );

        List<OutboxEvent> secondClaim =
                repository.claimBatch(
                        now,
                        now.plusSeconds(30),
                        workerB,
                        10
                );

        assertEquals(
                1,
                firstClaim.size()
        );

        assertEquals(
                0,
                secondClaim.size()
        );

        Integer processingCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM outbox_events
                        WHERE id = ?
                          AND status = 'PROCESSING'
                        """,
                        Integer.class,
                        eventId
                );

        assertEquals(
                1,
                processingCount
        );
    }
}