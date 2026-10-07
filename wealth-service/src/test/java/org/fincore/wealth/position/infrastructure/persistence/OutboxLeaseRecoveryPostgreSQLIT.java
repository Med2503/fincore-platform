package org.fincore.wealth.position.infrastructure.persistence;

import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class OutboxLeaseRecoveryPostgreSQLIT {

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

    @Test
    void shouldRecoverEventAfterLeaseExpiration() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        repository.save(
                OutboxEvent.pending(
                        eventId,
                        portfolioId,
                        "PositionChanged",
                        "{}",
                        Instant.parse(
                                "2026-09-30T10:00:00Z"
                        )
                )
        );

        Instant initialTime =
                Instant.parse(
                        "2026-09-30T10:00:00Z"
                );

        List<OutboxEvent> firstClaim =
                repository.claimBatch(
                        initialTime,
                        initialTime.plusSeconds(30),
                        UUID.randomUUID(),
                        10
                );

        assertEquals(
                1,
                firstClaim.size()
        );

        Instant afterLease =
                initialTime.plusSeconds(31);

        List<OutboxEvent> recovered =
                repository.claimBatch(
                        afterLease,
                        afterLease.plusSeconds(30),
                        UUID.randomUUID(),
                        10
                );

        assertEquals(
                1,
                recovered.size()
        );

        assertEquals(
                eventId,
                recovered.get(0).id()
        );
    }
}