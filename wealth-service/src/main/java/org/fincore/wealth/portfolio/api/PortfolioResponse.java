package org.fincore.wealth.portfolio.api;

import org.fincore.wealth.portfolio.domain.PortfolioStatus;

import java.time.Instant;
import java.util.UUID;

public record PortfolioResponse(
        UUID id,
        UUID userId,
        String name,
        String baseCurrency,
        PortfolioStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}