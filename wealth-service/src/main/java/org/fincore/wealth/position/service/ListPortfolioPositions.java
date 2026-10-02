package org.fincore.wealth.position.service;

import org.fincore.wealth.portfolio.application.exception.PortfolioNotFoundException;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.fincore.wealth.position.api.dto.PositionPageResponse;
import org.fincore.wealth.position.api.dto.PositionResponse;

import org.fincore.wealth.position.application.port.MarketPriceProvider;
import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.domain.PositionValuation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ListPortfolioPositions {
    private final PortfolioRepository portfolios;
    private final PositionRepository positions;
    private final MarketPriceProvider prices;

    public ListPortfolioPositions(
            PortfolioRepository portfolios,
            PositionRepository positions,
            MarketPriceProvider prices
    ) {
        this.portfolios = portfolios;
        this.positions = positions;
        this.prices = prices;
    }

    @Transactional(readOnly = true)
    public PositionPageResponse execute(
            UUID userId,
            UUID portfolioId,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        portfolios.findOwnedById(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException("not found"));

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "assetId")
        );

        var results = positions.findAllByPortfolioId(portfolioId, pageable);

        var responses = results.stream()
                .map(this::toResponse)
                .toList();

        return new PositionPageResponse(
                responses,
                page,
                size,
                results.size() == size
        );
    }

    private PositionResponse toResponse(Position position) {
        var quote = prices.findLatestPrice(position.assetId());

        if (quote.isEmpty()) {
            return new PositionResponse(
                    position.assetId(),
                    position.quantity(),
                    position.averageCost(),
                    position.currency(),
                    null, null, null, null, null,
                    false
            );
        }

        var marketPrice = quote.get();

        if (!position.currency().equalsIgnoreCase(marketPrice.currency())) {
            return new PositionResponse(
                    position.assetId(),
                    position.quantity(),
                    position.averageCost(),
                    position.currency(),
                    null, null, null, null,
                    marketPrice.observedAt(),
                    false
            );
        }

        var valuation = PositionValuation.calculate(
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
}