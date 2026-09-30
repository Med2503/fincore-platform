package org.fincore.wealth.portfolio.application.service;

import org.fincore.wealth.portfolio.application.exception.PortfolioNotFoundException;
import org.fincore.wealth.portfolio.domain.Portfolio;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class ArchivePortfolioService {

    private final PortfolioRepository repository;
    private final Clock clock;

    public ArchivePortfolioService(
            PortfolioRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Portfolio archive(UUID portfolioId, UUID userId) {
        Portfolio portfolio = repository.findOwnedById(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found"));

        portfolio.archive(clock.instant());

        return repository.save(portfolio);
    }
}