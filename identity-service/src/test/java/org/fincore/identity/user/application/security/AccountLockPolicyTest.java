package org.fincore.identity.user.application.security;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountLockPolicyTest {

    private final AccountLockPolicy policy =
            new AccountLockPolicy();

    @Test
    void shouldLockWhenMaximumAttemptsReached() {

        assertTrue(
                policy.shouldLock(5)
        );
    }

    @Test
    void shouldNotLockBeforeMaximumAttempts() {

        assertFalse(
                policy.shouldLock(4)
        );
    }

    @Test
    void shouldLockWhenAttemptsExceedMaximum() {

        assertTrue(
                policy.shouldLock(6)
        );
    }

    @Test
    void shouldHaveFifteenMinutesLockDuration() {

        assertEquals(
                15,
                policy.lockDuration().toMinutes()
        );
    }
}