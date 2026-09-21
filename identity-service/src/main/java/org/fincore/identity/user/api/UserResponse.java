package org.fincore.identity.user.api;

import org.fincore.identity.user.domain.model.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String status,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getStatus().name(),
                user.getCreatedAt()
        );

    }
}
