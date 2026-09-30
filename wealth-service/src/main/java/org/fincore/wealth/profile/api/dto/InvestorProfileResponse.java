package org.fincore.wealth.profile.api.dto;

import org.fincore.wealth.profile.domain.RiskProfile;

import java.time.Instant;
import java.util.UUID;

public record InvestorProfileResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String countryCode,
        RiskProfile riskProfile,
        Instant createdAt,
        Instant updatedAt
) {
}