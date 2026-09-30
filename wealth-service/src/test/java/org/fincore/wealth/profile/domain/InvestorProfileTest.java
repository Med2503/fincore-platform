package org.fincore.wealth.profile.domain;

import org.fincore.wealth.profile.domain.InvestorProfile;
import org.fincore.wealth.profile.domain.RiskProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvestorProfileTest {

    @Test
    void shouldNormalizeCountryCode() {
        InvestorProfile profile = new InvestorProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Ada",
                "Lovelace",
                "tn",
                RiskProfile.MODERATE,
                Instant.now(),
                Instant.now()
        );

        assertEquals("TN", profile.getCountryCode());
    }

    @Test
    void shouldRejectInvalidCountryCode() {
        assertThrows(IllegalArgumentException.class, () ->
                new InvestorProfile(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Ada",
                        "Lovelace",
                        "TUN",
                        RiskProfile.MODERATE,
                        Instant.now(),
                        Instant.now()
                )
        );
    }
}