package org.fincore.wealth.position.outbox.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "wealth.outbox.publisher"
)
public record OutboxPublisherProperties(
        Duration fixedDelay,
        int batchSize,
        int maxRetries,
        Duration leaseDuration
) {

    public OutboxPublisherProperties {
        if (fixedDelay.isNegative()
                || fixedDelay.isZero()) {
            throw new IllegalArgumentException(
                    "Fixed delay must be positive"
            );
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "Batch size must be positive"
            );
        }

        if (maxRetries <= 0) {
            throw new IllegalArgumentException(
                    "Maximum retries must be positive"
            );
        }

        if (leaseDuration.isNegative()
                || leaseDuration.isZero()) {
            throw new IllegalArgumentException(
                    "Lease duration must be positive"
            );
        }
    }
}