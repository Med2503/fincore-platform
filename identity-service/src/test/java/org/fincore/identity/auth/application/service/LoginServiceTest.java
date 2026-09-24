package org.fincore.identity.auth.application.service;

import org.fincore.identity.auth.application.command.LoginCommand;
import org.fincore.identity.auth.application.exception.AccountDisabledException;
import org.fincore.identity.auth.application.exception.AccountLockedException;
import org.fincore.identity.auth.application.exception.AccountPendingException;
import org.fincore.identity.auth.application.exception.InvalidCredentialsException;
import org.fincore.identity.auth.application.port.TokenIssuer;
import org.fincore.identity.auth.application.result.LoginResult;
import org.fincore.identity.user.application.port.PasswordHasher;
import org.fincore.identity.user.application.service.UsernameNormalizer;
import org.fincore.identity.user.application.usecase.RecordLoginFailureUseCase;
import org.fincore.identity.user.application.usecase.ResetLoginFailuresUseCase;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.model.UserStatus;
import org.fincore.identity.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginServiceTest {

    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private RecordLoginFailureUseCase recordLoginFailureUseCase;
    private ResetLoginFailuresUseCase resetLoginFailuresUseCase;
    private TokenIssuer tokenIssuer;
    private LoginService service;
    private UsernameNormalizer normalizer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        recordLoginFailureUseCase =
                mock(RecordLoginFailureUseCase.class);
        resetLoginFailuresUseCase =
                mock(ResetLoginFailuresUseCase.class);
        tokenIssuer = mock(TokenIssuer.class);
        normalizer = mock(UsernameNormalizer.class);
        service = new LoginService(
                userRepository,
                passwordHasher,
                recordLoginFailureUseCase,
                resetLoginFailuresUseCase,
                tokenIssuer,
                normalizer);
    }

    @Test
    void shouldLoginSuccessfully() {
        User user = activeUser();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordHasher.matches(
                "password",
                "hashed-password"
        )).thenReturn(true);

        when(tokenIssuer.issue(user))
                .thenReturn("jwt-token");

        LoginResult result = service.login(
                new LoginCommand("john", "password")
        );

        assertEquals("jwt-token", result.accessToken());

        verify(resetLoginFailuresUseCase)
                .reset(user.getId());

        verify(tokenIssuer)
                .issue(user);
    }

    @Test
    void shouldRejectUnknownUsername() {
        when(userRepository.findByUsername("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.login(
                        new LoginCommand("unknown", "password")
                )
        );

        verifyNoInteractions(passwordHasher);
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void shouldRejectInvalidPassword() {
        User user = activeUser();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordHasher.matches(
                "wrong-password",
                "hashed-password"
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.login(
                        new LoginCommand("john", "wrong-password")
                )
        );

        verify(recordLoginFailureUseCase)
                .record(user.getId());

        verifyNoInteractions(tokenIssuer);

        verifyNoInteractions(resetLoginFailuresUseCase);
    }

    @Test
    void shouldRejectDisabledAccount() {
        User user = activeUser();
        user.disable();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        assertThrows(
                AccountDisabledException.class,
                () -> service.login(
                        new LoginCommand("john", "password")
                )
        );

        verifyNoInteractions(passwordHasher);
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void shouldRejectPendingAccount() {
        User user = new User(
                UUID.randomUUID(),
                "john",
                "hashed-password",
                UserStatus.PENDING,
                Instant.now()
        );

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        assertThrows(
                AccountPendingException.class,
                () -> service.login(
                        new LoginCommand("john", "password")
                )
        );

        verifyNoInteractions(passwordHasher);
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void shouldRejectLockedAccount() {
        User user = activeUser();

        user.lockUntil(
                Instant.now().plusSeconds(900)
        );

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        assertThrows(
                AccountLockedException.class,
                () -> service.login(
                        new LoginCommand("john", "password")
                )
        );

        verifyNoInteractions(passwordHasher);
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void shouldUnlockExpiredAccount() {
        User user = activeUser();

        user.lockUntil(
                Instant.now().minusSeconds(1)
        );

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordHasher.matches(
                "password",
                "hashed-password"
        )).thenReturn(true);

        when(tokenIssuer.issue(user))
                .thenReturn("jwt-token");

        LoginResult result = service.login(
                new LoginCommand("john", "password")
        );

        assertEquals("jwt-token", result.accessToken());

        assertTrue(user.isActive());

        verify(userRepository)
                .save(user);
    }

    @Test
    void shouldResetFailuresAfterSuccessfulLogin() {
        User user = activeUser();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordHasher.matches(
                "password",
                "hashed-password"
        )).thenReturn(true);

        when(tokenIssuer.issue(user))
                .thenReturn("jwt-token");

        service.login(
                new LoginCommand("john", "password")
        );

        verify(resetLoginFailuresUseCase)
                .reset(user.getId());
    }

    @Test
    void shouldRecordFailureAfterWrongPassword() {
        User user = activeUser();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordHasher.matches(
                "wrong",
                "hashed-password"
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.login(
                        new LoginCommand("john", "wrong")
                )
        );

        verify(recordLoginFailureUseCase)
                .record(user.getId());
    }

    private User activeUser() {
        return new User(
                UUID.randomUUID(),
                "john",
                "hashed-password",
                UserStatus.ACTIVE,
                Instant.now()
        );
    }
    @Test
    void shouldNormalizeUsernameBeforeLookup() {
        User user = activeUser();

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordHasher.matches(
                "password",
                "hashed-password"
        )).thenReturn(true);

        when(tokenIssuer.issue(user))
                .thenReturn("jwt-token");

        service.login(
                new LoginCommand("  JOHN  ", "password")
        );

        verify(userRepository)
                .findByUsername("john");
    }
}