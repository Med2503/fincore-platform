package org.fincore.identity.shared.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.fincore.identity.shared.security.config.JwtProperties;
import org.fincore.identity.shared.security.jwt.InvalidJwtException;
import org.fincore.identity.shared.security.jwt.JwtKeyProvider;
import org.fincore.identity.shared.security.jwt.JwtPrincipal;
import org.fincore.identity.shared.security.jwt.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenValidatorTest {

    private static final String SECRET =
            "12345678901234567890123456789012";

    private JwtTokenValidator validator;

    @BeforeEach
    void setUp() {
        var properties = new JwtProperties(SECRET, 900);
        var keyProvider = new JwtKeyProvider(properties);

        validator = new JwtTokenValidator(keyProvider);
    }

    @Test
    void shouldValidateCorrectToken() {
        UUID userId = UUID.randomUUID();

        String token = createToken(
                "john",
                userId.toString(),
                "CUSTOMER",
                Instant.now().plusSeconds(900)
        );

        JwtPrincipal principal = validator.validate(token);

        assertEquals(userId, principal.userId());
        assertEquals("john", principal.username());
        assertEquals("CUSTOMER", principal.role());
    }

    @Test
    void shouldRejectExpiredToken() {
        String token = createToken(
                "john",
                UUID.randomUUID().toString(),
                "CUSTOMER",
                Instant.now().minusSeconds(60)
        );

        assertThrows(
                InvalidJwtException.class,
                () -> validator.validate(token)
        );
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThrows(
                InvalidJwtException.class,
                () -> validator.validate("not-a-jwt")
        );
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() {
        String token = Jwts.builder()
                .subject("john")
                .claim("userId", UUID.randomUUID().toString())
                .claim("role", "CUSTOMER")
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(Keys.hmacShaKeyFor(
                        "different-secret-123456789012345".getBytes()
                ))
                .compact();

        assertThrows(
                InvalidJwtException.class,
                () -> validator.validate(token)
        );
    }

    private String createToken(
            String username,
            String userId,
            String role,
            Instant expiration
    ) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();
    }
}