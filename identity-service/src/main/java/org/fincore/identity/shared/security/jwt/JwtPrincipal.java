package org.fincore.identity.shared.security.jwt;

import java.util.List;
import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        String username,
        List<String> roles,
        List<String> permissions
) {
}