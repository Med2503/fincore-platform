package org.fincore.identity.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.fincore.identity.auth.application.port.UserRoleProvider;
import org.fincore.identity.shared.security.config.JwtProperties;
import org.fincore.identity.shared.security.jwt.JwtKeyProvider;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenIssuerTest {

    private static final String SECRET =
            "12345678901234567890123456789012";

    private JwtTokenIssuer issuer;

    @BeforeEach
    void setUp() {

        JwtProperties properties =
                new JwtProperties(
                        SECRET,
                        900
                );

        JwtKeyProvider keyProvider =
                new JwtKeyProvider(properties);

        UserRoleProvider roleProvider =
                user -> "CUSTOMER";

        issuer = new JwtTokenIssuer(
                keyProvider,
                properties,
                roleProvider
        );
    }

    @Test
    void shouldGenerateValidJwt() {

        User user = createUser();

        String token = issuer.issue(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldContainUsernameAsSubject() {

        User user = createUser();

        String token = issuer.issue(user);

        Claims claims = parse(token);

        assertEquals(
                user.getUsername(),
                claims.getSubject()
        );
    }

    @Test
    void shouldContainUserId() {

        User user = createUser();

        String token = issuer.issue(user);

        Claims claims = parse(token);

        assertEquals(
                user.getId().toString(),
                claims.get("userId", String.class)
        );
    }

    @Test
    void shouldContainRole() {

        User user = createUser();

        String token = issuer.issue(user);

        Claims claims = parse(token);

        assertEquals(
                "CUSTOMER",
                claims.get("role", String.class)
        );
    }

    @Test
    void shouldContainExpiration() {

        User user = createUser();

        String token = issuer.issue(user);

        Claims claims = parse(token);

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        assertTrue(
                claims.getExpiration()
                        .after(claims.getIssuedAt())
        );
    }

    @Test
    void shouldUseConfiguredExpiration() {

        User user = createUser();

        String token = issuer.issue(user);

        Claims claims = parse(token);

        long duration =
                claims.getExpiration().getTime()
                        - claims.getIssuedAt().getTime();

        assertEquals(
                900_000,
                duration
        );
    }
    @Test
    void shouldRejectBlankSecret() {

        JwtProperties properties =
                new JwtProperties("", 900);

        assertThrows(
                IllegalStateException.class,
                () -> new JwtKeyProvider(properties)
        );
    }
    @Test
    void shouldRejectShortSecret() {

        JwtProperties properties =
                new JwtProperties("short-secret", 900);

        assertThrows(
                IllegalStateException.class,
                () -> new JwtKeyProvider(properties)
        );
    }

    private Claims parse(String token) {

        SecretKey key = Keys.hmacShaKeyFor(
                SECRET.getBytes()
        );

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private User createUser() {

        return new User(
                UUID.randomUUID(),
                "john",
                "hashed-password",
                UserStatus.ACTIVE,
                Instant.now()
        );
    }
}
