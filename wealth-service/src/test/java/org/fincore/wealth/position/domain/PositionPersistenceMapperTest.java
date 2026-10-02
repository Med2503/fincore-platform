package org.fincore.wealth.position.domain;

import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.infrastructure.persistence.PositionJpaEntity;
import org.fincore.wealth.position.infrastructure.persistence.PositionPersistenceMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PositionPersistenceMapperTest {

    private final PositionPersistenceMapper mapper =
            new PositionPersistenceMapper();

    @Test
    void shouldMapDomainToEntity() {
        UUID id = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Instant updatedAt = Instant.parse(
                "2026-10-02T10:00:00Z"
        );

        Position position = new Position(
                id,
                portfolioId,
                assetId,
                new BigDecimal("150.5000000000"),
                new BigDecimal("102.3500000000"),
                "USD",
                updatedAt,
                105L
        );

        PositionJpaEntity entity =
                mapper.toEntity(position);

        assertThat(entity.getId())
                .isEqualTo(id);

        assertThat(entity.getPortfolioId())
                .isEqualTo(portfolioId);

        assertThat(entity.getAssetId())
                .isEqualTo(assetId);

        assertThat(entity.getQuantity())
                .isEqualByComparingTo(
                        "150.5000000000"
                );

        assertThat(entity.getAverageCost())
                .isEqualByComparingTo(
                        "102.3500000000"
                );

        assertThat(entity.getCurrency())
                .isEqualTo("USD");

        assertThat(entity.getUpdatedAt())
                .isEqualTo(updatedAt);

        assertThat(entity.getLastExecutionSequence())
                .isEqualTo(105L);
    }

    @Test
    void shouldMapEntityToDomain() {
        UUID id = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Instant updatedAt = Instant.parse(
                "2026-10-02T10:00:00Z"
        );

        PositionJpaEntity entity =
                new PositionJpaEntity();

        entity.setId(id);
        entity.setPortfolioId(portfolioId);
        entity.setAssetId(assetId);
        entity.setQuantity(
                new BigDecimal("150.5000000000")
        );
        entity.setAverageCost(
                new BigDecimal("102.3500000000")
        );
        entity.setCurrency("USD");
        entity.setUpdatedAt(updatedAt);
        entity.setLastExecutionSequence(105L);

        Position position =
                mapper.toDomain(entity);

        assertThat(position.id())
                .isEqualTo(id);

        assertThat(position.portfolioId())
                .isEqualTo(portfolioId);

        assertThat(position.assetId())
                .isEqualTo(assetId);

        assertThat(position.quantity())
                .isEqualByComparingTo(
                        "150.5000000000"
                );

        assertThat(position.averageCost())
                .isEqualByComparingTo(
                        "102.3500000000"
                );

        assertThat(position.currency())
                .isEqualTo("USD");

        assertThat(position.updatedAt())
                .isEqualTo(updatedAt);

        assertThat(position.lastExecutionSequence())
                .isEqualTo(105L);
    }

    @Test
    void shouldPreserveLastExecutionSequenceDuringRoundTrip() {
        Position original = new Position(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("200"),
                new BigDecimal("98.75"),
                "EUR",
                Instant.parse(
                        "2026-10-02T12:00:00Z"
                ),
                250L
        );

        PositionJpaEntity entity =
                mapper.toEntity(original);

        Position restored =
                mapper.toDomain(entity);

        assertThat(restored)
                .isEqualTo(original);
    }
}
