package org.fincore.identity.shared.security.jwt;

import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        String username,
        String role
) {
}
