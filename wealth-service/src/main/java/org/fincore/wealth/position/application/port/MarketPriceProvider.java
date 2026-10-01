package org.fincore.wealth.position.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MarketPriceProvider {

    Optional<MarketPrice> findLatestPrice(UUID assetId);

    record MarketPrice(
            UUID assetId,
            BigDecimal price,
            String currency,
            Instant observedAt
    ) {
    }
}