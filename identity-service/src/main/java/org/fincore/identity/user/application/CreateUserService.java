package org.fincore.identity.user.application;

import org.fincore.identity.user.application.command.CreateUserCommand;
import org.fincore.identity.user.application.exception.DuplicateUsernameException;
import org.fincore.identity.user.application.port.PasswordHasher;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.model.UserStatus;
import org.fincore.identity.user.domain.repository.UserRepository;

import java.time.Instant;
import java.util.UUID;

public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public CreateUserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }


    @Override
    public User create(CreateUserCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new DuplicateUsernameException(command.username());
        }

        String passwordHash = passwordHasher.hash(command.password());

        Instant now = Instant.now();

        User user = new User(
                UUID.randomUUID(),
                command.username(),
                passwordHash,
                UserStatus.PENDING,
                now
        );

        return userRepository.save(user);
    }
}
