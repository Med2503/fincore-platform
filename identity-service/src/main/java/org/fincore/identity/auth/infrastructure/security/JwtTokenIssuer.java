package org.fincore.identity.auth.infrastructure.security;


import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import org.fincore.identity.auth.application.port.TokenIssuer;
import org.fincore.identity.auth.application.port.UserAuthoritiesProvider;
import org.fincore.identity.auth.application.port.UserRoleProvider;
import org.fincore.identity.shared.security.config.JwtProperties;
import org.fincore.identity.shared.security.jwt.JwtKeyProvider;
import org.fincore.identity.user.domain.model.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtKeyProvider keyProvider;
    private final JwtProperties properties;
    private final UserRoleProvider roleProvider;
    private final UserAuthoritiesProvider authoritiesProvider;

    public JwtTokenIssuer(JwtKeyProvider keyProvider, JwtProperties properties, UserRoleProvider provider, UserAuthoritiesProvider authoritiesProvider) {
        this.keyProvider = keyProvider;
        this.properties = properties;
        this.roleProvider = provider;
        this.authoritiesProvider = authoritiesProvider;
    }

    @Override
    public String issue(User user) {

        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plusSeconds(properties.accessTokenExpiration());
        List<String> roles = roleProvider.getRoles(user);
        List<String> permissions = authoritiesProvider.getAuthorities(user.getId());

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(keyProvider.getSigningKey())
                .compact();
    }
}
