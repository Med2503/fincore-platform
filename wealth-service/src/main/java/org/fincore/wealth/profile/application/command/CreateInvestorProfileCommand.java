package org.fincore.wealth.profile.application.command;

import org.fincore.wealth.profile.domain.RiskProfile;

import java.util.UUID;

public record CreateInvestorProfileCommand(
        UUID userId,
        String firstName,
        String lastName,
        String countryCode,
        RiskProfile riskProfile
) {
}