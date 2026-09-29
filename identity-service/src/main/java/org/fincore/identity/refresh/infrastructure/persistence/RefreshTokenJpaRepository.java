package org.fincore.identity.refresh.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository
        extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                select token
                from RefreshTokenJpaEntity token
                where token.tokenHash = :tokenHash
            """)
    Optional<RefreshTokenJpaEntity> findByHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                update RefreshTokenJpaEntity token
                set token.revokedAt = :now
                where token.familyId = :familyId
                  and token.revokedAt is null
            """)
    int revokeActiveFamily(
            @Param("familyId") UUID familyId,
            @Param("now") java.time.Instant now
    );

    @Modifying
    @Query("""
        update RefreshTokenJpaEntity token
        set token.revokedAt = :now
        where token.userId = :userId
          and token.revokedAt is null
    """)
    int revokeAllActiveForUser(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );
}
