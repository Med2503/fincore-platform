package org.fincore.identity.user.domain.model;

import java.time.Instant;

public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private UserStatus status;
    private Instant lockUntil;
    private int failedLoginAttempts;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    public User(Long id,
                String username,
                String passwordHash,
                UserStatus status,
                Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;

    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isLocked() {
        return status == UserStatus.LOCKED && lockUntil != null && lockUntil.isAfter(Instant.now());
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
        this.status = UserStatus.LOCKED;
        this.lockUntil = until;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.lockUntil = null;
        this.updatedAt = Instant.now();
    }


    public long getId() {
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

    public Instant getLockUntil() {
        return lockUntil;
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
