package org.fincore.wealth.position.infrastructure.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class PositionChangedDlqConfiguration {

    @Bean
    public Consumer<Message<String>> positionChangedDlqInput(
            PositionChangedDlqConsumer consumer
    ) {
        return consumer::consume;
    }
}