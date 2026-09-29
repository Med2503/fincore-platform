package org.fincore.edge.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String ROLES_HEADER = "X-User-Roles";
    private static final String PERMISSIONS_HEADER = "X-User-Permissions";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final EdgeJwtValidator validator;
    private final PublicRoutePolicy publicRoutePolicy;

    public JwtGatewayFilter(
            EdgeJwtValidator validator,
            PublicRoutePolicy publicRoutePolicy
    ) {
        this.validator = validator;
        this.publicRoutePolicy = publicRoutePolicy;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        ServerHttpRequest.Builder builder = request.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USERNAME_HEADER);
                    headers.remove(ROLES_HEADER);
                    headers.remove(PERMISSIONS_HEADER);
                });

        String correlationId = request.getHeaders().getFirst(CORRELATION_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        builder.header(CORRELATION_HEADER, correlationId);

        if (publicRoutePolicy.isPublic(path)) {
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        String authorization = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authorization.substring(7).trim();

        try {
            JwtIdentity identity = validator.validate(token);

            builder.header(USER_ID_HEADER, identity.userId().toString());
            builder.header(USERNAME_HEADER, identity.username());
            builder.header(ROLES_HEADER, String.join(",", identity.roles()));
            builder.header(PERMISSIONS_HEADER, String.join(",", identity.permissions()));

            return chain.filter(exchange.mutate().request(builder.build()).build());
        } catch (RuntimeException exception) {
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}