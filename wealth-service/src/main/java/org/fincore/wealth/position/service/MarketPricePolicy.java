package org.fincore.wealth.position.service;

import org.fincore.wealth.position.domain.MarketPrice;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class MarketPricePolicy {

    private final Clock clock;
    private final Duration maxAge;

    public MarketPricePolicy(
            Clock clock,
            MarketPriceProperties properties
    ) {
        this.clock = clock;
        this.maxAge = properties.maxAge();
    }

    public boolean isUsable(MarketPrice marketPrice) {
        Instant now = clock.instant();
        Instant observedAt = marketPrice.observedAt();

        if (observedAt.isAfter(now)) {
            return false;
        }

        Duration age =
                Duration.between(observedAt, now);

        return !age.isNegative()
                && age.compareTo(maxAge) <= 0;
    }
}