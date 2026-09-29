package org.fincore.edge.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtGatewayFilterTest {

    private static final String SECRET =
            "01234567890123456789012345678901";

    private JwtGatewayFilter filter;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        EdgeJwtValidator validator =
                new EdgeJwtValidator(new EdgeJwtProperties(SECRET));

        filter = new JwtGatewayFilter(
                validator,
                new PublicRoutePolicy()
        );

        signingKey = new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    @Test
    void shouldAllowPublicRouteWithoutJwt() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login").build()
        );

        AtomicReference<ServerWebExchange> forwarded =
                new AtomicReference<>();

        GatewayFilterChain chain = nextExchange -> {
            forwarded.set(nextExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(forwarded.get());
        assertEquals(
                "/api/v1/auth/login",
                forwarded.get().getRequest().getURI().getPath()
        );
    }

    @Test
    void shouldRejectPrivateRouteWithoutJwt() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me").build()
        );

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse().getStatusCode()
        );

        verifyNoInteractions(chain);
    }

    @Test
    void shouldRejectInvalidBearerToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .build()
        );

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse().getStatusCode()
        );

        verifyNoInteractions(chain);
    }

    @Test
    void shouldForwardValidJwtAndPropagateIdentity() {
        UUID userId = UUID.randomUUID();
        String token = createToken(userId);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );

        AtomicReference<ServerWebExchange> forwarded =
                new AtomicReference<>();

        GatewayFilterChain chain = nextExchange -> {
            forwarded.set(nextExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(forwarded.get());

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();

        assertEquals(userId.toString(), headers.getFirst("X-User-Id"));
        assertEquals("demo", headers.getFirst("X-Username"));
        assertEquals("CUSTOMER", headers.getFirst("X-User-Roles"));
        assertEquals(
                "PROFILE_READ,PORTFOLIO_READ",
                headers.getFirst("X-User-Permissions")
        );
    }

    @Test
    void shouldRemoveIdentityHeadersProvidedByClient() {
        UUID trustedUserId = UUID.randomUUID();
        String token = createToken(trustedUserId);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", "attacker-user-id")
                        .header("X-Username", "attacker")
                        .header("X-User-Roles", "ADMIN")
                        .header("X-User-Permissions", "ROLE_ASSIGN")
                        .build()
        );

        AtomicReference<ServerWebExchange> forwarded =
                new AtomicReference<>();

        GatewayFilterChain chain = nextExchange -> {
            forwarded.set(nextExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();

        assertEquals(trustedUserId.toString(), headers.getFirst("X-User-Id"));
        assertEquals("demo", headers.getFirst("X-Username"));
        assertEquals("CUSTOMER", headers.getFirst("X-User-Roles"));
        assertEquals(
                "PROFILE_READ,PORTFOLIO_READ",
                headers.getFirst("X-User-Permissions")
        );
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login").build()
        );

        AtomicReference<ServerWebExchange> forwarded =
                new AtomicReference<>();

        GatewayFilterChain chain = nextExchange -> {
            forwarded.set(nextExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String correlationId = forwarded.get()
                .getRequest()
                .getHeaders()
                .getFirst("X-Correlation-Id");

        assertNotNull(correlationId);
        assertFalse(correlationId.isBlank());

        assertDoesNotThrow(() -> UUID.fromString(correlationId));
    }

    @Test
    void shouldPreserveExistingCorrelationId() {
        String correlationId = "correlation-test-123";

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login")
                        .header("X-Correlation-Id", correlationId)
                        .build()
        );

        AtomicReference<ServerWebExchange> forwarded =
                new AtomicReference<>();

        GatewayFilterChain chain = nextExchange -> {
            forwarded.set(nextExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals(
                correlationId,
                forwarded.get().getRequest()
                        .getHeaders()
                        .getFirst("X-Correlation-Id")
        );
    }

    private String createToken(UUID userId) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject("demo")
                .claim("userId", userId.toString())
                .claim("roles", List.of("CUSTOMER"))
                .claim("permissions",
                        List.of("PROFILE_READ", "PORTFOLIO_READ"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(signingKey)
                .compact();
    }
}