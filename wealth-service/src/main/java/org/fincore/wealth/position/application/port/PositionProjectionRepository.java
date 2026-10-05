package org.fincore.wealth.position.application.port;

import org.fincore.wealth.position.application.projection.PositionProjection;

import java.util.Optional;
import java.util.UUID;

public interface PositionProjectionRepository {

    void save(PositionProjection projection);

    void delete(UUID portfolioId, UUID assetId);

    Optional<PositionProjection> find(
            UUID portfolioId,
            UUID assetId
    );
}