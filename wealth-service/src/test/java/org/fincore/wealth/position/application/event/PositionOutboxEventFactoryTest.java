package org.fincore.wealth.position.application.event;

import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PositionOutboxEventFactoryTest {

    private PositionOutboxEventFactory factory;

    private UUID portfolioId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        factory = new PositionOutboxEventFactory(
                new ObjectMapper()
        );

        portfolioId = UUID.randomUUID();
        assetId = UUID.randomUUID();
    }

    @Test
    void shouldCreatePositionUpdatedOutboxEvent() {
        Instant updatedAt = Instant.parse(
                "2026-10-02T10:00:00Z"
        );

        Position position = new Position(
                UUID.randomUUID(),
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("105.50"),
                "USD",
                updatedAt,
                101L
        );

        OutboxEvent result = factory.create(position);

        assertThat(result.id())
                .isNotNull();

        assertThat(result.aggregateId())
                .isEqualTo(portfolioId);

        assertThat(result.eventType())
                .isEqualTo("PositionUpdated");

        assertThat(result.status())
                .isEqualTo(
                        org.fincore.wealth.position.outbox.domain.OutboxEventStatus.PENDING
                );

        assertThat(result.occurredAt())
                .isEqualTo(updatedAt);

        JsonNode payload = readPayload(result.payload());

        assertThat(payload.get("eventId"))
                .isNotNull();

        assertThat(payload.get("portfolioId").asText())
                .isEqualTo(portfolioId.toString());

        assertThat(payload.get("assetId").asText())
                .isEqualTo(assetId.toString());

        assertThat(payload.get("quantity").asText())
                .isEqualTo("10");

        assertThat(payload.get("averageCost").asText())
                .isEqualTo("105.50");

        assertThat(payload.get("currency").asText())
                .isEqualTo("USD");

        assertThat(payload.get("executionSequence").asLong())
                .isEqualTo(101L);

        assertThat(payload.get("occurredAt").asText())
                .isEqualTo(updatedAt.toString());
    }

    @Test
    void shouldUsePositionUpdatedAtAsEventOccurredAt() {
        Instant updatedAt = Instant.parse(
                "2026-10-02T12:30:45Z"
        );

        Position position = new Position(
                UUID.randomUUID(),
                portfolioId,
                assetId,
                new BigDecimal("5"),
                new BigDecimal("200"),
                "EUR",
                updatedAt,
                50L
        );

        OutboxEvent result = factory.create(position);

        assertThat(result.occurredAt())
                .isEqualTo(updatedAt);
    }

    private JsonNode readPayload(String payload) {
        try {
            return new ObjectMapper().readTree(payload);
        } catch (Exception exception) {
            throw new AssertionError(
                    "Invalid JSON payload",
                    exception
            );
        }
    }
}
