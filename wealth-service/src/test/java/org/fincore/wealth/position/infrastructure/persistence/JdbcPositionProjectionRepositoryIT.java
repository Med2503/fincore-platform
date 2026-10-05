package org.fincore.wealth.position.infrastructure.persistence;

import org.fincore.wealth.position.application.port.PositionProjectionRepository;
import org.fincore.wealth.position.application.projection.PositionProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class JdbcPositionProjectionRepositoryIT {

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
    private PositionProjectionRepository repository;

    private UUID portfolioId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        portfolioId = UUID.randomUUID();
        assetId = UUID.randomUUID();
    }

    @Test
    void shouldSaveAndFindProjection() {
        PositionProjection projection =
                projection(
                        new BigDecimal("10.0000000000"),
                        new BigDecimal("125.5000000000"),
                        10
                );

        repository.save(projection);

        Optional<PositionProjection> result =
                repository.find(
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .isPresent();

        assertThat(result.get().portfolioId())
                .isEqualTo(portfolioId);

        assertThat(result.get().assetId())
                .isEqualTo(assetId);

        assertThat(result.get().quantity())
                .isEqualByComparingTo(
                        "10.0000000000"
                );

        assertThat(result.get().averageCost())
                .isEqualByComparingTo(
                        "125.5000000000"
                );

        assertThat(result.get().currency())
                .isEqualTo("EUR");

        assertThat(result.get().executionSequence())
                .isEqualTo(10);
    }

    @Test
    void shouldUpdateProjectionWhenSequenceIsHigher() {
        repository.save(
                projection(
                        new BigDecimal("10"),
                        new BigDecimal("100"),
                        10
                )
        );

        repository.save(
                projection(
                        new BigDecimal("15"),
                        new BigDecimal("110"),
                        11
                )
        );

        Optional<PositionProjection> result =
                repository.find(
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .isPresent();

        assertThat(result.get().quantity())
                .isEqualByComparingTo("15");

        assertThat(result.get().averageCost())
                .isEqualByComparingTo("110");

        assertThat(result.get().executionSequence())
                .isEqualTo(11);
    }

    @Test
    void shouldIgnoreOlderProjection() {
        repository.save(
                projection(
                        new BigDecimal("15"),
                        new BigDecimal("110"),
                        10
                )
        );

        repository.save(
                projection(
                        new BigDecimal("5"),
                        new BigDecimal("90"),
                        9
                )
        );

        Optional<PositionProjection> result =
                repository.find(
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .isPresent();

        assertThat(result.get().quantity())
                .isEqualByComparingTo("15");

        assertThat(result.get().averageCost())
                .isEqualByComparingTo("110");

        assertThat(result.get().executionSequence())
                .isEqualTo(10);
    }

    @Test
    void shouldIgnoreProjectionWithSameSequence() {
        repository.save(
                projection(
                        new BigDecimal("10"),
                        new BigDecimal("100"),
                        10
                )
        );

        repository.save(
                projection(
                        new BigDecimal("20"),
                        new BigDecimal("200"),
                        10
                )
        );

        Optional<PositionProjection> result =
                repository.find(
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .isPresent();

        assertThat(result.get().quantity())
                .isEqualByComparingTo("10");

        assertThat(result.get().averageCost())
                .isEqualByComparingTo("100");

        assertThat(result.get().executionSequence())
                .isEqualTo(10);
    }

    @Test
    void shouldDeleteProjection() {
        repository.save(
                projection(
                        new BigDecimal("10"),
                        new BigDecimal("100"),
                        10
                )
        );

        repository.delete(
                portfolioId,
                assetId
        );

        Optional<PositionProjection> result =
                repository.find(
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenProjectionDoesNotExist() {
        Optional<PositionProjection> result =
                repository.find(
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .isEmpty();
    }

    private PositionProjection projection(
            BigDecimal quantity,
            BigDecimal averageCost,
            long sequence
    ) {
        return new PositionProjection(
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