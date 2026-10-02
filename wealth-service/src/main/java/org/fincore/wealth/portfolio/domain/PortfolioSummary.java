package org.fincore.wealth.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PortfolioSummary(
        String currency,
        int positionCount,
        int valuedPositionCount,
        int missingPriceCount,
        int stalePriceCount,
        BigDecimal totalCostBasis,
        BigDecimal totalMarketValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPercentage,
        Instant valuedAt
) {
}