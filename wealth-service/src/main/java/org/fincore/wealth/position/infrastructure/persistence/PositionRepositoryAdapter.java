package org.fincore.wealth.position.infrastructure.persistence;


import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.domain.Position;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PositionRepositoryAdapter implements PositionRepository {
    private final SpringDataPositionRepository repository;
    private final PositionPersistenceMapper mapper;

    public PositionRepositoryAdapter(
            SpringDataPositionRepository repository,
            PositionPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override

    public Optional<Position> findForUpdate(UUID portfolioId, UUID assetId) {
        return repository.findForUpdate(portfolioId, assetId)
                .map(mapper::toDomain);
    }

    @Override
    public Position save(Position position) {
        PositionJpaEntity saved = repository.save(mapper.toEntity(position));
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(Position position) {
        repository.findById(position.id()).ifPresent(repository::delete);
    }

    @Override
    public List<Position> findAllByPortfolioId(UUID portfolioId, Pageable pageable) {
        return repository.findAllByPortfolioId(portfolioId, pageable)
                .map(mapper::toDomain)
                .getContent();
    }
}