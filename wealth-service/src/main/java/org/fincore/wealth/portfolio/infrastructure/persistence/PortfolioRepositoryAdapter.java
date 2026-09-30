package org.fincore.wealth.portfolio.infrastructure.persistence;

import org.fincore.wealth.portfolio.domain.Portfolio;
import org.fincore.wealth.portfolio.domain.PortfolioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PortfolioRepositoryAdapter implements PortfolioRepository {

    private final PortfolioJpaRepository jpaRepository;
    private final PortfolioMapper mapper;

    public PortfolioRepositoryAdapter(
            PortfolioJpaRepository jpaRepository,
            PortfolioMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Portfolio save(Portfolio portfolio) {
        PortfolioJpaEntity entity = mapper.toEntity(portfolio);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Portfolio> findOwnedById(UUID portfolioId, UUID userId) {
        return jpaRepository.findByIdAndUserId(portfolioId, userId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Portfolio> findAllByUserId(
            UUID userId,
            Pageable pageable
    ) {
        return jpaRepository.findAllByUserId(userId, pageable)
                .map(mapper::toDomain);
    }
}