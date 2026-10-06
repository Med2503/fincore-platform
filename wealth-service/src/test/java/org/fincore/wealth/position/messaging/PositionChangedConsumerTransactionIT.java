package org.fincore.wealth.position.messaging;

import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.fincore.wealth.position.application.port.PositionProjectionRepository;
import org.fincore.wealth.position.application.service.PositionChangedProjectionService;
import org.fincore.wealth.position.infrastructure.messaging.PositionChangedConsumer;
import org.fincore.wealth.position.infrastructure.persistence.JdbcProcessedPositionChangedEventRepository;

import org.fincore.wealth.position.outbox.infrastructure.persistence.JdbcPositionProjectionRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class PositionChangedConsumerTransactionIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("wealth_test")
                    .withUsername("wealth_user")
                    .withPassword("wealth_password");

    @DynamicPropertySource
    static void configureDatabase(
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
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcPositionProjectionRepository projectionRepository;

    @Autowired
    private JdbcProcessedPositionChangedEventRepository processedEvents;

    private UUID portfolioId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "DELETE FROM processed_position_changed_events"
        );

        jdbcTemplate.update(
                "DELETE FROM position_projections"
        );

        portfolioId = UUID.randomUUID();
        assetId = UUID.randomUUID();
    }

    @Test
    void shouldRollbackClaimWhenProjectionFails() {
        UUID eventId = UUID.randomUUID();

        PositionChangedEvent event =
                validEvent(eventId);

        PositionChangedConsumer consumer =
                new PositionChangedConsumer(
                        processedEvents,
                        failingProjectionService()
                );

        assertThatThrownBy(
                () -> consumer.consume(event)
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Projection failed"
                );

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM processed_position_changed_events
                        WHERE event_id = ?
                        """,
                Integer.class,
                eventId
        );

        assertThat(count)
                .isZero();
    }

    @Test
    void shouldAllowSameEventToBeProcessedAgainAfterRollback() {
        UUID eventId = UUID.randomUUID();

        PositionChangedEvent event =
                validEvent(eventId);

        PositionChangedConsumer failingConsumer =
                new PositionChangedConsumer(
                        processedEvents,
                        failingProjectionService()
                );

        assertThatThrownBy(
                () -> failingConsumer.consume(event)
        )
                .isInstanceOf(
                        IllegalStateException.class
                );

        PositionChangedConsumer successfulConsumer =
                new PositionChangedConsumer(
                        processedEvents,
                        new PositionChangedProjectionService(
                                projectionRepository
                        )
                );

        successfulConsumer.consume(event);

        assertThat(
                projectionRepository.find(
                        portfolioId,
                        assetId
                )
        )
                .isPresent();

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM processed_position_changed_events
                        WHERE event_id = ?
                        """,
                Integer.class,
                eventId
        );

        assertThat(count)
                .isEqualTo(1);
    }

    private PositionChangedEvent validEvent(
            UUID eventId
    ) {
        return new PositionChangedEvent(
                eventId,
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                "EUR",
                1,
                Instant.parse(
                        "2026-10-05T10:00:00Z"
                )
        );
    }

    private PositionChangedProjectionService
    failingProjectionService() {

        return new PositionChangedProjectionService(
                new PositionProjectionRepository() {

                    @Override
                    public void save(
                            org.fincore.wealth.position.application
                                    .projection.PositionProjection projection
                    ) {
                        throw new IllegalStateException(
                                "Projection failed"
                        );
                    }

                    @Override
                    public void delete(
                            UUID portfolioId,
                            UUID assetId
                    ) {
                        throw new IllegalStateException(
                                "Projection failed"
                        );
                    }

                    @Override
                    public java.util.Optional<
                            org.fincore.wealth.position.application
                                    .projection.PositionProjection
                            > find(
                            UUID portfolioId,
                            UUID assetId
                    ) {
                        return java.util.Optional.empty();
                    }
                }
        );
    }
}