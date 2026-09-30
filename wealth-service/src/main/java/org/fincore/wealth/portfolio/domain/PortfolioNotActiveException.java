package org.fincore.wealth.portfolio.domain;

import java.util.UUID;

public class PortfolioNotActiveException extends RuntimeException {

    public PortfolioNotActiveException(UUID portfolioId) {
        super("Portfolio is not active: " + portfolioId);
    }
}
