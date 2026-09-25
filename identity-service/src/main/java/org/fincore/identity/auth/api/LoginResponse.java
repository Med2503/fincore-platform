package org.fincore.identity.auth.api;

public record LoginResponse(
        String accessToken,
        String tokenType
) {
}
