package org.fincore.wealth.position.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record PositionValuation(
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPercentage
) {
    public static PositionValuation calculate(
            Position position,
            BigDecimal marketPrice
    ) {
        Objects.requireNonNull(position);
        Objects.requireNonNull(marketPrice);

        if (marketPrice.signum() < 0) {
            throw new IllegalArgumentException("Market price cannot be negative");
        }

        BigDecimal marketValue = position.quantity().multiply(marketPrice);
        BigDecimal costBasis = position.quantity().multiply(position.averageCost());
        BigDecimal pnl = marketValue.subtract(costBasis);

        BigDecimal percentage = costBasis.signum() == 0
                ? BigDecimal.ZERO
                : pnl.multiply(BigDecimal.valueOf(100))
                .divide(costBasis, 8, RoundingMode.HALF_EVEN);

        return new PositionValuation(marketValue, pnl, percentage);
    }
}