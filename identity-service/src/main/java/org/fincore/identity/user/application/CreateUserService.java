package org.fincore.identity.user.application;

import org.fincore.identity.user.application.command.CreateUserCommand;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.model.UserStatus;
import org.fincore.identity.user.domain.repository.UserRepository;

import java.time.Instant;
import java.util.UUID;

public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;

    public CreateUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public User create(CreateUserCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new IllegalArgumentException("Username already exists");
        }


        Instant now = Instant.now();

        User user = new User(
                UUID.randomUUID(),
                command.username(),
                command.password(),
                UserStatus.PENDING,
                now
        );

        return userRepository.save(user);
    }
}
