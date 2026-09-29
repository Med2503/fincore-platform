package org.fincore.identity.refresh.infrastructure.persistence;


import org.fincore.identity.refresh.domain.RefreshToken;
import org.fincore.identity.refresh.domain.RefreshTokenRepository;
import org.springframework.stereotype.Repository;


import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository springDataRepository;
    private final RefreshTokenMapper mapper;

    public RefreshTokenRepositoryAdapter(
            RefreshTokenJpaRepository springDataRepository,
            RefreshTokenMapper mapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
        return springDataRepository.findByHashForUpdate(tokenHash)
                .map(mapper::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = mapper.toEntity(refreshToken);

        RefreshTokenJpaEntity saved =
                springDataRepository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public int revokeActiveFamily(UUID familyId, Instant revokedAt) {
        return springDataRepository.revokeActiveFamily(familyId, revokedAt);
    }

    @Override
    public int revokeAllActiveForUser(UUID userId, Instant revokedAt) {
        return springDataRepository.revokeAllActiveForUser(userId, revokedAt);
    }
}
