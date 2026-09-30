package org.fincore.wealth.profile.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.fincore.wealth.profile.domain.RiskProfile;

public record CreateInvestorProfileRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @Pattern(regexp = "^[A-Za-z]{2}$")
        String countryCode,

        RiskProfile riskProfile
) {
}