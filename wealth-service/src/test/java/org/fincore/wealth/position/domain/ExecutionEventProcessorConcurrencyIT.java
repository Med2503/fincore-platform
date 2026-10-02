package org.fincore.wealth.position.domain;

import org.fincore.wealth.portfolio.domain.PortfolioStatus;
import org.fincore.wealth.portfolio.infrastructure.persistence.PortfolioJpaEntity;
import org.fincore.wealth.portfolio.infrastructure.persistence.PortfolioJpaRepository;
import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.domain.ExecutionEvent;
import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.service.ExecutionEventProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class ExecutionEventProcessorConcurrencyIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("wealth_test")
                    .withUsername("wealth")
                    .withPassword("wealth");

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
    private ExecutionEventProcessor processor;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PortfolioJpaRepository portfolioRepository;

    @Autowired
    private PositionRepository positionRepository;

    private UUID portfolioId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        portfolioId = UUID.randomUUID();
        assetId = UUID.randomUUID();

        createPortfolio();
    }

    @Test
    void shouldProcessConcurrentBuysIntoSinglePosition()
            throws Exception {

        ExecutionEvent firstEvent = createBuyEvent(
                new BigDecimal("10"),
                new BigDecimal("100")
        );

        ExecutionEvent secondEvent = createBuyEvent(
                new BigDecimal("20"),
                new BigDecimal("110")
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch start = new CountDownLatch(1);

        Future<?> firstTransaction = executor.submit(
                () -> processConcurrently(firstEvent, start)
        );

        Future<?> secondTransaction = executor.submit(
                () -> processConcurrently(secondEvent, start)
        );

        start.countDown();

        firstTransaction.get(15, TimeUnit.SECONDS);
        secondTransaction.get(15, TimeUnit.SECONDS);

        executor.shutdown();

        Optional<Position> position =
                transactionTemplate.execute(status ->
                        positionRepository.findForUpdate(
                                portfolioId,
                                assetId
                        )
                );

        assertTrue(position.isPresent());

        assertEquals(
                new BigDecimal("30"),
                position.orElseThrow().quantity()
        );
    }

    private void processConcurrently(
            ExecutionEvent event,
            CountDownLatch start
    ) {
        try {
            start.await();

            transactionTemplate.executeWithoutResult(
                    status -> processor.process(event)
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Concurrent test interrupted",
                    exception
            );
        }
    }

    private void createPortfolio() {
        PortfolioJpaEntity portfolio =
                new PortfolioJpaEntity();

        portfolio.setId(portfolioId);
        portfolio.setUserId(UUID.randomUUID());
        portfolio.setName("Concurrent Test Portfolio");
        portfolio.setBaseCurrency("USD");
        portfolio.setStatus(PortfolioStatus.ACTIVE);

        Instant now = Instant.now();

        portfolio.setCreatedAt(now);
        portfolio.setUpdatedAt(now);

        portfolioRepository.saveAndFlush(portfolio);
    }

    private ExecutionEvent createBuyEvent(
            BigDecimal quantity,
            BigDecimal price
    ) {
        return new ExecutionEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                portfolioId,
                assetId,
                ExecutionEvent.Side.BUY,
                quantity,
                price,
                BigDecimal.ZERO,
                "USD",
                Instant.now()
        );
    }

    private void createInitialPosition() {
        Position position = Position.open(
                portfolioId,
                assetId,
                new BigDecimal("100"),
                new BigDecimal("90"),
                BigDecimal.ZERO,
                "USD",
                Instant.now()
        );

        transactionTemplate.executeWithoutResult(
                status -> positionRepository.save(position)
        );
    }

    @Test
    void shouldSerializeConcurrentBuysOnExistingPosition()
            throws Exception {

        createInitialPosition();

        ExecutionEvent firstEvent = createBuyEvent(
                new BigDecimal("10"),
                new BigDecimal("100")
        );

        ExecutionEvent secondEvent = createBuyEvent(
                new BigDecimal("20"),
                new BigDecimal("110")
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch start = new CountDownLatch(1);

        Future<?> firstTransaction = executor.submit(
                () -> processConcurrently(firstEvent, start)
        );

        Future<?> secondTransaction = executor.submit(
                () -> processConcurrently(secondEvent, start)
        );

        start.countDown();

        firstTransaction.get(15, TimeUnit.SECONDS);
        secondTransaction.get(15, TimeUnit.SECONDS);

        executor.shutdown();

        Optional<Position> position =
                transactionTemplate.execute(status ->
                        positionRepository.findForUpdate(
                                portfolioId,
                                assetId
                        )
                );

        assertTrue(position.isPresent());

        assertEquals(
                new BigDecimal("130"),
                position.orElseThrow().quantity()
        );
    }

    private ExecutionEvent createSellEvent(
            BigDecimal quantity,
            BigDecimal price
    ) {
        return new ExecutionEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                portfolioId,
                assetId,
                ExecutionEvent.Side.SELL,
                quantity,
                price,
                BigDecimal.ZERO,
                "USD",
                Instant.now()
        );
    }

    @Test
    void shouldSerializeConcurrentBuyAndSell()
            throws Exception {

        createInitialPosition();

        ExecutionEvent buyEvent = createBuyEvent(
                new BigDecimal("20"),
                new BigDecimal("100")
        );

        ExecutionEvent sellEvent = createSellEvent(
                new BigDecimal("30"),
                new BigDecimal("110")
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch start = new CountDownLatch(1);

        Future<?> buyTransaction = executor.submit(
                () -> processConcurrently(buyEvent, start)
        );

        Future<?> sellTransaction = executor.submit(
                () -> processConcurrently(sellEvent, start)
        );

        start.countDown();

        buyTransaction.get(15, TimeUnit.SECONDS);
        sellTransaction.get(15, TimeUnit.SECONDS);

        executor.shutdown();

        Optional<Position> position =
                transactionTemplate.execute(status ->
                        positionRepository.findForUpdate(
                                portfolioId,
                                assetId
                        )
                );

        assertTrue(position.isPresent());

        assertEquals(
                new BigDecimal("90"),
                position.orElseThrow().quantity()
        );
    }


}