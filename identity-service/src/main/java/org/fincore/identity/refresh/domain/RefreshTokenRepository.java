package org.fincore.identity.refresh.domain;


import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByHashForUpdate(String tokenHash);

    RefreshToken save(RefreshToken refreshToken);

    int revokeActiveFamily(UUID familyId, Instant revokedAt);


    int revokeAllActiveForUser(UUID userId, Instant revokedAt);
}
