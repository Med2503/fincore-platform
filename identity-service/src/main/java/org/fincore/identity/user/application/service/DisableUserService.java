package org.fincore.identity.user.application.service;

import org.fincore.identity.refresh.domain.RefreshTokenRepository;
import org.fincore.identity.user.application.exception.UserNotFoundException;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class DisableUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public DisableUserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Transactional
    public void disable(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.disable();
        userRepository.save(user);

        refreshTokenRepository.revokeAllActiveForUser(
                userId,
                clock.instant()
        );
    }
}
