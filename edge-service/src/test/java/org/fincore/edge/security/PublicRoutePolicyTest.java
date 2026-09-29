package org.fincore.edge.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PublicRoutePolicyTest {

    private final PublicRoutePolicy policy = new PublicRoutePolicy();

    @Test
    void shouldAllowRegistrationEndpoint() {
        assertTrue(policy.isPublic("/api/v1/users"));
    }

    @Test
    void shouldAllowLoginEndpoint() {
        assertTrue(policy.isPublic("/api/v1/auth/login"));
    }

    @Test
    void shouldAllowRefreshEndpoint() {
        assertTrue(policy.isPublic("/api/v1/auth/refresh"));
    }

    @Test
    void shouldNotAllowPrivateEndpoint() {
        assertFalse(policy.isPublic("/api/v1/users/me"));
    }

    @Test
    void shouldNotAllowUnknownAuthEndpoint() {
        assertFalse(policy.isPublic("/api/v1/auth/logout-all"));
    }

    @Test
    void shouldNotAllowSimilarButDifferentPath() {
        assertFalse(policy.isPublic("/api/v1/users/"));
    }
}
