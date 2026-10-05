package org.fincore.wealth.position.application.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PositionProjection(
        UUID portfolioId,
        UUID assetId,
        BigDecimal quantity,
        BigDecimal averageCost,
        String currency,
        long executionSequence,
        Instant updatedAt
) {
}