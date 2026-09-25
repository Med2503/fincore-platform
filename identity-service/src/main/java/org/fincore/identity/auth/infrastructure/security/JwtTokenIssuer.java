package org.fincore.identity.auth.infrastructure.security;


import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import org.fincore.identity.auth.application.port.TokenIssuer;
import org.fincore.identity.auth.application.port.UserRoleProvider;
import org.fincore.identity.shared.security.config.JwtProperties;
import org.fincore.identity.shared.security.jwt.JwtKeyProvider;
import org.fincore.identity.user.domain.model.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtKeyProvider keyProvider;
    private final JwtProperties properties;
    private final UserRoleProvider provider;

    public JwtTokenIssuer(JwtKeyProvider keyProvider, JwtProperties properties, UserRoleProvider provider) {
        this.keyProvider = keyProvider;
        this.properties = properties;
        this.provider = provider;
    }

    @Override
    public String issue(User user) {

        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plusSeconds(properties.accessTokenExpiration());
        String role = provider.getPrimaryRole(user);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("role", role)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(keyProvider.getSigningKey())
                .compact();
    }
}
