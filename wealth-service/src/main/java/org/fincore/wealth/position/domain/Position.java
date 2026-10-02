package org.fincore.wealth.position.domain;

import org.fincore.wealth.position.exception.InsufficientPositionQuantityException;

import java.math.BigDecimal;
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
        Instant updatedAt,
        long lastExecutionSequence
) {

    public Position {
        Objects.requireNonNull(id);
        Objects.requireNonNull(portfolioId);
        Objects.requireNonNull(assetId);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(averageCost);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(updatedAt);

        if (quantity.signum() < 0) {
            throw new IllegalArgumentException(
                    "Quantity cannot be negative"
            );
        }

        if (averageCost.signum() < 0) {
            throw new IllegalArgumentException(
                    "Average cost cannot be negative"
            );
        }

        if (lastExecutionSequence < 0) {
            throw new IllegalArgumentException(
                    "Execution sequence cannot be negative"
            );
        }
    }

    public static Position open(
            UUID portfolioId,
            UUID assetId,
            BigDecimal quantity,
            BigDecimal executionPrice,
            BigDecimal fees,
            String currency,
            Instant occurredAt,
            long executionSequence
    ) {
        validatePositive(quantity, "quantity");
        validateNonNegative(executionPrice, "executionPrice");
        validateNonNegative(fees, "fees");

        return new Position(
                UUID.randomUUID(),
                portfolioId,
                assetId,
                quantity,
                calculateInitialAverageCost(
                        quantity,
                        executionPrice,
                        fees
                ),
                currency,
                occurredAt,
                executionSequence
        );
    }

    public Position buy(
            BigDecimal additionalQuantity,
            BigDecimal executionPrice,
            BigDecimal fees,
            String executionCurrency,
            Instant occurredAt,
            long executionSequence
    ) {
        validatePositive(additionalQuantity, "additionalQuantity");
        validateNonNegative(executionPrice, "executionPrice");
        validateNonNegative(fees, "fees");
        validateCurrency(executionCurrency);

        BigDecimal existingCost =
                quantity.multiply(averageCost);

        BigDecimal additionalCost =
                additionalQuantity
                        .multiply(executionPrice)
                        .add(fees);

        BigDecimal newQuantity =
                quantity.add(additionalQuantity);

        BigDecimal newAverageCost =
                existingCost
                        .add(additionalCost)
                        .divide(
                                newQuantity,
                                10,
                                java.math.RoundingMode.HALF_UP
                        );

        return new Position(
                id,
                portfolioId,
                assetId,
                newQuantity,
                newAverageCost,
                currency,
                occurredAt,
                executionSequence
        );
    }

    public SaleResult sell(
            BigDecimal quantityToSell,
            BigDecimal executionPrice,
            BigDecimal fees,
            String executionCurrency,
            Instant occurredAt,
            long executionSequence
    ) {
        validatePositive(quantityToSell, "quantityToSell");
        validateNonNegative(executionPrice, "executionPrice");
        validateNonNegative(fees, "fees");
        validateCurrency(executionCurrency);

        if (quantityToSell.compareTo(quantity) > 0) {
            throw new InsufficientPositionQuantityException(
                    quantity

            );
        }

        BigDecimal realizedPnl =
                quantityToSell
                        .multiply(executionPrice.subtract(averageCost))
                        .subtract(fees);

        BigDecimal remainingQuantity =
                quantity.subtract(quantityToSell);

        if (remainingQuantity.signum() == 0) {
            return new SaleResult(
                    null,
                    realizedPnl,
                    true
            );
        }

        Position remaining = new Position(
                id,
                portfolioId,
                assetId,
                remainingQuantity,
                averageCost,
                currency,
                occurredAt,
                executionSequence
        );

        return new SaleResult(
                remaining,
                realizedPnl,
                false
        );
    }

    private static BigDecimal calculateInitialAverageCost(
            BigDecimal quantity,
            BigDecimal executionPrice,
            BigDecimal fees
    ) {
        return quantity
                .multiply(executionPrice)
                .add(fees)
                .divide(
                        quantity,
                        10,
                        java.math.RoundingMode.HALF_UP
                );
    }

    private void validateCurrency(String executionCurrency) {
        if (!currency.equals(executionCurrency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch"
            );
        }
    }

    private static void validatePositive(
            BigDecimal value,
            String field
    ) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(
                    field + " must be positive"
            );
        }
    }

    private static void validateNonNegative(
            BigDecimal value,
            String field
    ) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(
                    field + " cannot be negative"
            );
        }
    }

    public record SaleResult(
            Position remainingPosition,
            BigDecimal realizedPnl,
            boolean fullyClosed
    ) {
    }
}