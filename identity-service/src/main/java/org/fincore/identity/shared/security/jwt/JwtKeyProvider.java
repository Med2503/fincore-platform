package org.fincore.identity.shared.security.jwt;


import io.jsonwebtoken.security.Keys;
import org.fincore.identity.shared.security.config.JwtProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtKeyProvider {
    private final SecretKey signingKey;

    public JwtKeyProvider(JwtProperties properties) {
        validate(properties.secret());
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }

    private void validate(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must be configured");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 256 bits");
        }


    }
}
