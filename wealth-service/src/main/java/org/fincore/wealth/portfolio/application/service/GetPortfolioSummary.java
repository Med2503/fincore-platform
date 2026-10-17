
package org.fincore.wealth.portfolio.application.service;

import org.fincore.wealth.portfolio.application.exception.PortfolioNotFoundException;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.fincore.wealth.portfolio.domain.PortfolioSummary;
import org.fincore.wealth.position.application.port.MarketPriceProvider;
import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.domain.MarketPrice;
import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.domain.PositionValuation;
import org.fincore.wealth.position.exception.MixedPositionCurrencyException;
import org.fincore.wealth.position.service.MarketPricePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GetPortfolioSummary {

    private final PortfolioRepository portfolios;
    private final PositionRepository positions;
    private final MarketPriceProvider prices;
    private final MarketPricePolicy pricePolicy;
    private final Clock clock;

    public GetPortfolioSummary(
            PortfolioRepository portfolios,
            PositionRepository positions,
            MarketPriceProvider prices,
            MarketPricePolicy pricePolicy,
            Clock clock
    ) {
        this.portfolios = portfolios;
        this.positions = positions;
        this.prices = prices;
        this.pricePolicy = pricePolicy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PortfolioSummary execute(
            UUID userId,
            UUID portfolioId
    ) {
        portfolios.findOwnedById(portfolioId, userId)
                .orElseThrow(() ->
                        new PortfolioNotFoundException("Portfolio not found")
                );

        var portfolioPositions =
                positions.findAllByPortfolioId(portfolioId);

        Instant now = clock.instant();

        if (portfolioPositions.isEmpty()) {
            return emptySummary(now);
        }

        String currency = portfolioPositions.getFirst().currency();

        for (Position position : portfolioPositions) {
            if (!currency.equalsIgnoreCase(position.currency())) {
                throw new MixedPositionCurrencyException(
                        "Portfolio positions must use the same currency"
                );
            }
        }

        Set<UUID> assetIds = portfolioPositions.stream()
                .map(Position::assetId)
                .collect(Collectors.toSet());

        Map<UUID, MarketPrice> marketPrices =
                prices.findLatestPrices(assetIds);

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalMarketValue = BigDecimal.ZERO;

        int valued = 0;
        int missing = 0;
        int stale = 0;

        for (Position position : portfolioPositions) {
            totalCost = totalCost.add(
                    position.quantity().multiply(position.averageCost())
            );

            MarketPrice marketPrice =
                    marketPrices.get(position.assetId());

            if (marketPrice == null) {
                missing++;
                continue;
            }

            if (!currency.equalsIgnoreCase(marketPrice.currency())) {
                missing++;
                continue;
            }

            if (!pricePolicy.isUsable(marketPrice, now)) {
                stale++;
                continue;
            }

            PositionValuation valuation =
                    PositionValuation.calculate(
                            position,
                            marketPrice.price()
                    );

            totalMarketValue = totalMarketValue.add(
                    valuation.marketValue()
            );

            valued++;
        }

        BigDecimal pnl = valued == portfolioPositions.size()
                ? totalMarketValue.subtract(totalCost)
                : null;

        BigDecimal pnlPercentage =
                pnl == null || totalCost.signum() == 0
                        ? null
                        : pnl.multiply(BigDecimal.valueOf(100))
                        .divide(
                                totalCost,
                                8,
                                RoundingMode.HALF_EVEN
                        );

        return new PortfolioSummary(
                currency,
                portfolioPositions.size(),
                valued,
                missing,
                stale,
                totalCost,
                valued == 0 ? null : totalMarketValue,
                pnl,
                pnlPercentage,
                now
        );
    }

    private PortfolioSummary emptySummary(Instant now) {
        return new PortfolioSummary(
                null,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                now
        );
    }
}
