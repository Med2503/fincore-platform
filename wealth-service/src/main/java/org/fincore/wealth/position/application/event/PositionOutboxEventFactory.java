package org.fincore.wealth.position.application.event;


import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class PositionOutboxEventFactory {

    private static final String EVENT_TYPE =
            "PositionUpdated";

    private final ObjectMapper objectMapper;

    public PositionOutboxEventFactory(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    public OutboxEvent create(
            Position position
    ) {
        PositionUpdatedEvent event =
                new PositionUpdatedEvent(
                        java.util.UUID.randomUUID(),
                        position.portfolioId(),
                        position.assetId(),
                        position.quantity(),
                        position.averageCost(),
                        position.currency(),
                        position.lastExecutionSequence(),
                        position.updatedAt()
                );

        try {
            String payload =
                    objectMapper.writeValueAsString(event);

            return OutboxEvent.pending(
                    position.portfolioId(),
                    EVENT_TYPE,
                    payload,
                    position.updatedAt()
            );

        } catch (JsonParseException exception) {
            throw new IllegalStateException(
                    "Cannot serialize position event",
                    exception
            );
        }
    }
}