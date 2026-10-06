package org.fincore.wealth.position.infrastructure.messaging;

import org.fincore.wealth.position.application.port.PositionChangedEventPublisher;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class PositionChangedEventPublisherAdapter
        implements PositionChangedEventPublisher {

    private static final String BINDING =
            "positionChangedReplay-out-0";

    private final StreamBridge streamBridge;

    public PositionChangedEventPublisherAdapter(
            StreamBridge streamBridge
    ) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean publish(String payload) {
        return streamBridge.send(
                BINDING,
                payload
        );
    }
}