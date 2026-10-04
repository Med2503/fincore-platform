package org.fincore.wealth.position.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PositionChangedEvent(
        UUID eventId,
        UUID portfolioId,
        UUID assetId,
        BigDecimal quantity,
        BigDecimal averageCost,
        String currency,
        long executionSequence,
        Instant occurredAt
) {

    public PositionChangedEvent {
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(portfolioId, "Portfolio ID is required");
        Objects.requireNonNull(assetId, "Asset ID is required");
        Objects.requireNonNull(quantity, "Quantity is required");
        Objects.requireNonNull(averageCost, "Average cost is required");
        Objects.requireNonNull(currency, "Currency is required");
        Objects.requireNonNull(occurredAt, "Occurred at is required");

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

        if (currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Currency cannot be blank"
            );
        }

        if (executionSequence <= 0) {
            throw new IllegalArgumentException(
                    "Execution sequence must be positive"
            );
        }
    }
}
