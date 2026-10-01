package org.fincore.wealth.position.domain;

import org.fincore.wealth.position.exception.InsufficientPositionQuantityException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Position(
        UUID id,
        UUID portfolioId,
        UUID assetId,
        BigDecimal quantity,
        BigDecimal averageCost,
        String currency,
        Instant updatedAt
) {
    private static final int CALCULATION_SCALE = 16;

    public Position {
        Objects.requireNonNull(id);
        Objects.requireNonNull(portfolioId);
        Objects.requireNonNull(assetId);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(averageCost);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(updatedAt);

        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("Position quantity must be positive");
        }
        if (averageCost.signum() < 0) {
            throw new IllegalArgumentException("Average cost cannot be negative");
        }
        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be ISO-like 3-letter code");
        }
    }

    public static Position open(
            UUID portfolioId,
            UUID assetId,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal fees,
            String currency,
            Instant now
    ) {
        validateTrade(quantity, price, fees);

        BigDecimal totalCost = quantity.multiply(price).add(fees);
        BigDecimal averageCost = totalCost.divide(
                quantity, CALCULATION_SCALE, RoundingMode.HALF_EVEN
        );

        return new Position(
                UUID.randomUUID(),
                portfolioId,
                assetId,
                quantity,
                averageCost,
                currency.toUpperCase(),
                now
        );
    }

    public Position buy(
            BigDecimal boughtQuantity,
            BigDecimal price,
            BigDecimal fees,
            String tradeCurrency,
            Instant now
    ) {
        validateTrade(boughtQuantity, price, fees);
        requireSameCurrency(tradeCurrency);

        BigDecimal newQuantity = quantity.add(boughtQuantity);
        BigDecimal oldCost = quantity.multiply(averageCost);
        BigDecimal newCost = boughtQuantity.multiply(price).add(fees);

        BigDecimal newAverageCost = oldCost.add(newCost).divide(
                newQuantity, CALCULATION_SCALE, RoundingMode.HALF_EVEN
        );

        return new Position(
                id, portfolioId, assetId, newQuantity,
                newAverageCost, currency, now
        );
    }

    public SaleResult sell(
            BigDecimal soldQuantity,
            BigDecimal price,
            BigDecimal fees,
            String tradeCurrency,
            Instant now
    ) {
        validateTrade(soldQuantity, price, fees);
        requireSameCurrency(tradeCurrency);

        if (soldQuantity.compareTo(quantity) > 0) {
            throw new InsufficientPositionQuantityException("Insuffisant Qauntity");
        }

        BigDecimal realizedPnl = price.subtract(averageCost)
                .multiply(soldQuantity)
                .subtract(fees);

        BigDecimal remaining = quantity.subtract(soldQuantity);

        Position updated = remaining.signum() == 0
                ? null
                : new Position(
                id, portfolioId, assetId, remaining,
                averageCost, currency, now
        );

        return new SaleResult(updated, realizedPnl);
    }

    private void requireSameCurrency(String tradeCurrency) {
        if (!currency.equalsIgnoreCase(tradeCurrency)) {
            throw new IllegalArgumentException("Trade currency mismatch");
        }
    }

    private static void validateTrade(
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal fees
    ) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (fees == null || fees.signum() < 0) {
            throw new IllegalArgumentException("Fees cannot be negative");
        }
    }

    public record SaleResult(Position remainingPosition, BigDecimal realizedPnl) {
        public boolean fullyClosed() {
            return remainingPosition == null;
        }
    }
}