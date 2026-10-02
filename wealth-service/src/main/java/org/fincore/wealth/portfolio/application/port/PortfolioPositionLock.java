package org.fincore.wealth.portfolio.application.port;

import java.util.UUID;

public interface PortfolioPositionLock {

    void lockForPositionUpdate(UUID portfolioId);
}