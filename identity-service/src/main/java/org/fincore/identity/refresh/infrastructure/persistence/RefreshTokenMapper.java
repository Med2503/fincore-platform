package org.fincore.identity.refresh.infrastructure.persistence;

import org.fincore.identity.refresh.domain.RefreshToken;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {

    public RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return new RefreshToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getFamilyId(),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getReplacedByTokenId()
        );
    }

    public RefreshTokenJpaEntity toEntity(RefreshToken token) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();

        entity.setId(token.getId());
        entity.setUserId(token.getUserId());
        entity.setTokenHash(token.getTokenHash());
        entity.setFamilyId(token.getFamilyId());
        entity.setIssuedAt(token.getIssuedAt());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setRevokedAt(token.getRevokedAt());
        entity.setReplacedByTokenId(token.getReplacedByTokenId());

        return entity;
    }
}