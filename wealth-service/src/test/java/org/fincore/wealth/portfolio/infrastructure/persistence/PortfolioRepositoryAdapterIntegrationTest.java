package org.fincore.wealth.portfolio.infrastructure.persistence;


import org.fincore.wealth.portfolio.domain.Portfolio;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.fincore.wealth.portfolio.domain.PortfolioStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class PortfolioRepositoryAdapterIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
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

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "true"
        );
    }

    @Autowired
    private PortfolioRepository repository;

    @Autowired
    private PortfolioJpaRepository springDataRepository;

    @BeforeEach
    void cleanDatabase() {
        springDataRepository.deleteAll();
    }

    @Test
    void shouldPersistAndRetrievePortfolio() {
        UUID userId = UUID.randomUUID();

        Portfolio portfolio = Portfolio.create(
                UUID.randomUUID(),
                userId,
                "Long Term Portfolio",
                "EUR",
                Instant.now()
        );

        Portfolio saved = repository.save(portfolio);

        Optional<Portfolio> result =
                repository.findOwnedById(
                        saved.getId(),
                        userId
                );

        assertThat(result).isPresent();

        Portfolio loaded = result.orElseThrow();

        assertThat(loaded.getId())
                .isEqualTo(saved.getId());

        assertThat(loaded.getUserId())
                .isEqualTo(userId);

        assertThat(loaded.getName())
                .isEqualTo("Long Term Portfolio");

        assertThat(loaded.getBaseCurrency())
                .isEqualTo("EUR");

        assertThat(loaded.getStatus())
                .isEqualTo(PortfolioStatus.ACTIVE);
    }

    @Test
    void shouldNotReturnPortfolioOwnedByAnotherUser() {
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();

        Portfolio portfolio = Portfolio.create(
                UUID.randomUUID(),
                ownerId,
                "Private Portfolio",
                "EUR",
                Instant.now()
        );

        Portfolio saved = repository.save(portfolio);

        Optional<Portfolio> result =
                repository.findOwnedById(
                        saved.getId(),
                        attackerId
                );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindPortfoliosByUser() {
        UUID userId = UUID.randomUUID();

        Portfolio first = Portfolio.create(
                UUID.randomUUID(),
                userId,
                "Portfolio One",
                "EUR",
                Instant.now()
        );

        Portfolio second = Portfolio.create(
                UUID.randomUUID(),
                userId,
                "Portfolio Two",
                "USD",
                Instant.now()
        );

        repository.save(first);
        repository.save(second);

        var result =
                repository.findAllByUserId(
                        userId,
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                20
                        )
                );

        assertThat(result.getTotalElements())
                .isEqualTo(2);

        assertThat(result.getContent())
                .extracting(Portfolio::getName)
                .containsExactlyInAnyOrder(
                        "Portfolio One",
                        "Portfolio Two"
                );
    }

    @Test
    void shouldReturnEmptyForUnknownUser() {
        UUID existingUserId = UUID.randomUUID();
        UUID unknownUserId = UUID.randomUUID();

        Portfolio portfolio = Portfolio.create(
                UUID.randomUUID(),
                existingUserId,
                "Portfolio",
                "EUR",
                Instant.now()
        );

        repository.save(portfolio);

        var result =
                repository.findAllByUserId(
                        unknownUserId,
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                20
                        )
                );

        assertThat(result.getTotalElements())
                .isZero();
    }
}
