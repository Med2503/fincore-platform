package org.fincore.wealth.position.infrastructure.messaging;

import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class PositionChangedConsumerConfiguration {

    @Bean
    public Consumer<PositionChangedEvent> positionChangedInput(
            PositionChangedConsumer consumer
    ) {
        return consumer::consume;
    }
}