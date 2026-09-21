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

    // Création d'un nouvel utilisateur
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

    // Reconstruction complète depuis la persistence
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

    public void recordFailedLogin() {
        failedLoginAttempts++;
        updatedAt = Instant.now();
    }

    public void resetFailedLoginAttempts() {
        failedLoginAttempts = 0;
        updatedAt = Instant.now();
    }

    public void lockUntil(Instant until) {
        status = UserStatus.LOCKED;
        lockedUntil = until;
        updatedAt = Instant.now();
    }

    public void activate() {
        status = UserStatus.ACTIVE;
        lockedUntil = null;
        updatedAt = Instant.now();
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