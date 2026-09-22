package org.fincore.identity.user.application.service;

import org.fincore.identity.user.application.command.CreateUserCommand;
import org.fincore.identity.user.application.exception.DuplicateUsernameException;
import org.fincore.identity.user.application.port.PasswordHasher;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.model.UserStatus;
import org.fincore.identity.user.domain.repository.UserRepository;
import org.fincore.identity.user.application.usecase.CreateUserUseCase;

import java.time.Instant;
import java.util.UUID;

public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final UsernameNormalizer normalizer;

    public CreateUserService(UserRepository userRepository, PasswordHasher passwordHasher, UsernameNormalizer normalizer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.normalizer = normalizer;
    }


    @Override
    public User create(CreateUserCommand command) {

        String username = normalizer.normalize(command.username());
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException(username);
        }

        String passwordHash = passwordHasher.hash(command.password());

        Instant now = Instant.now();

        User user = new User(
                UUID.randomUUID(),
                username,
                passwordHash,
                UserStatus.PENDING,
                now
        );

        return userRepository.save(user);
    }
}
