package org.fincore.wealth.position.infrastructure.persistence;

import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.infrastructure.persistence.PositionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PositionPersistenceMapper {

    public Position toDomain(PositionJpaEntity entity) {
        return new Position(
                entity.getId(),
                entity.getPortfolioId(),
                entity.getAssetId(),
                entity.getQuantity(),
                entity.getAverageCost(),
                entity.getCurrency(),
                entity.getUpdatedAt(),
                entity.getLastExecutionSequence()
        );
    }

    public PositionJpaEntity toEntity(Position position) {
        PositionJpaEntity entity =
                new PositionJpaEntity();

        entity.setId(position.id());
        entity.setPortfolioId(position.portfolioId());
        entity.setAssetId(position.assetId());
        entity.setQuantity(position.quantity());
        entity.setAverageCost(position.averageCost());
        entity.setCurrency(position.currency());
        entity.setUpdatedAt(position.updatedAt());
        entity.setLastExecutionSequence(
                position.lastExecutionSequence()
        );

        return entity;
    }
}
