package org.fincore.wealth.position.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PositionResponse(
        UUID assetId,
        BigDecimal quantity,
        BigDecimal averageCost,
        String currency,
        BigDecimal marketPrice,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPercentage,
        Instant priceObservedAt,
        boolean priceAvailable
) {
}