package org.fincore.wealth.portfolio.application.command;

import java.util.UUID;

public record CreatePortfolioCommand(
        UUID userId,
        String name,
        String baseCurrency
) {
}