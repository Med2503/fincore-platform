package org.fincore.edge.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitConfigTest {

    private final KeyResolver keyResolver =
            new RateLimitConfig().clientKeyResolver();

    @Test
    void shouldUseUserIdWhenIdentityHeaderExists() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/wealth/portfolio")
                        .header("X-User-Id", "user-123")
                        .build()
        );

        String key = keyResolver.resolve(exchange).block();

        assertEquals("user:user-123", key);
    }

    @Test
    void shouldUseIpAddressWhenUserIdIsMissing() {
        var request = MockServerHttpRequest.get("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("192.168.1.25", 50000))
                .build();

        var exchange = MockServerWebExchange.from(request);

        String key = keyResolver.resolve(exchange).block();

        assertEquals("ip:192.168.1.25", key);
    }

    @Test
    void shouldNotUseClientSuppliedIdentityForPublicRequest() {

        var request = MockServerHttpRequest.post("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("10.0.0.8", 50000))
                .build();

        var exchange = MockServerWebExchange.from(request);

        String key = keyResolver.resolve(exchange).block();

        assertEquals("ip:10.0.0.8", key);
    }
}