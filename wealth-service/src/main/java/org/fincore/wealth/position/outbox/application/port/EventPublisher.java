package org.fincore.wealth.position.outbox.application.port;

public interface EventPublisher {

    boolean publish(
            String eventType,
            String payload
    );
}
