package org.fincore.wealth.position.infrastructure.messaging;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(
        DlqReplayProperties.class
)
public class DlqReplayConfiguration {
}