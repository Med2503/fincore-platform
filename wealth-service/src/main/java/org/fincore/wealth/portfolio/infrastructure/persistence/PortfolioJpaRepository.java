package org.fincore.wealth.portfolio.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select p
    from PortfolioJpaEntity p
    where p.id = :portfolioId
    """)
    Optional<PortfolioJpaEntity> findByIdForUpdate(
            @Param("portfolioId") UUID portfolioId
    );
}