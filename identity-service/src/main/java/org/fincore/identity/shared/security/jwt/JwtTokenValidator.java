package org.fincore.identity.shared.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenValidator {

    private final JwtKeyProvider keyProvider;

    public JwtTokenValidator(JwtKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public JwtPrincipal validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keyProvider.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            String userIdValue = claims.get("userId", String.class);
            List<String> roles = claims.get("roles", List.class);
            List<String> permissions = claims.get("permissions", List.class);

            if (username == null || username.isBlank()) {
                throw new InvalidJwtException("Missing subject");
            }

            if (roles == null || permissions == null) {
                throw new InvalidJwtException("Missing role");
            }

            UUID userId = UUID.fromString(userIdValue);

            return new JwtPrincipal(userId, username, roles, permissions);

        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidJwtException("Invalid or expired JWT", exception);
        }
    }
}