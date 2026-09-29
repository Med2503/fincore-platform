package org.fincore.edge.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record EdgeJwtProperties(String secret) {
}
