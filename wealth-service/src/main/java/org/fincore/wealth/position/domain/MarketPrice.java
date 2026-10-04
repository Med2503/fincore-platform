package org.fincore.wealth.position.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MarketPrice(
        UUID assetId,
        BigDecimal price,
        String currency,
        Instant observedAt
) {

    public MarketPrice {
        Objects.requireNonNull(
                assetId,
                "Asset ID is required"
        );

        Objects.requireNonNull(
                price,
                "Price is required"
        );

        Objects.requireNonNull(
                currency,
                "Currency is required"
        );

        Objects.requireNonNull(
                observedAt,
                "Observed at is required"
        );

        if (price.signum() < 0) {
            throw new IllegalArgumentException(
                    "Price cannot be negative"
            );
        }

        if (currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Currency cannot be blank"
            );
        }
    }
}