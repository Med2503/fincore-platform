package org.fincore.wealth.position.application.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PositionOutboxEventFactoryTest {

    private PositionOutboxEventFactory factory;

    @BeforeEach
    void setUp() {
        factory =
                new PositionOutboxEventFactory(
                        new ObjectMapper()
                );
    }

    @Test
    void shouldUseEventIdAsOutboxId() {
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        Position position =
                new Position(
                        UUID.randomUUID(),
                        portfolioId,
                        assetId,
                        new BigDecimal("10"),
                        new BigDecimal("100"),
                        "EUR",
                        Instant.parse(
                                "2026-09-30T10:00:00Z"
                        ),
                        1
                );

        OutboxEvent outbox =
                factory.create(
                        position,
                        1,
                        Instant.parse(
                                "2026-09-30T10:00:00Z"
                        )
                );

        assertNotNull(outbox.id());

        assertEquals(
                portfolioId,
                outbox.aggregateId()
        );

        assertEquals(
                "PositionChanged",
                outbox.eventType()
        );
    }

    @Test
    void shouldUsePortfolioAsAggregateIdForClosedPosition() {
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        OutboxEvent outbox =
                factory.createClosed(
                        portfolioId,
                        assetId,
                        "EUR",
                        7,
                        Instant.parse(
                                "2026-09-30T10:00:00Z"
                        )
                );

        assertNotNull(outbox.id());

        assertEquals(
                portfolioId,
                outbox.aggregateId()
        );

        assertEquals(
                "PositionChanged",
                outbox.eventType()
        );
    }

    @Test
    void shouldSerializeSameIdentityInsidePayload() throws Exception {
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        Position position =
                new Position(
                        UUID.randomUUID(),
                        portfolioId,
                        assetId,
                        new BigDecimal("10"),
                        new BigDecimal("100"),
                        "EUR",
                        Instant.parse(
                                "2026-09-30T10:00:00Z"
                        ),
                        3
                );

        OutboxEvent outbox =
                factory.create(
                        position,
                        3,
                        Instant.parse(
                                "2026-09-30T10:00:00Z"
                        )
                );

        JsonNode payload =
                new ObjectMapper()
                        .readTree(outbox.payload());

        assertEquals(
                outbox.id().toString(),
                payload
                        .get("eventId")
                        .asText()
        );

        assertEquals(
                portfolioId.toString(),
                payload
                        .get("portfolioId")
                        .asText()
        );

        assertEquals(
                portfolioId,
                outbox.aggregateId()
        );
    }
}