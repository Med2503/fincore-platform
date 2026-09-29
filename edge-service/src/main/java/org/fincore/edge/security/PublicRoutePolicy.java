package org.fincore.edge.security;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PublicRoutePolicy {

    private final List<String> publicPaths = List.of(
            "/api/v1/users",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh"
    );

    public boolean isPublic(String path) {
        return publicPaths.stream().anyMatch(path::equals);
    }
}
