package org.fincore.edge.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EdgeJwtValidatorTest {

    private static final String SECRET =
            "01234567890123456789012345678901";

    private EdgeJwtValidator validator;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        validator = new EdgeJwtValidator(new EdgeJwtProperties(SECRET));

        signingKey = new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    @Test
    void shouldValidateValidTokenAndExtractIdentity() {
        UUID userId = UUID.randomUUID();

        String token = createToken(
                userId,
                Instant.now().plusSeconds(300),
                signingKey
        );

        JwtIdentity identity = validator.validate(token);

        assertEquals(userId, identity.userId());
        assertEquals("demo", identity.username());
        assertEquals(List.of("CUSTOMER"), identity.roles());
        assertEquals(List.of("PROFILE_READ", "PORTFOLIO_READ"),
                identity.permissions());
    }

    @Test
    void shouldRejectExpiredToken() {
        String token = createToken(
                UUID.randomUUID(),
                Instant.now().minusSeconds(60),
                signingKey
        );

        assertThrows(RuntimeException.class, () -> validator.validate(token));
    }

    @Test
    void shouldRejectTokenSignedWithAnotherKey() {
        SecretKey anotherKey = new SecretKeySpec(
                "abcdefghijklmnopqrstuvwxyz123456".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        String token = createToken(
                UUID.randomUUID(),
                Instant.now().plusSeconds(300),
                anotherKey
        );

        assertThrows(RuntimeException.class, () -> validator.validate(token));
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThrows(
                RuntimeException.class,
                () -> validator.validate("this-is-not-a-jwt")
        );
    }

    @Test
    void shouldRejectTokenWithoutSubject() {
        String token = Jwts.builder()
                .claim("userId", UUID.randomUUID().toString())
                .claim("roles", List.of("CUSTOMER"))
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(signingKey)
                .compact();

        assertThrows(RuntimeException.class, () -> validator.validate(token));
    }

    @Test
    void shouldRejectInvalidUserIdClaim() {
        String token = Jwts.builder()
                .subject("demo")
                .claim("userId", "not-a-uuid")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(signingKey)
                .compact();

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(token));
    }

    @Test
    void shouldRejectSecretShorterThan32Bytes() {
        EdgeJwtProperties invalidProperties =
                new EdgeJwtProperties("too-short");

        assertThrows(
                IllegalStateException.class,
                () -> new EdgeJwtValidator(invalidProperties)
        );
    }

    private String createToken(
            UUID userId,
            Instant expiration,
            SecretKey key
    ) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject("demo")
                .claim("userId", userId.toString())
                .claim("roles", List.of("CUSTOMER"))
                .claim("permissions",
                        List.of("PROFILE_READ", "PORTFOLIO_READ"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }
}