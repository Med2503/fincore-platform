package org.fincore.wealth.position.service;

import org.fincore.wealth.portfolio.application.exception.PortfolioNotFoundException;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.fincore.wealth.position.api.dto.PositionPageResponse;
import org.fincore.wealth.position.api.dto.PositionResponse;
import org.fincore.wealth.position.application.port.MarketPriceProvider;
import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.domain.MarketPrice;
import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.domain.PositionValuation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ListPortfolioPositions {

    private final PortfolioRepository portfolios;
    private final PositionRepository positions;
    private final MarketPriceProvider prices;
    private final MarketPricePolicy pricePolicy;

    public ListPortfolioPositions(
            PortfolioRepository portfolios,
            PositionRepository positions,
            MarketPriceProvider prices,
            MarketPricePolicy pricePolicy
    ) {
        this.portfolios = portfolios;
        this.positions = positions;
        this.prices = prices;
        this.pricePolicy = pricePolicy;
    }

    @Transactional(readOnly = true)
    public PositionPageResponse execute(
            UUID userId,
            UUID portfolioId,
            int page,
            int size
    ) {
        validatePagination(page, size);

        portfolios.findOwnedById(
                        portfolioId,
                        userId
                )
                .orElseThrow(() ->
                        new PortfolioNotFoundException("not found")
                );

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("assetId")
                )
        );

        var result = positions.findAllByPortfolioId(
                portfolioId,
                pageable
        );

        Map<UUID, MarketPrice> marketPrices =
                loadMarketPrices(result.getContent());

        var responses = result.getContent()
                .stream()
                .map(position ->
                        toResponse(
                                position,
                                marketPrices
                        )
                )
                .toList();

        return new PositionPageResponse(
                responses,
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        );
    }

    private Map<UUID, MarketPrice> loadMarketPrices(
            java.util.List<Position> positions
    ) {
        if (positions.isEmpty()) {
            return Map.of();
        }

        Set<UUID> assetIds = positions.stream()
                .map(Position::assetId)
                .collect(Collectors.toSet());

        return prices.findLatestPrices(assetIds);
    }

    private PositionResponse toResponse(
            Position position,
            Map<UUID, MarketPrice> marketPrices
    ) {
        MarketPrice marketPrice =
                marketPrices.get(position.assetId());

        if (marketPrice == null) {
            return unavailableResponse(
                    position,
                    null
            );
        }

        if (!pricePolicy.isUsable(marketPrice)) {
            return unavailableResponse(
                    position,
                    marketPrice
            );
        }

        if (!position.currency()
                .equalsIgnoreCase(
                        marketPrice.currency()
                )) {

            return unavailableResponse(
                    position,
                    marketPrice
            );
        }

        var valuation =
                PositionValuation.calculate(
                        position,
                        marketPrice.price()
                );

        return new PositionResponse(
                position.assetId(),
                position.quantity(),
                position.averageCost(),
                position.currency(),
                marketPrice.price(),
                valuation.marketValue(),
                valuation.unrealizedPnl(),
                valuation.unrealizedPnlPercentage(),
                marketPrice.observedAt(),
                true
        );
    }

    private PositionResponse unavailableResponse(
            Position position,
            MarketPrice marketPrice
    ) {
        return new PositionResponse(
                position.assetId(),
                position.quantity(),
                position.averageCost(),
                position.currency(),
                null,
                null,
                null,
                null,
                marketPrice == null
                        ? null
                        : marketPrice.observedAt(),
                false
        );
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters"
            );
        }
    }
}