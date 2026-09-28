package org.fincore.identity.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.fincore.identity.shared.security.dto.AuthenticatedUser;
import org.fincore.identity.shared.security.jwt.InvalidJwtException;
import org.fincore.identity.shared.security.jwt.JwtPrincipal;
import org.fincore.identity.shared.security.jwt.JwtTokenValidator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenValidator tokenValidator;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtTokenValidator tokenValidator,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.tokenValidator = tokenValidator;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();

        try {
            JwtPrincipal principal = tokenValidator.validate(token);

            var authority = new SimpleGrantedAuthority(
                    "ROLE_" + principal.role()
            );

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            new AuthenticatedUser(
                                    principal.userId(),
                                    principal.username()
                            ),
                            null,
                            List.of(authority)
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (InvalidJwtException exception) {
            SecurityContextHolder.clearContext();

            authenticationEntryPoint.commence(
                    request,
                    response,
                    new org.springframework.security.authentication
                            .BadCredentialsException("Invalid bearer token", exception)
            );
        }
    }
}