package org.fincore.wealth.portfolio.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class Portfolio {

    private final UUID id;
    private final UUID userId;
    private String name;
    private String baseCurrency;
    private PortfolioStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Portfolio(
            UUID id,
            UUID userId,
            String name,
            String baseCurrency,
            PortfolioStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.name = normalizeName(name);
        this.baseCurrency = normalizeCurrency(baseCurrency);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Portfolio create(
            UUID id,
            UUID userId,
            String name,
            String currency,
            Instant now
    ) {
        return new Portfolio(
                id,
                userId,
                name,
                currency,
                PortfolioStatus.ACTIVE,
                now,
                now
        );
    }

    public void rename(String newName, Instant now) {
        ensureActive();
        this.name = normalizeName(newName);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void archive(Instant now) {
        ensureActive();
        this.status = PortfolioStatus.ARCHIVED;
        this.updatedAt = Objects.requireNonNull(now);
    }

    private void ensureActive() {
        if (status != PortfolioStatus.ACTIVE) {
            throw new PortfolioNotActiveException(id);
        }
    }

    private static String normalizeName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Portfolio name is required");
        }

        String normalized = value.trim();

        if (normalized.length() < 3 || normalized.length() > 120) {
            throw new IllegalArgumentException(
                    "Portfolio name must contain 3 to 120 characters"
            );
        }

        return normalized;
    }

    private static String normalizeCurrency(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Currency is required");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must be a three-letter code"
            );
        }

        return normalized;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getBaseCurrency() { return baseCurrency; }
    public PortfolioStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}