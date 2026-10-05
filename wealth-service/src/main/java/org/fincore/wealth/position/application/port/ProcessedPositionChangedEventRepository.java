package org.fincore.wealth.position.application.port;

import java.util.UUID;

public interface ProcessedPositionChangedEventRepository {

    boolean claim(UUID eventId);
}