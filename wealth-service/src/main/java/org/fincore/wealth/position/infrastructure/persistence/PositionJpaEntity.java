package org.fincore.wealth.position.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "positions")
public class PositionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(
            nullable = false,
            precision = 28,
            scale = 10
    )
    private BigDecimal quantity;

    @Column(
            name = "average_cost",
            nullable = false,
            precision = 28,
            scale = 10
    )
    private BigDecimal averageCost;

    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Column(
            name = "last_execution_sequence",
            nullable = false
    )
    private long lastExecutionSequence;

    public PositionJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getPortfolioId() {
        return portfolioId;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getAverageCost() {
        return averageCost;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getLastExecutionSequence() {
        return lastExecutionSequence;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setPortfolioId(UUID portfolioId) {
        this.portfolioId = portfolioId;
    }

    public void setAssetId(UUID assetId) {
        this.assetId = assetId;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setAverageCost(BigDecimal averageCost) {
        this.averageCost = averageCost;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setLastExecutionSequence(
            long lastExecutionSequence
    ) {
        this.lastExecutionSequence = lastExecutionSequence;
    }
}
