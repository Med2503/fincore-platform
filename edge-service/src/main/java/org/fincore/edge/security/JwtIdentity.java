package org.fincore.edge.security;

import java.util.List;
import java.util.UUID;

public record JwtIdentity(
        UUID userId,
        String username,
        List<String> roles,
        List<String> permissions
) {
}
