package org.fincore.wealth.position.outbox.infrastructure.persistence;

import org.fincore.wealth.position.application.port.PositionProjectionRepository;
import org.fincore.wealth.position.application.projection.PositionProjection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
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
    public Optional<PositionProjection> find(
            UUID portfolioId,
            UUID assetId
    ) {
        return jdbcTemplate.query(
                """
                        SELECT
                            portfolio_id,
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
                this::map,
                portfolioId,
                assetId
        ).stream().findFirst();
    }

    private PositionProjection map(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {

        return new PositionProjection(
                resultSet.getObject("portfolio_id", UUID.class),
                resultSet.getObject("asset_id", UUID.class),
                resultSet.getBigDecimal("quantity"),
                resultSet.getBigDecimal("average_cost"),
                resultSet.getString("currency"),
                resultSet.getLong("execution_sequence"),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }
}