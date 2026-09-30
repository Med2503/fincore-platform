package org.fincore.wealth.profile.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class InvestorProfile {

    private final UUID id;
    private final UUID userId;

    private String firstName;
    private String lastName;
    private String countryCode;
    private RiskProfile riskProfile;

    private final Instant createdAt;
    private Instant updatedAt;

    public InvestorProfile(
            UUID id,
            UUID userId,
            String firstName,
            String lastName,
            String countryCode,
            RiskProfile riskProfile,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.countryCode = normalizeCountryCode(countryCode);
        this.riskProfile = Objects.requireNonNull(riskProfile);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void update(
            String firstName,
            String lastName,
            String countryCode,
            RiskProfile riskProfile,
            Instant now
    ) {
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.countryCode = normalizeCountryCode(countryCode);
        this.riskProfile = Objects.requireNonNull(riskProfile);
        this.updatedAt = Objects.requireNonNull(now);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeCountryCode(String value) {
        String normalized = requireText(value, "countryCode").toUpperCase();

        if (!normalized.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("countryCode must be ISO alpha-2");
        }

        return normalized;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getCountryCode() { return countryCode; }
    public RiskProfile getRiskProfile() { return riskProfile; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}