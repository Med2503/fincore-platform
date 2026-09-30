package org.fincore.wealth.profile.domain;

import java.util.Optional;
import java.util.UUID;

public interface InvestorProfileRepository {

    InvestorProfile save(InvestorProfile profile);

    Optional<InvestorProfile> findByUserId(UUID userId);
}