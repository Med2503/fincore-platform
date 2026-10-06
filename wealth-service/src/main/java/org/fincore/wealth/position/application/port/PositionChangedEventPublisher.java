package org.fincore.wealth.position.application.port;

public interface PositionChangedEventPublisher {

    boolean publish(String payload);
}