package org.fincore.wealth.position.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExecutionEvent(
        UUID eventId,
        UUID executionId,
        UUID portfolioId,
        UUID assetId,
        Side side,
        BigDecimal quantity,
        BigDecimal executionPrice,
        BigDecimal fees,
        String currency,
        Instant occurredAt
) {
    public enum Side {
        BUY,
        SELL
    }
}