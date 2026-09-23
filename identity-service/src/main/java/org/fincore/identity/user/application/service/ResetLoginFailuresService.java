package org.fincore.identity.user.application.service;


import org.fincore.identity.user.application.exception.UserNotFoundException;
import org.fincore.identity.user.application.usecase.ResetLoginFailuresUseCase;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class ResetLoginFailuresService
        implements ResetLoginFailuresUseCase {

    private final UserRepository userRepository;

    public ResetLoginFailuresService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void reset(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(userId)
                );

        user.resetFailedLoginAttempts();

        userRepository.save(user);
    }
}
