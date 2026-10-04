package org.fincore.wealth.position.domain;

import org.fincore.wealth.portfolio.domain.PortfolioStatus;
import org.fincore.wealth.portfolio.infrastructure.persistence.PortfolioJpaEntity;
import org.fincore.wealth.portfolio.infrastructure.persistence.PortfolioJpaRepository;
import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.application.port.ProcessedEventRepository;
import org.fincore.wealth.position.infrastructure.persistence.SpringDataPositionRepository;
import org.fincore.wealth.position.service.ExecutionEventProcessor;
import org.junit.jupiter.api.AfterEach;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ExecutionEventProcessorConcurrencyIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            );

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
    }

    @Autowired
    private ExecutionEventProcessor processor;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PortfolioJpaRepository portfolioRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private SpringDataPositionRepository positionJpaRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private UUID portfolioId;
    private UUID assetId;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        portfolioId = UUID.randomUUID();
        assetId = UUID.randomUUID();

        executor = Executors.newFixedThreadPool(2);

        PortfolioJpaEntity portfolio =
                new PortfolioJpaEntity();

        portfolio.setId(portfolioId);
        portfolio.setUserId(UUID.randomUUID());
        portfolio.setName(
                "Concurrent Test Portfolio"
        );
        portfolio.setBaseCurrency("USD");
        portfolio.setStatus(
                PortfolioStatus.ACTIVE
        );

        Instant now = Instant.now();

        portfolio.setCreatedAt(now);
        portfolio.setUpdatedAt(now);

        portfolioRepository.saveAndFlush(portfolio);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void shouldProcessConcurrentBuysIntoSinglePosition()
            throws Exception {

        ExecutionEvent firstBuy =
                createBuyEvent(
                        100L,
                        "10",
                        "100"
                );

        ExecutionEvent secondBuy =
                createBuyEvent(
                        101L,
                        "20",
                        "110"
                );

        CountDownLatch start =
                new CountDownLatch(1);

        Future<?> first =
                executor.submit(() ->
                        executeInTransaction(
                                start,
                                firstBuy
                        )
                );

        Future<?> second =
                executor.submit(() ->
                        executeInTransaction(
                                start,
                                secondBuy
                        )
                );

        start.countDown();

        first.get();
        second.get();

        Position position =
                positionRepository
                        .findForUpdate(
                                portfolioId,
                                assetId
                        )
                        .orElseThrow();

        assertThat(position.quantity())
                .isEqualByComparingTo("30");

        assertThat(position.lastExecutionSequence())
                .isEqualTo(101L);
    }

    @Test
    void shouldSerializeConcurrentBuysOnExistingPosition()
            throws Exception {

        createInitialPosition(
                "100",
                "90",
                99L
        );

        ExecutionEvent firstBuy =
                createBuyEvent(
                        100L,
                        "10",
                        "100"
                );

        ExecutionEvent secondBuy =
                createBuyEvent(
                        101L,
                        "20",
                        "110"
                );

        CountDownLatch start =
                new CountDownLatch(1);

        Future<?> first =
                executor.submit(() ->
                        executeInTransaction(
                                start,
                                firstBuy
                        )
                );

        Future<?> second =
                executor.submit(() ->
                        executeInTransaction(
                                start,
                                secondBuy
                        )
                );

        start.countDown();

        first.get();
        second.get();

        Position position =
                positionRepository
                        .findForUpdate(
                                portfolioId,
                                assetId
                        )
                        .orElseThrow();

        assertThat(position.quantity())
                .isEqualByComparingTo("130");

        assertThat(position.lastExecutionSequence())
                .isEqualTo(101L);
    }

    @Test
    void shouldSerializeConcurrentBuyAndSell()
            throws Exception {

        createInitialPosition(
                "100",
                "90",
                99L
        );

        ExecutionEvent buy =
                createBuyEvent(
                        100L,
                        "20",
                        "100"
                );

        ExecutionEvent sell =
                createSellEvent(
                        101L,
                        "30",
                        "110"
                );

        CountDownLatch start =
                new CountDownLatch(1);

        Future<?> buyFuture =
                executor.submit(() ->
                        executeInTransaction(
                                start,
                                buy
                        )
                );

        Future<?> sellFuture =
                executor.submit(() ->
                        executeInTransaction(
                                start,
                                sell
                        )
                );

        start.countDown();

        buyFuture.get();
        sellFuture.get();

        Position position =
                positionRepository
                        .findForUpdate(
                                portfolioId,
                                assetId
                        )
                        .orElseThrow();

        assertThat(position.quantity())
                .isEqualByComparingTo("90");

        assertThat(position.lastExecutionSequence())
                .isEqualTo(101L);
    }

    private void executeInTransaction(
            CountDownLatch start,
            ExecutionEvent event
    ) {
        await(start);

        transactionTemplate.executeWithoutResult(
                status -> processor.process(event)
        );
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Test thread interrupted",
                    exception
            );
        }
    }

    private void createInitialPosition(
            String quantity,
            String averageCost,
            long sequence
    ) {
        Position position = new Position(
                UUID.randomUUID(),
                portfolioId,
                assetId,
                new BigDecimal(quantity),
                new BigDecimal(averageCost),
                "USD",
                Instant.now(),
                sequence
        );

        positionRepository.save(position);
    }

    private ExecutionEvent createBuyEvent(
            long sequence,
            String quantity,
            String price
    ) {
        return new ExecutionEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                portfolioId,
                assetId,
                ExecutionEvent.Side.BUY,
                new BigDecimal(quantity),
                new BigDecimal(price),
                BigDecimal.ZERO,
                "USD",
                Instant.now(),
                sequence
        );
    }

    private ExecutionEvent createSellEvent(
            long sequence,
            String quantity,
            String price
    ) {
        return new ExecutionEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                portfolioId,
                assetId,
                ExecutionEvent.Side.SELL,
                new BigDecimal(quantity),
                new BigDecimal(price),
                BigDecimal.ZERO,
                "USD",
                Instant.now(),
                sequence
        );
    }
    @Test
    void shouldProcessSameEventOnlyOnceConcurrently()
            throws Exception {

        ExecutionEvent event = createBuyEvent(
                100L,
                "10",
                "100"
        );

        CountDownLatch start =
                new CountDownLatch(1);

        Future<ExecutionEventProcessor.ProcessingResult> first =
                executor.submit(() -> {
                    await(start);

                    return transactionTemplate.execute(
                            status -> processor.process(event)
                    );
                });

        Future<ExecutionEventProcessor.ProcessingResult> second =
                executor.submit(() -> {
                    await(start);

                    return transactionTemplate.execute(
                            status -> processor.process(event)
                    );
                });

        start.countDown();

        ExecutionEventProcessor.ProcessingResult firstResult =
                first.get();

        ExecutionEventProcessor.ProcessingResult secondResult =
                second.get();

        assertThat(
                java.util.Set.of(
                        firstResult,
                        secondResult
                )
        )
                .containsExactlyInAnyOrder(
                        ExecutionEventProcessor.ProcessingResult.APPLIED,
                        ExecutionEventProcessor.ProcessingResult.DUPLICATE
                );

        Position position =
                positionRepository
                        .findForUpdate(
                                portfolioId,
                                assetId
                        )
                        .orElseThrow();

        assertThat(position.quantity())
                .isEqualByComparingTo("10");

        assertThat(position.lastExecutionSequence())
                .isEqualTo(100L);
    }
}



