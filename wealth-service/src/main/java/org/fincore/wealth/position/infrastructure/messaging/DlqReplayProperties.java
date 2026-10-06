package org.fincore.wealth.position.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "wealth.dlq.replay"
)
public record DlqReplayProperties(
        int maxReplays
) {
}