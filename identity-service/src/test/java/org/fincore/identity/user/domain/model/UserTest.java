package org.fincore.identity.user.domain.model;


import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

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
    void shouldRecordFailedLogin() {

        User user = activeUser();

        user.recordFailedLogin();

        assertEquals(
                1,
                user.getFailedLoginAttempts()
        );
    }

    @Test
    void shouldResetFailedLoginAttempts() {

        User user = activeUser();

        user.recordFailedLogin();
        user.recordFailedLogin();

        user.resetFailedLoginAttempts();

        assertEquals(
                0,
                user.getFailedLoginAttempts()
        );
    }

    @Test
    void shouldLockUser() {

        User user = activeUser();

        Instant lockedUntil =
                Instant.now().plusSeconds(900);

        user.lockUntil(lockedUntil);

        assertEquals(
                UserStatus.LOCKED,
                user.getStatus()
        );

        assertEquals(
                lockedUntil,
                user.getLockedUntil()
        );

        assertTrue(user.isLocked());
    }

    @Test
    void shouldActivateUser() {

        User user = activeUser();

        user.lockUntil(
                Instant.now().plusSeconds(900)
        );

        user.activate();

        assertEquals(
                UserStatus.ACTIVE,
                user.getStatus()
        );

        assertNull(user.getLockedUntil());

        assertEquals(
                0,
                user.getFailedLoginAttempts()
        );
    }

    @Test
    void shouldDisableUser() {

        User user = activeUser();

        user.disable();

        assertEquals(
                UserStatus.DISABLED,
                user.getStatus()
        );

        assertFalse(user.isActive());
    }

    @Test
    void shouldRejectFailedLoginForDisabledUser() {

        User user = activeUser();

        user.disable();

        assertThrows(
                IllegalStateException.class,
                user::recordFailedLogin
        );
    }

    @Test
    void shouldRejectFailedLoginForLockedUser() {

        User user = activeUser();

        user.lockUntil(
                Instant.now().plusSeconds(900)
        );

        assertThrows(
                IllegalStateException.class,
                user::recordFailedLogin
        );
    }
}