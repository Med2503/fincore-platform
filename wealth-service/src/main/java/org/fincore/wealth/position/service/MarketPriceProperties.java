package org.fincore.wealth.position.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "wealth.market-price"
)
public record MarketPriceProperties(
        Duration maxAge
) {

    public MarketPriceProperties {
        if (maxAge == null) {
            throw new IllegalArgumentException(
                    "Market price max age is required"
            );
        }

        if (maxAge.isNegative()
                || maxAge.isZero()) {

            throw new IllegalArgumentException(
                    "Market price max age must be positive"
            );
        }
    }
}