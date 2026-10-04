package org.fincore.wealth.portfolio.domain;

import org.fincore.wealth.position.service.MarketPricePolicy;
import org.fincore.wealth.position.domain.MarketPrice;
import org.fincore.wealth.position.service.MarketPriceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MarketPricePolicyTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-10-04T10:00:00Z"
            );

    private MarketPricePolicy policy;

    @BeforeEach
    void setUp() {
        Clock clock =
                Clock.fixed(
                        NOW,
                        ZoneOffset.UTC
                );

        MarketPriceProperties properties =
                new MarketPriceProperties(
                        java.time.Duration.ofSeconds(30)
                );

        policy = new MarketPricePolicy(
                clock,
                properties
        );
    }

    @Test
    void shouldAcceptFreshPrice() {
        MarketPrice price = priceAt(
                NOW.minusSeconds(10)
        );

        assertThat(
                policy.isUsable(price)
        ).isTrue();
    }

    @Test
    void shouldAcceptPriceExactlyAtMaximumAge() {
        MarketPrice price = priceAt(
                NOW.minusSeconds(30)
        );

        assertThat(
                policy.isUsable(price)
        ).isTrue();
    }

    @Test
    void shouldRejectExpiredPrice() {
        MarketPrice price = priceAt(
                NOW.minusSeconds(31)
        );

        assertThat(
                policy.isUsable(price)
        ).isFalse();
    }

    @Test
    void shouldRejectFuturePrice() {
        MarketPrice price = priceAt(
                NOW.plusSeconds(1)
        );

        assertThat(
                policy.isUsable(price)
        ).isFalse();
    }

    private MarketPrice priceAt(
            Instant observedAt
    ) {
        return new MarketPrice(
                UUID.randomUUID(),
                new BigDecimal("100"),
                "USD",
                observedAt
        );
    }
}