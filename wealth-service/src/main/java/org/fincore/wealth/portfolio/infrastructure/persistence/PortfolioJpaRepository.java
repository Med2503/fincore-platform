package org.fincore.wealth.portfolio.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PortfolioJpaRepository
        extends JpaRepository<PortfolioJpaEntity, UUID> {

    Optional<PortfolioJpaEntity> findByIdAndUserId(
            UUID portfolioId,
            UUID userId
    );

    Page<PortfolioJpaEntity> findAllByUserId(
            UUID userId,
            Pageable pageable
    );
}