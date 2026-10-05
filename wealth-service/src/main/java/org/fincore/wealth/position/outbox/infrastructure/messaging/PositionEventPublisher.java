package org.fincore.wealth.position.outbox.infrastructure.messaging;

import org.fincore.wealth.position.outbox.application.port.EventPublisher;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class PositionEventPublisher implements EventPublisher {

    private static final String BINDING =
            "positionChanged-out-0";

    private final StreamBridge streamBridge;

    public PositionEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean publish(
            String eventType,
            String payload
    ) {
        if (!"PositionChanged".equals(eventType)) {
            throw new IllegalArgumentException(
                    "Unsupported event type: " + eventType
            );
        }

        return streamBridge.send(
                BINDING,
                payload
        );
    }
}