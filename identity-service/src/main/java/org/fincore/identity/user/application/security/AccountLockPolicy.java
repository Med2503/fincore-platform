package org.fincore.identity.user.application.security;


import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AccountLockPolicy {

    private final int maxFailedAttempts;
    private final Duration lockDuration;

    public AccountLockPolicy() {
        this.maxFailedAttempts = 5;
        this.lockDuration = Duration.ofMinutes(15);
    }

    public boolean shouldLock(int failedAttempts) {
        return failedAttempts >= maxFailedAttempts;
    }

    public Duration lockDuration() {
        return lockDuration;
    }

    public int maxFailedAttempts() {
        return maxFailedAttempts;
    }
}
