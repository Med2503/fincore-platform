package org.fincore.wealth.position.messaging;

import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.fincore.wealth.position.application.port.PositionProjectionRepository;
import org.fincore.wealth.position.application.projection.PositionProjection;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class PositionChangedConsumerIT {

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

        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcPositionProjectionRepository projectionRepository;

    @Autowired
    private JdbcProcessedPositionChangedEventRepository processedEvents;

    private PositionChangedConsumer consumer;

    private UUID portfolioId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        consumer = new PositionChangedConsumer(
                processedEvents,
                new PositionChangedProjectionService(
                        projectionRepository
                )
        );

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
    void shouldProcessEventAndCreateProjection() {
        PositionChangedEvent event =
                createEvent(
                        UUID.randomUUID(),
                        new BigDecimal("10"),
                        new BigDecimal("125.50"),
                        1
                );

        consumer.consume(event);

        Optional<PositionProjection> result =
                projectionRepository.find(
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .isPresent();

        assertThat(result.get().quantity())
                .isEqualByComparingTo("10");

        assertThat(result.get().averageCost())
                .isEqualByComparingTo("125.50");

        assertThat(result.get().executionSequence())
                .isEqualTo(1);
    }

    @Test
    void shouldIgnoreDuplicateEvent() {
        UUID eventId = UUID.randomUUID();

        PositionChangedEvent event =
                createEvent(
                        eventId,
                        new BigDecimal("10"),
                        new BigDecimal("125.50"),
                        1
                );

        consumer.consume(event);

        PositionChangedEvent duplicate =
                createEvent(
                        eventId,
                        new BigDecimal("20"),
                        new BigDecimal("200.00"),
                        2
                );

        consumer.consume(duplicate);

        Optional<PositionProjection> result =
                projectionRepository.find(
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .isPresent();

        assertThat(result.get().quantity())
                .isEqualByComparingTo("10");

        assertThat(result.get().averageCost())
                .isEqualByComparingTo("125.50");

        assertThat(result.get().executionSequence())
                .isEqualTo(1);
    }

    @Test
    void shouldIgnoreOlderEvent() {
        PositionChangedEvent current =
                createEvent(
                        UUID.randomUUID(),
                        new BigDecimal("20"),
                        new BigDecimal("150.00"),
                        10
                );

        consumer.consume(current);

        PositionChangedEvent older =
                createEvent(
                        UUID.randomUUID(),
                        new BigDecimal("10"),
                        new BigDecimal("100.00"),
                        9
                );

        consumer.consume(older);

        Optional<PositionProjection> result =
                projectionRepository.find(
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .isPresent();

        assertThat(result.get().quantity())
                .isEqualByComparingTo("20");

        assertThat(result.get().averageCost())
                .isEqualByComparingTo("150.00");

        assertThat(result.get().executionSequence())
                .isEqualTo(10);
    }

    @Test
    void shouldDeleteProjectionWhenPositionIsClosed() {
        PositionChangedEvent opened =
                createEvent(
                        UUID.randomUUID(),
                        new BigDecimal("20"),
                        new BigDecimal("150.00"),
                        10
                );

        consumer.consume(opened);

        PositionChangedEvent closed =
                createEvent(
                        UUID.randomUUID(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        11
                );

        consumer.consume(closed);

        Optional<PositionProjection> result =
                projectionRepository.find(
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .isEmpty();
    }

    @Test
    void shouldProcessEventsWithIncreasingSequence() {
        consumer.consume(
                createEvent(
                        UUID.randomUUID(),
                        new BigDecimal("10"),
                        new BigDecimal("100.00"),
                        1
                )
        );

        consumer.consume(
                createEvent(
                        UUID.randomUUID(),
                        new BigDecimal("15"),
                        new BigDecimal("105.00"),
                        2
                )
        );

        consumer.consume(
                createEvent(
                        UUID.randomUUID(),
                        new BigDecimal("20"),
                        new BigDecimal("110.00"),
                        3
                )
        );

        Optional<PositionProjection> result =
                projectionRepository.find(
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .isPresent();

        assertThat(result.get().quantity())
                .isEqualByComparingTo("20");

        assertThat(result.get().averageCost())
                .isEqualByComparingTo("110.00");

        assertThat(result.get().executionSequence())
                .isEqualTo(3);
    }

    private PositionChangedEvent createEvent(
            UUID eventId,
            BigDecimal quantity,
            BigDecimal averageCost,
            long sequence
    ) {
        return new PositionChangedEvent(
                eventId,
                portfolioId,
                assetId,
                quantity,
                averageCost,
                "EUR",
                sequence,
                Instant.parse(
                        "2026-10-05T10:00:00Z"
                )
        );
    }
}