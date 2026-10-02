package org.fincore.wealth.position.domain;

import org.fincore.wealth.position.exception.InsufficientPositionQuantityException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    private final UUID portfolioId = UUID.randomUUID();
    private final UUID assetId = UUID.randomUUID();

    private final Instant now =
            Instant.parse("2026-09-30T10:00:00Z");

    @Test
    void openCreatesPositionWithExecutionSequence() {

        Position position = Position.open(
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                "USD",
                now,
                100L
        );

        assertEquals(
                portfolioId,
                position.portfolioId()
        );

        assertEquals(
                assetId,
                position.assetId()
        );

        assertEquals(
                new BigDecimal("10"),
                position.quantity()
        );

        assertEquals(
                0,
                new BigDecimal("100")
                        .compareTo(position.averageCost())
        );

        assertEquals(
                "USD",
                position.currency()
        );

        assertEquals(
                now,
                position.updatedAt()
        );

        assertEquals(
                100L,
                position.lastExecutionSequence()
        );
    }

    @Test
    void buyCalculatesWeightedAverageCost() {

        Position initial = Position.open(
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                "USD",
                now,
                100L
        );

        Position updated = initial.buy(
                new BigDecimal("10"),
                new BigDecimal("120"),
                BigDecimal.ZERO,
                "USD",
                now,
                101L
        );

        assertEquals(
                new BigDecimal("20"),
                updated.quantity()
        );

        assertEquals(
                0,
                new BigDecimal("110")
                        .compareTo(updated.averageCost())
        );

        assertEquals(
                101L,
                updated.lastExecutionSequence()
        );
    }

    @Test
    void sellCalculatesRealizedProfitAndPreservesAverageCost() {

        Position position = Position.open(
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                "USD",
                now,
                100L
        );

        Position.SaleResult result = position.sell(
                new BigDecimal("4"),
                new BigDecimal("130"),
                new BigDecimal("2"),
                "USD",
                now,
                101L
        );

        assertEquals(
                0,
                new BigDecimal("118")
                        .compareTo(result.realizedPnl())
        );

        assertFalse(result.fullyClosed());

        assertNotNull(result.remainingPosition());

        assertEquals(
                new BigDecimal("6"),
                result.remainingPosition().quantity()
        );

        assertEquals(
                0,
                new BigDecimal("100")
                        .compareTo(
                                result.remainingPosition()
                                        .averageCost()
                        )
        );

        assertEquals(
                101L,
                result.remainingPosition()
                        .lastExecutionSequence()
        );
    }

    @Test
    void sellingEntirePositionReturnsFullyClosedResult() {

        Position position = Position.open(
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                "USD",
                now,
                100L
        );

        Position.SaleResult result = position.sell(
                new BigDecimal("10"),
                new BigDecimal("130"),
                new BigDecimal("2"),
                "USD",
                now,
                101L
        );

        assertTrue(result.fullyClosed());

        assertNull(result.remainingPosition());

        assertEquals(
                0,
                new BigDecimal("298")
                        .compareTo(result.realizedPnl())
        );
    }

    @Test
    void rejectsSaleAboveAvailableQuantity() {

        Position position = Position.open(
                portfolioId,
                assetId,
                new BigDecimal("2"),
                new BigDecimal("50"),
                BigDecimal.ZERO,
                "USD",
                now,
                100L
        );

        assertThrows(
                InsufficientPositionQuantityException.class,
                () -> position.sell(
                        new BigDecimal("3"),
                        new BigDecimal("55"),
                        BigDecimal.ZERO,
                        "USD",
                        now,
                        101L
                )
        );
    }

    @Test
    void buyWithFeesIncludesFeesInAverageCost() {

        Position position = Position.open(
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                "USD",
                now,
                100L
        );

        Position updated = position.buy(
                new BigDecimal("10"),
                new BigDecimal("120"),
                new BigDecimal("20"),
                "USD",
                now,
                101L
        );

        /*
         * Old cost = 10 * 100 = 1000
         * New cost = 10 * 120 = 1200
         * Fees      = 20
         *
         * Total     = 2220
         * Quantity  = 20
         *
         * Average   = 111
         */
        assertEquals(
                0,
                new BigDecimal("111")
                        .compareTo(updated.averageCost())
        );

        assertEquals(
                101L,
                updated.lastExecutionSequence()
        );
    }

    @Test
    void sellWithDifferentCurrencyIsRejected() {

        Position position = Position.open(
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                "USD",
                now,
                100L
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> position.sell(
                        new BigDecimal("2"),
                        new BigDecimal("130"),
                        BigDecimal.ZERO,
                        "EUR",
                        now,
                        101L
                )
        );
    }
}