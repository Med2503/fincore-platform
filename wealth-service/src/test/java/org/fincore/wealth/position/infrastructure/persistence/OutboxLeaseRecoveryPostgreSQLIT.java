
package org.fincore.wealth.position.infrastructure.persistence;

import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.outbox.domain.OutboxEventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class OutboxLeaseRecoveryPostgreSQLIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("wealth_test")
                    .withUsername("wealth")
                    .withPassword("wealth");

    private static final Instant NOW =
            Instant.parse("2026-09-30T10:00:00Z");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.cloud.stream.enabled", () -> false);
    }

    @Autowired
    private OutboxEventRepository repository;

    @Test
    void shouldRecoverEventAfterLeaseExpiration() {
        UUID eventId = savePendingEvent();

        UUID firstWorker = UUID.randomUUID();

        List<OutboxEvent> firstClaim = repository.claimBatch(
                NOW, NOW.plusSeconds(30), firstWorker, 10);

        assertEquals(1, firstClaim.size());
        assertEquals(eventId, firstClaim.get(0).id());
        assertEquals(OutboxEventStatus.PROCESSING,
                firstClaim.get(0).status());
        assertEquals(firstWorker, firstClaim.get(0).claimedBy());

        List<OutboxEvent> recovered = repository.claimBatch(
                NOW.plusSeconds(31),
                NOW.plusSeconds(61),
                UUID.randomUUID(),
                10);

        assertEquals(1, recovered.size());
        assertEquals(eventId, recovered.get(0).id());
        assertEquals(OutboxEventStatus.PROCESSING,
                recovered.get(0).status());
        assertEquals(NOW.plusSeconds(61),
                recovered.get(0).lockedUntil());
    }

    @Test
    void shouldNotRecoverEventWhileLeaseIsActive() {
        UUID eventId = savePendingEvent();

        repository.claimBatch(
                NOW, NOW.plusSeconds(30), UUID.randomUUID(), 10);

        List<OutboxEvent> secondClaim = repository.claimBatch(
                NOW.plusSeconds(15),
                NOW.plusSeconds(45),
                UUID.randomUUID(),
                10);

        assertTrue(secondClaim.stream()
                .noneMatch(event -> event.id().equals(eventId)));
    }

    @Test
    void shouldNotClaimEventBeforeNextAttemptTime() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        repository.save(OutboxEvent.pending(
                eventId, aggregateId, "PositionChanged", "{}", NOW));

        repository.update(new OutboxEvent(
                eventId, aggregateId, "PositionChanged", "{}", NOW,
                null, OutboxEventStatus.PENDING, 1,
                NOW.plusSeconds(60), null, null));

        List<OutboxEvent> events = repository.claimBatch(
                NOW, NOW.plusSeconds(30), UUID.randomUUID(), 10);

        assertTrue(events.stream()
                .noneMatch(event -> event.id().equals(eventId)));
    }


    private UUID savePendingEvent() {
        UUID id = UUID.randomUUID();

        repository.save(OutboxEvent.pending(
                id,
                UUID.randomUUID(),
                "PositionChanged",
                "{}",
                NOW));

        return id;
    }

    private UUID findAggregateId(UUID eventId) {
        // Replaced in the next step by retaining the aggregate ID locally.
        throw new UnsupportedOperationException(
                "Retain the aggregate ID in the test setup");
    }
}
