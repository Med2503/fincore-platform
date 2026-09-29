package org.fincore.identity.refresh.infrastructure.persistence;

import org.fincore.identity.refresh.application.RefreshTokenCrypto;
import org.fincore.identity.refresh.domain.RefreshToken;
import org.fincore.identity.refresh.domain.RefreshTokenRepository;
import org.fincore.identity.security.refresh.RefreshTokenProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenRotationService {

    private final RefreshTokenRepository repository;
    private final RefreshTokenCrypto crypto;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    public RefreshTokenRotationService(
            RefreshTokenRepository repository,
            RefreshTokenCrypto crypto,
            RefreshTokenProperties properties,
            Clock clock
    ) {
        this.repository = repository;
        this.crypto = crypto;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        String hash = crypto.hash(rawToken);
        Instant now = clock.instant();

        Optional<RefreshToken> found =
                repository.findByHashForUpdate(hash);

        if (found.isEmpty()) {
            return RotationResult.invalid();
        }

        RefreshToken current = found.get();

        if (current.isRevoked()) {
            repository.revokeActiveFamily(current.getFamilyId(), now);
            return RotationResult.reuseDetected();
        }

        if (current.isExpired(now)) {
            return RotationResult.invalid();
        }

        String replacementRaw = crypto.generateRawToken();
        String replacementHash = crypto.hash(replacementRaw);
        UUID replacementId = UUID.randomUUID();

        RefreshToken replacement = new RefreshToken(
                replacementId,
                current.getUserId(),
                replacementHash,
                current.getFamilyId(),
                now,
                now.plus(properties.expiration()),
                null,
                null
        );

        current.revoke(now, replacementId);

        repository.save(current);
        repository.save(replacement);

        return new RotationResult(
                RotationStatus.SUCCESS,
                current.getUserId(),
                replacementRaw
        );
    }
}