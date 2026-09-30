package org.fincore.wealth.portfolio.application.service;

import org.fincore.wealth.portfolio.application.exception.PortfolioNotFoundException;
import org.fincore.wealth.portfolio.domain.Portfolio;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetPortfolioService {

    private final PortfolioRepository repository;

    public GetPortfolioService(PortfolioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Portfolio getOwnedPortfolio(UUID portfolioId, UUID userId) {
        return repository.findOwnedById(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found"));
    }
}