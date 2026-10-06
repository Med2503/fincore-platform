package org.fincore.wealth.position.messaging;

import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.fincore.wealth.position.application.port.PositionProjectionRepository;
import org.fincore.wealth.position.application.port.ProcessedPositionChangedEventRepository;
import org.fincore.wealth.position.application.projection.PositionProjection;
import org.fincore.wealth.position.application.service.PositionChangedProjectionService;
import org.fincore.wealth.position.infrastructure.messaging.PositionChangedConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class PositionChangedConsumerTransactionIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(
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
    private PositionChangedConsumer consumer;

    @Autowired
    private ProcessedPositionChangedEventRepository processedEvents;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update(
                "DELETE FROM processed_position_changed_events"
        );

        jdbcTemplate.update(
                "DELETE FROM position_projections"
        );
    }

    @Test
    void shouldRollbackProcessedEventWhenProjectionFails() {
        UUID eventId = UUID.randomUUID();

        PositionChangedEvent event = createEvent(eventId);

        assertThrows(
                IllegalStateException.class,
                () -> consumer.consume(event)
        );

        assertTrue(
                processedEventDoesNotExist(eventId)
        );
    }

    private boolean processedEventDoesNotExist(
            UUID eventId
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM processed_position_changed_events
                WHERE event_id = ?
                """,
                Integer.class,
                eventId
        );

        return count != null && count == 0;
    }

    private PositionChangedEvent createEvent(
            UUID eventId
    ) {
        return new PositionChangedEvent(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10"),
                new BigDecimal("100"),
                "EUR",
                1,
                Instant.parse(
                        "2026-09-30T10:00:00Z"
                )
        );
    }

    @TestConfiguration
    static class FailureConfiguration {

        @Bean
        PositionChangedProjectionService projectionService() {
            PositionProjectionRepository repository =
                    new FailingPositionProjectionRepository();

            return new PositionChangedProjectionService(
                    repository
            );
        }
    }

    static class FailingPositionProjectionRepository
            implements PositionProjectionRepository {

        @Override
        public void save(
                PositionProjection projection
        ) {
            throw new IllegalStateException(
                    "Projection failure"
            );
        }

        @Override
        public java.util.Optional<PositionProjection> find(
                UUID portfolioId,
                UUID assetId
        ) {
            return java.util.Optional.empty();
        }
    }
}