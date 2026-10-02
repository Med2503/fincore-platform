package org.fincore.wealth.portfolio.infrastructure.persistence;

import org.fincore.wealth.portfolio.application.port.PortfolioPositionLock;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public class PortfolioPositionLockAdapter implements PortfolioPositionLock {

    private final PortfolioJpaRepository repository;

    public PortfolioPositionLockAdapter(
            PortfolioJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void lockForPositionUpdate(UUID portfolioId) {
        repository.findByIdForUpdate(portfolioId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Portfolio not found: " + portfolioId
                        )
                );
    }
}