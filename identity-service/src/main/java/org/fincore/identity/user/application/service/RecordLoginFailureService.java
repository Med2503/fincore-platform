package org.fincore.identity.user.application.service;

import org.fincore.identity.user.application.exception.UserNotFoundException;
import org.fincore.identity.user.application.security.AccountLockPolicy;
import org.fincore.identity.user.application.usecase.RecordLoginFailureUseCase;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public class RecordLoginFailureService implements RecordLoginFailureUseCase {

    private final UserRepository userRepository;
    private final AccountLockPolicy lockPolicy;

    public RecordLoginFailureService(UserRepository userRepository, AccountLockPolicy lockPolicy) {
        this.userRepository = userRepository;
        this.lockPolicy = lockPolicy;
    }


    @Override
    @Transactional
    public void record(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(userId)
                );
        user.recordFailedLogin();

        if (lockPolicy.shouldLock(user.getFailedLoginAttempts())) {

            Instant lockUntil = Instant.now().plus(lockPolicy.lockDuration());
            user.lockUntil(lockUntil);


        }
        userRepository.save(user);

    }
}
