package org.fincore.identity.refresh.domain;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final UUID familyId;
    private final Instant issuedAt;
    private final Instant expiresAt;

    private Instant revokedAt;
    private UUID replacedByTokenId;

    public RefreshToken(
            UUID id,
            UUID userId,
            String tokenHash,
            UUID familyId,
            Instant issuedAt,
            Instant expiresAt,
            Instant revokedAt,
            UUID replacedByTokenId
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable(Instant now) {
        return !isRevoked() && !isExpired(now);
    }

    public void revoke(Instant now, UUID replacementId) {
        if (isRevoked()) {
            return;
        }

        this.revokedAt = now;
        this.replacedByTokenId = replacementId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getReplacedByTokenId() {
        return replacedByTokenId;
    }
}
