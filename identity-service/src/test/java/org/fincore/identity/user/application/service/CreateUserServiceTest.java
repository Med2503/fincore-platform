package org.fincore.identity.user.application.service;


import org.fincore.identity.user.application.command.CreateUserCommand;
import org.fincore.identity.user.application.exception.DuplicateUsernameException;
import org.fincore.identity.user.application.port.PasswordHasher;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.model.UserStatus;
import org.fincore.identity.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private CreateUserService service;

    @BeforeEach
    void setUp() {
        service = new CreateUserService(
                userRepository,
                passwordHasher
        );
    }

    @Test
    void shouldCreateUserSuccessfully() {

        // Given
        String username = "john";
        String rawPassword = "Password123!";
        String passwordHash = "$2a$12$hashed-password";

        CreateUserCommand command =
                new CreateUserCommand(
                        username,
                        rawPassword
                );

        when(userRepository.existsByUsername(username))
                .thenReturn(false);

        when(passwordHasher.hash(rawPassword))
                .thenReturn(passwordHash);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = service.create(command);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());

        assertEquals(username, result.getUsername());

        assertEquals(
                passwordHash,
                result.getPasswordHash()
        );

        assertEquals(
                UserStatus.PENDING,
                result.getStatus()
        );

        verify(userRepository)
                .existsByUsername(username);

        verify(passwordHasher)
                .hash(rawPassword);

        verify(userRepository)
                .save(any(User.class));
    }

    @Test
    void shouldGenerateUuidForNewUser() {

        // Given
        CreateUserCommand command =
                new CreateUserCommand(
                        "john",
                        "Password123!"
                );

        when(userRepository.existsByUsername("john"))
                .thenReturn(false);

        when(passwordHasher.hash("Password123!"))
                .thenReturn("$2a$12$hashed-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = service.create(command);

        // Then
        assertNotNull(result.getId());
        assertInstanceOf(UUID.class, result.getId());
    }

    @Test
    void shouldHashRawPasswordBeforeSaving() {

        // Given
        String rawPassword = "Password123!";
        String passwordHash = "$2a$12$hashed-password";

        CreateUserCommand command =
                new CreateUserCommand(
                        "john",
                        rawPassword
                );

        when(userRepository.existsByUsername("john"))
                .thenReturn(false);

        when(passwordHasher.hash(rawPassword))
                .thenReturn(passwordHash);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = service.create(command);

        // Then
        assertEquals(
                passwordHash,
                result.getPasswordHash()
        );

        assertNotEquals(
                rawPassword,
                result.getPasswordHash()
        );

        verify(passwordHasher)
                .hash(rawPassword);
    }

    @Test
    void shouldNotCreateUserWhenUsernameAlreadyExists() {

        // Given
        String username = "john";

        CreateUserCommand command =
                new CreateUserCommand(
                        username,
                        "Password123!"
                );

        when(userRepository.existsByUsername(username))
                .thenReturn(true);

        // When / Then
        assertThrows(
                DuplicateUsernameException.class,
                () -> service.create(command)
        );

        verify(userRepository)
                .existsByUsername(username);

        verify(passwordHasher, never())
                .hash(anyString());

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldNotSaveRawPassword() {

        // Given
        String rawPassword = "Password123!";
        String passwordHash = "$2a$12$hashed-password";

        CreateUserCommand command =
                new CreateUserCommand(
                        "john",
                        rawPassword
                );

        when(userRepository.existsByUsername("john"))
                .thenReturn(false);

        when(passwordHasher.hash(rawPassword))
                .thenReturn(passwordHash);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        service.create(command);

        // Then
        verify(userRepository)
                .save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertNotEquals(
                rawPassword,
                savedUser.getPasswordHash()
        );

        assertEquals(
                passwordHash,
                savedUser.getPasswordHash()
        );
    }

    @Test
    void shouldCreateUserWithPendingStatus() {

        // Given
        CreateUserCommand command =
                new CreateUserCommand(
                        "john",
                        "Password123!"
                );

        when(userRepository.existsByUsername("john"))
                .thenReturn(false);

        when(passwordHasher.hash("Password123!"))
                .thenReturn("$2a$12$hashed-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = service.create(command);

        // Then
        assertEquals(
                UserStatus.PENDING,
                result.getStatus()
        );
    }
}
