package org.fincore.identity.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UUID id;
    private String username;
    private String passwordHash;
    private UserStatus status;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    public User(
            UUID id,
            String username,
            String passwordHash,
            UserStatus status,
            Instant createdAt
    ) {
        this(
                id,
                username,
                passwordHash,
                status,
                0,
                null,
                createdAt,
                createdAt,
                0L
        );
    }

    public User(
            UUID id,
            String username,
            String passwordHash,
            UserStatus status,
            int failedLoginAttempts,
            Instant lockedUntil,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isLocked() {
        return status == UserStatus.LOCKED
                && lockedUntil != null
                && lockedUntil.isAfter(Instant.now());
    }

    public boolean isDisabled() {
        return status == UserStatus.DISABLED;
    }

    public boolean isPending() {
        return status == UserStatus.PENDING;
    }

    public void recordFailedLogin() {
        ensureLoginAllowed();

        failedLoginAttempts++;
        updatedAt = Instant.now();
    }

    public void resetFailedLoginAttempts() {
        failedLoginAttempts = 0;
        updatedAt = Instant.now();
    }

    public void lockUntil(Instant until) {
        if (until == null) {
            throw new IllegalArgumentException(
                    "Lock expiration cannot be null"
            );
        }

        status = UserStatus.LOCKED;
        lockedUntil = until;
        updatedAt = Instant.now();
    }

    public void activate() {
        status = UserStatus.ACTIVE;
        lockedUntil = null;
        failedLoginAttempts = 0;
        updatedAt = Instant.now();
    }

    public void disable() {
        status = UserStatus.DISABLED;
        lockedUntil = null;
        updatedAt = Instant.now();
    }

    private void ensureLoginAllowed() {
        if (status == UserStatus.DISABLED) {
            throw new IllegalStateException(
                    "Disabled user cannot authenticate"
            );
        }

        if (status == UserStatus.LOCKED) {
            throw new IllegalStateException(
                    "Locked user cannot authenticate"
            );
        }

        if (status == UserStatus.PENDING) {
            throw new IllegalStateException(
                    "Pending user cannot authenticate"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}