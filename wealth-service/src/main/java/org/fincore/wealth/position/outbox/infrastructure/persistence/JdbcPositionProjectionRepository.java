package org.fincore.wealth.position.outbox.infrastructure.persistence;

import org.fincore.wealth.position.application.port.PositionProjectionRepository;
import org.fincore.wealth.position.application.projection.PositionProjection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcPositionProjectionRepository
        implements PositionProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPositionProjectionRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(PositionProjection projection) {
        jdbcTemplate.update(
                """
                INSERT INTO position_projections (
                    portfolio_id,
                    asset_id,
                    quantity,
                    average_cost,
                    currency,
                    execution_sequence,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (portfolio_id, asset_id)
                DO UPDATE SET
                    quantity = EXCLUDED.quantity,
                    average_cost = EXCLUDED.average_cost,
                    currency = EXCLUDED.currency,
                    execution_sequence = EXCLUDED.execution_sequence,
                    updated_at = EXCLUDED.updated_at
                WHERE position_projections.execution_sequence
                      < EXCLUDED.execution_sequence
                """,
                projection.portfolioId(),
                projection.assetId(),
                projection.quantity(),
                projection.averageCost(),
                projection.currency(),
                projection.executionSequence(),
                projection.updatedAt()
        );
    }

    @Override
    public void delete(
            UUID portfolioId,
            UUID assetId
    ) {
        jdbcTemplate.update(
                """
                DELETE FROM position_projections
                WHERE portfolio_id = ?
                  AND asset_id = ?
                """,
                portfolioId,
                assetId
        );
    }

    @Override
    public Optional<PositionProjection> find(
            UUID portfolioId,
            UUID assetId
    ) {
        return jdbcTemplate.query(
                """
                SELECT portfolio_id,
                       asset_id,
                       quantity,
                       average_cost,
                       currency,
                       execution_sequence,
                       updated_at
                FROM position_projections
                WHERE portfolio_id = ?
                  AND asset_id = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }

                    return Optional.of(
                            new PositionProjection(
                                    rs.getObject(
                                            "portfolio_id",
                                            UUID.class
                                    ),
                                    rs.getObject(
                                            "asset_id",
                                            UUID.class
                                    ),
                                    rs.getBigDecimal("quantity"),
                                    rs.getBigDecimal("average_cost"),
                                    rs.getString("currency"),
                                    rs.getLong(
                                            "execution_sequence"
                                    ),
                                    rs.getTimestamp(
                                            "updated_at"
                                    ).toInstant()
                            )
                    );
                },
                portfolioId,
                assetId
        );
    }
}
