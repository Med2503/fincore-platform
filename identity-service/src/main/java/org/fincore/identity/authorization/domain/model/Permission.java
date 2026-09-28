package org.fincore.identity.authorization.domain.model;

import java.util.UUID;

public record Permission(
        UUID id,
        String name,
        String description
) {
    public Permission {
        if (id == null) {
            throw new IllegalArgumentException(" id is required");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(" name is required");
        }

        name = name.trim().toUpperCase();
    }

}
