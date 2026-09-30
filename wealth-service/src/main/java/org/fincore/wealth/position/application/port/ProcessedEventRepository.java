package org.fincore.wealth.position.application.port;

import java.util.UUID;

public interface ProcessedEventRepository {
    boolean alreadyProcessed(UUID eventId);

    void markProcessed(UUID eventId);
}