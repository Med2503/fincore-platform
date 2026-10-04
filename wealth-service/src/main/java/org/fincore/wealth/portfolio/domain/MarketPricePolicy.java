package org.fincore.wealth.portfolio.domain;


import org.fincore.wealth.position.domain.MarketPrice;

import java.time.Duration;
import java.time.Instant;

public class MarketPricePolicy {

    private final Duration maxAge;

    public MarketPricePolicy(Duration maxAge) {
        if (maxAge == null || maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("maxAge must be positive");
        }
        this.maxAge = maxAge;
    }

    public boolean isFresh(
            MarketPrice price,
            Instant now
    ) {
        if (price == null || price.observedAt() == null) {
            return false;
        }

        Duration age = Duration.between(price.observedAt(), now);
        return !age.isNegative() && age.compareTo(maxAge) <= 0;
    }
}
