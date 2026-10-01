package org.fincore.wealth.position.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.fincore.wealth.position.infrastructure.persistence.PositionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataPositionRepository
        extends JpaRepository<PositionJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from PositionJpaEntity p
            where p.portfolioId = :portfolioId
              and p.assetId = :assetId
            """)
    Optional<PositionJpaEntity> findForUpdate(
            @Param("portfolioId") UUID portfolioId,
            @Param("assetId") UUID assetId
    );

    Optional<PositionJpaEntity> findByPortfolioIdAndAssetId(
            UUID portfolioId,
            UUID assetId
    );

    Page<PositionJpaEntity> findAllByPortfolioId(
            UUID portfolioId,
            Pageable pageable
    );
}