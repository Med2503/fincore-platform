package org.fincore.identity.authorization.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Role(
        UUID id,
        String name,
        String description,
        Instant createdAt
) {
    public Role {
        if (id == null) {
            throw new IllegalArgumentException("id is required!");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        name = name.trim().toUpperCase();
    }
}
