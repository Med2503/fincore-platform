package org.fincore.identity.refresh.application;

import org.fincore.identity.refresh.domain.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class RevokeAllSessionsService implements RevokeAllSessionsUseCase {

    private final RefreshTokenRepository repository;
    private final Clock clock;

    public RevokeAllSessionsService(
            RefreshTokenRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int revokeAll(UUID userId) {
        return repository.revokeAllActiveForUser(userId, clock.instant());
    }
}
