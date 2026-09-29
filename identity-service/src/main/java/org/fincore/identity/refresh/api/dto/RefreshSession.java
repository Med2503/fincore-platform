package org.fincore.identity.refresh.api.dto;

public record RefreshSession(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
