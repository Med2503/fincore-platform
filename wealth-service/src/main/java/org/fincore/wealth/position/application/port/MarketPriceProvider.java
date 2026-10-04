package org.fincore.wealth.position.application.port;

import org.fincore.wealth.position.domain.MarketPrice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MarketPriceProvider {

    //Optional<MarketPrice> findLatestPrice(UUID assetId);


    Map<UUID, MarketPrice> findLatestPrices(
            Set<UUID> assetIds
    );


}