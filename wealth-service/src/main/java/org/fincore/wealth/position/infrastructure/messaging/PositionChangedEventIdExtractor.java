package org.fincore.wealth.position.infrastructure.messaging;


import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
public class PositionChangedEventIdExtractor {

    private final ObjectMapper objectMapper;

    public PositionChangedEventIdExtractor(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    public UUID extract(String payload) {

        try {
            JsonNode root =
                    objectMapper.readTree(payload);

            JsonNode eventId =
                    root.get("eventId");

            if (eventId == null
                    || eventId.isNull()
                    || eventId.asText().isBlank()) {

                throw new IllegalArgumentException(
                        "Missing eventId in payload"
                );
            }

            return UUID.fromString(
                    eventId.asText()
            );

        } catch (Exception exception) {

            throw new IllegalArgumentException(
                    "Invalid PositionChanged payload",
                    exception
            );
        }
    }
}