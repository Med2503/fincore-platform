package org.fincore.wealth.position.application.event;

import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.domain.OutboxEventStatus;
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
    void shouldCreatePositionChangedOutboxEvent() {
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

        OutboxEvent result =
                factory.create(position);

        assertThat(result.id())
                .isNotNull();

        assertThat(result.aggregateId())
                .isEqualTo(portfolioId);

        assertThat(result.eventType())
                .isEqualTo("PositionChanged");

        assertThat(result.status())
                .isEqualTo(OutboxEventStatus.PENDING);

        assertThat(result.occurredAt())
                .isEqualTo(updatedAt);

        assertThat(result.publishedAt())
                .isNull();

        assertThat(result.retryCount())
                .isZero();

        JsonNode payload =
                readPayload(result.payload());

        assertThat(payload.get("eventId"))
                .isNotNull();

        assertThat(payload.get("portfolioId").asText())
                .isEqualTo(
                        portfolioId.toString()
                );

        assertThat(payload.get("assetId").asText())
                .isEqualTo(
                        assetId.toString()
                );

        assertThat(payload.get("quantity").asText())
                .isEqualTo("10");

        assertThat(payload.get("averageCost").asText())
                .isEqualTo("105.50");

        assertThat(payload.get("currency").asText())
                .isEqualTo("USD");

        assertThat(payload.get("executionSequence").asLong())
                .isEqualTo(101L);

        assertThat(payload.get("occurredAt").asText())
                .isEqualTo(
                        updatedAt.toString()
                );
    }

    @Test
    void shouldCreateZeroQuantityPositionChangedEventWhenPositionIsClosed() {
        Instant occurredAt = Instant.parse(
                "2026-10-02T10:00:00Z"
        );

        Position position = new Position(
                UUID.randomUUID(),
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                "USD",
                Instant.parse(
                        "2026-10-02T09:00:00Z"
                ),
                100L
        );

        OutboxEvent result =
                factory.createClosed(
                        position,
                        occurredAt,
                        101L
                );

        assertThat(result.id())
                .isNotNull();

        assertThat(result.aggregateId())
                .isEqualTo(portfolioId);

        assertThat(result.eventType())
                .isEqualTo("PositionChanged");

        assertThat(result.status())
                .isEqualTo(OutboxEventStatus.PENDING);

        assertThat(result.occurredAt())
                .isEqualTo(occurredAt);

        JsonNode payload =
                readPayload(result.payload());

        assertThat(payload.get("eventId"))
                .isNotNull();

        assertThat(payload.get("portfolioId").asText())
                .isEqualTo(
                        portfolioId.toString()
                );

        assertThat(payload.get("assetId").asText())
                .isEqualTo(
                        assetId.toString()
                );

        assertThat(payload.get("quantity").asText())
                .isEqualTo("0");

        assertThat(payload.get("averageCost").asText())
                .isEqualTo("0");

        assertThat(payload.get("currency").asText())
                .isEqualTo("USD");

        assertThat(payload.get("executionSequence").asLong())
                .isEqualTo(101L);

        assertThat(payload.get("occurredAt").asText())
                .isEqualTo(
                        occurredAt.toString()
                );
    }

    @Test
    void shouldUsePositionUpdatedAtAsOccurredAtForNormalChange() {
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

        OutboxEvent result =
                factory.create(position);

        assertThat(result.occurredAt())
                .isEqualTo(updatedAt);
    }

    @Test
    void shouldUseProvidedOccurredAtForClosedPosition() {
        Instant positionUpdatedAt = Instant.parse(
                "2026-10-02T09:00:00Z"
        );

        Instant occurredAt = Instant.parse(
                "2026-10-02T10:00:00Z"
        );

        Position position = new Position(
                UUID.randomUUID(),
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                "USD",
                positionUpdatedAt,
                100L
        );

        OutboxEvent result =
                factory.createClosed(
                        position,
                        occurredAt,
                        101L
                );

        assertThat(result.occurredAt())
                .isEqualTo(occurredAt);

        JsonNode payload =
                readPayload(result.payload());

        assertThat(payload.get("occurredAt").asText())
                .isEqualTo(
                        occurredAt.toString()
                );
    }

    @Test
    void shouldUseProvidedExecutionSequenceForClosedPosition() {
        Position position = new Position(
                UUID.randomUUID(),
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                "USD",
                Instant.parse(
                        "2026-10-02T09:00:00Z"
                ),
                100L
        );

        OutboxEvent result =
                factory.createClosed(
                        position,
                        Instant.parse(
                                "2026-10-02T10:00:00Z"
                        ),
                        101L
                );

        JsonNode payload =
                readPayload(result.payload());

        assertThat(payload.get("executionSequence").asLong())
                .isEqualTo(101L);
    }

    private JsonNode readPayload(String payload) {
        try {
            return new ObjectMapper()
                    .readTree(payload);
        } catch (Exception exception) {
            throw new AssertionError(
                    "Invalid JSON payload",
                    exception
            );
        }
    }
}