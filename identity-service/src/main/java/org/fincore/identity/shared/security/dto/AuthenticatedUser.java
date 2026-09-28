package org.fincore.identity.shared.security.dto;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String username
) {
}
