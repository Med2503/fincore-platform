package org.fincore.wealth.portfolio.domain;


import org.fincore.wealth.portfolio.domain.MarketPricePolicy;
import org.fincore.wealth.position.application.port.MarketPriceProvider;
import org.fincore.wealth.position.domain.MarketPrice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MarketPricePolicyTest {

    @Test
    void acceptsPriceWithinMaximumAge() {
        var policy = new MarketPricePolicy(Duration.ofMinutes(15));
        Instant now = Instant.parse("2026-10-02T10:15:00Z");

        var price = new MarketPrice(
                UUID.randomUUID(),
                new BigDecimal("120.50"),
                "USD",
                now.minus(Duration.ofMinutes(10))
        );

        assertTrue(policy.isFresh(price, now));
    }

    @Test
    void rejectsStaleOrFuturePrice() {
        var policy = new MarketPricePolicy(Duration.ofMinutes(15));
        Instant now = Instant.parse("2026-10-02T10:15:00Z");

        var stale = new MarketPrice(
                UUID.randomUUID(),
                new BigDecimal("120.50"),
                "USD",
                now.minus(Duration.ofMinutes(20))
        );

        var future = new MarketPrice(
                UUID.randomUUID(),
                new BigDecimal("120.50"),
                "USD",
                now.plusSeconds(30)
        );

        assertFalse(policy.isFresh(stale, now));
        assertFalse(policy.isFresh(future, now));
    }
}