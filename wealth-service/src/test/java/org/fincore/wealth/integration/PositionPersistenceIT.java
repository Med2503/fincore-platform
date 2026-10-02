package org.fincore.wealth.integration;


import org.fincore.wealth.position.infrastructure.persistence.PositionJpaEntity;
import org.fincore.wealth.position.infrastructure.persistence.SpringDataPositionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PositionPersistenceIT extends PostgreSQLIntegrationTestBase {

    @Autowired
    SpringDataPositionRepository positions;

    @Test
    void persistsPositionWithDecimalPrecision() {
        UUID portfolioId = insertPortfolio();
        UUID assetId = UUID.randomUUID();

        PositionJpaEntity entity = newPosition(
                portfolioId,
                assetId,
                new BigDecimal("12.1234567890"),
                new BigDecimal("101.9876543210")
        );

        PositionJpaEntity saved = positions.saveAndFlush(entity);

        PositionJpaEntity reloaded = positions.findById(saved.getId())
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("12.1234567890")
                        .compareTo(reloaded.getQuantity())
        );
        assertEquals(
                0,
                new BigDecimal("101.9876543210")
                        .compareTo(reloaded.getAverageCost())
        );
    }

    @Test
    void databaseRejectsDuplicatePortfolioAssetPosition() {
        UUID portfolioId = insertPortfolio();
        UUID assetId = UUID.randomUUID();

        positions.saveAndFlush(newPosition(
                portfolioId, assetId,
                new BigDecimal("1"), new BigDecimal("100")
        ));

        assertThrows(DataIntegrityViolationException.class, () ->
                positions.saveAndFlush(newPosition(
                        portfolioId, assetId,
                        new BigDecimal("2"), new BigDecimal("110")
                ))
        );
    }

    private PositionJpaEntity newPosition(
            UUID portfolioId,
            UUID assetId,
            BigDecimal quantity,
            BigDecimal averageCost
    ) {
        PositionJpaEntity entity = new PositionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setPortfolioId(portfolioId);
        entity.setAssetId(assetId);
        entity.setQuantity(quantity);
        entity.setAverageCost(averageCost);
        entity.setCurrency("USD");
        entity.setUpdatedAt(Instant.parse("2026-10-02T00:00:00Z"));
        return entity;
    }

    private UUID insertPortfolio() {
        UUID portfolioId = UUID.randomUUID();

        jdbcTemplate.update("""
                insert into portfolios
                    (id, user_id, name, base_currency, status,
                     created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?)
                """,
                portfolioId,
                UUID.randomUUID(),
                "Integration test portfolio",
                "USD",
                "ACTIVE",
                Instant.parse("2026-10-02T00:00:00Z"),
                Instant.parse("2026-10-02T00:00:00Z")
        );

        return portfolioId;
    }

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
}