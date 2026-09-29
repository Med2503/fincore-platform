package org.fincore.edge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public class EdgeJwtValidator {

    private final SecretKey signingKey;

    public EdgeJwtValidator(EdgeJwtProperties properties) {
        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 bytes");
        }

        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public JwtIdentity validate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String username = claims.getSubject();
        String userId = claims.get("userId", String.class);

        if (username == null || username.isBlank() || userId == null) {
            throw new IllegalArgumentException("Required JWT claims are missing");
        }

        UUID parsedUserId = UUID.fromString(userId);

        return new JwtIdentity(
                parsedUserId,
                username,
                readStringList(claims.get("roles")),
                readStringList(claims.get("permissions"))
        );
    }

    private List<String> readStringList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }

        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }
}
