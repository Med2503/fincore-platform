package org.fincore.wealth.portfolio.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository {

    Portfolio save(Portfolio portfolio);

    Optional<Portfolio> findOwnedById(UUID portfolioId, UUID userId);

    Page<Portfolio> findAllByUserId(UUID userId, Pageable pageable);
}