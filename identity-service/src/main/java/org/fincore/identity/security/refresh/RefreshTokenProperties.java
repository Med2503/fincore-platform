package org.fincore.identity.security.refresh;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.refresh-token")
public record RefreshTokenProperties(Duration expiration) {
}