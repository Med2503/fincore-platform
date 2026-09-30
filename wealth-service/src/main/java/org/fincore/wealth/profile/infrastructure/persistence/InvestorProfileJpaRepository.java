package org.fincore.wealth.profile.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvestorProfileJpaRepository
        extends JpaRepository<InvestorProfileJpaEntity, UUID> {

    Optional<InvestorProfileJpaEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}