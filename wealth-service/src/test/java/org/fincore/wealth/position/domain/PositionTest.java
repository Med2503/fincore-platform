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
    private final Instant now = Instant.parse("2026-09-30T10:00:00Z");

    @Test
    void buyCalculatesWeightedAverageCost() {
        Position initial = Position.open(
                portfolioId, assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO, "USD", now
        );

        Position updated = initial.buy(
                new BigDecimal("10"),
                new BigDecimal("120"),
                BigDecimal.ZERO, "USD", now
        );

        assertEquals(new BigDecimal("20"), updated.quantity());
        assertEquals(
                0,
                new BigDecimal("110").compareTo(updated.averageCost())
        );
    }

    @Test
    void sellCalculatesRealizedProfitAndPreservesAverageCost() {
        Position position = Position.open(
                portfolioId, assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO, "USD", now
        );

        var result = position.sell(
                new BigDecimal("4"),
                new BigDecimal("130"),
                new BigDecimal("2"),
                "USD", now
        );

        assertEquals(
                0,
                new BigDecimal("118").compareTo(result.realizedPnl())
        );
        assertEquals(new BigDecimal("6"), result.remainingPosition().quantity());
        assertEquals(
                0,
                new BigDecimal("100").compareTo(
                        result.remainingPosition().averageCost()
                )
        );
    }

    @Test
    void rejectsSaleAboveAvailableQuantity() {
        Position position = Position.open(
                portfolioId, assetId,
                new BigDecimal("2"),
                new BigDecimal("50"),
                BigDecimal.ZERO, "USD", now
        );

        assertThrows(
                InsufficientPositionQuantityException.class,
                () -> position.sell(
                        new BigDecimal("3"),
                        new BigDecimal("55"),
                        BigDecimal.ZERO,
                        "USD", now
                )
        );
    }
}