package org.fincore.identity.refresh.api.dto;

public record RefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
