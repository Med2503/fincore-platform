package org.fincore.identity.user.application.service;


import org.fincore.identity.user.application.exception.UserNotFoundException;
import org.fincore.identity.user.application.security.AccountLockPolicy;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.model.UserStatus;
import org.fincore.identity.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordLoginFailureServiceTest {

    @Mock
    private UserRepository userRepository;

    private RecordLoginFailureService service;

    private AccountLockPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new AccountLockPolicy();

        service = new RecordLoginFailureService(
                userRepository,
                policy
        );
    }

    @Test
    void shouldIncrementFailedAttempts() {

        UUID userId = UUID.randomUUID();

        User user = activeUser();

        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(user));

        service.record(userId);

        assertEquals(
                1,
                user.getFailedLoginAttempts()
        );

        assertEquals(
                UserStatus.ACTIVE,
                user.getStatus()
        );

        verify(userRepository)
                .save(user);
    }

    @Test
    void shouldLockUserAfterFiveFailures() {

        UUID userId = UUID.randomUUID();

        User user = activeUser();

        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(user));

        service.record(userId);
        service.record(userId);
        service.record(userId);
        service.record(userId);
        service.record(userId);

        assertEquals(
                5,
                user.getFailedLoginAttempts()
        );

        assertEquals(
                UserStatus.LOCKED,
                user.getStatus()
        );

        assertNotNull(
                user.getLockedUntil()
        );

        assertTrue(
                user.getLockedUntil()
                        .isAfter(Instant.now())
        );
    }

    @Test
    void shouldSaveUserAfterFailure() {

        UUID userId = UUID.randomUUID();

        User user = activeUser();

        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(user));

        service.record(userId);

        verify(userRepository)
                .save(user);
    }

    private User activeUser() {

        Instant now = Instant.now();

        return new User(
                UUID.randomUUID(),
                "john",
                "hash",
                UserStatus.ACTIVE,
                now
        );
    }
    @Test
    void shouldThrowWhenUserDoesNotExist() {

        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> service.record(userId)
        );

        verify(userRepository, never())
                .save(any());
    }
}
