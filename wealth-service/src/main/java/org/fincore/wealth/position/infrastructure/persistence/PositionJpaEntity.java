package org.fincore.wealth.position.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "positions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_positions_portfolio_asset",
                columnNames = {"portfolio_id", "asset_id"}
        )
)
public class PositionJpaEntity {
    @Id
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(nullable = false, precision = 28, scale = 10)
    private BigDecimal quantity;

    @Column(name = "average_cost", nullable = false, precision = 28, scale = 10)
    private BigDecimal averageCost;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PositionJpaEntity() {
    }

    public UUID getId() { return id; }
    public UUID getPortfolioId() { return portfolioId; }
    public UUID getAssetId() { return assetId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getAverageCost() { return averageCost; }
    public String getCurrency() { return currency; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setPortfolioId(UUID portfolioId) { this.portfolioId = portfolioId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setAverageCost(BigDecimal averageCost) { this.averageCost = averageCost; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}