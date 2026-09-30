package org.fincore.wealth.position.application.port;

import org.fincore.wealth.position.domain.Position;

import java.util.Optional;
import java.util.UUID;

public interface PositionRepository {
    Optional<Position> findForUpdate(UUID portfolioId, UUID assetId);

    Position save(Position position);

    void delete(Position position);
}
