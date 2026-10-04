package org.fincore.wealth.position.service;

import org.fincore.wealth.position.domain.MarketPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MarketPricePolicyTest {

    private static final Instant NOW =
            Instant.parse("2026-10-04T10:00:00Z");

    private MarketPricePolicy policy;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                NOW,
                ZoneOffset.UTC
        );

        MarketPriceProperties properties =
                new MarketPriceProperties(
                        Duration.ofSeconds(30)
                );

        policy = new MarketPricePolicy(
                clock,
                properties
        );
    }

    @Test
    void shouldAcceptFreshPrice() {
        MarketPrice price = marketPrice(
                "2026-10-04T09:59:45Z"
        );

        assertThat(policy.isUsable(price))
                .isTrue();
    }

    @Test
    void shouldAcceptPriceExactlyAtMaximumAge() {
        MarketPrice price = marketPrice(
                "2026-10-04T09:59:30Z"
        );

        assertThat(policy.isUsable(price))
                .isTrue();
    }

    @Test
    void shouldRejectExpiredPrice() {
        MarketPrice price = marketPrice(
                "2026-10-04T09:59:29Z"
        );

        assertThat(policy.isUsable(price))
                .isFalse();
    }

    @Test
    void shouldRejectFuturePrice() {
        MarketPrice price = marketPrice(
                "2026-10-04T10:00:01Z"
        );

        assertThat(policy.isUsable(price))
                .isFalse();
    }

    private MarketPrice marketPrice(String observedAt) {
        return new MarketPrice(
                UUID.randomUUID(),
                new BigDecimal("100.50"),
                "USD",
                Instant.parse(observedAt)
        );
    }
}