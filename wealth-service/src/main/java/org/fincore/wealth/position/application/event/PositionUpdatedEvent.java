package org.fincore.wealth.position.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PositionUpdatedEvent(
        UUID eventId,
        UUID portfolioId,
        UUID assetId,
        BigDecimal quantity,
        BigDecimal averageCost,
        String currency,
        long executionSequence,
        Instant occurredAt
) {
}
