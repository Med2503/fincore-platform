package org.fincore.wealth.position.application.port;

import org.fincore.wealth.position.domain.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository {
    Optional<Position> findForUpdate(UUID portfolioId, UUID assetId);

    Position save(Position position);

    void delete(Position position);


    Page<Position> findAllByPortfolioId(
            UUID portfolioId,
            Pageable pageable
    );

    List<Position> findAllByPortfolioId(UUID portfolioId);
}
