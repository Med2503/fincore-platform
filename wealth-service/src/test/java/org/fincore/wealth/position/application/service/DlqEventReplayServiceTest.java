package org.fincore.wealth.position.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fincore.wealth.position.application.port.DlqReplayAttemptRepository;
import org.fincore.wealth.position.application.port.DlqReplayOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DlqEventReplayServiceTest {

    private DlqReplayAttemptRepository attempts;
    private DlqReplayOutboxRepository outbox;
    private DlqEventReplayService service;
    private Clock clock;

    @BeforeEach
    void setUp() {
        attempts = mock(
                DlqReplayAttemptRepository.class
        );

        outbox = mock(
                DlqReplayOutboxRepository.class
        );

        clock = Clock.fixed(
                Instant.parse(
                        "2026-09-30T10:00:00Z"
                ),
                ZoneOffset.UTC
        );

        service = new DlqEventReplayService(
                attempts,
                outbox,
                new ObjectMapper(),
                clock
        );
    }

    @Test
    void shouldCreateReplayOutboxWithPortfolioAsAggregate() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        String payload =
                payload(
                        eventId,
                        portfolioId
                );

        when(
                attempts.claimNextReplay(
                        eq(eventId),
                        any(Instant.class),
                        eq(3)
                )
        ).thenReturn(1);

        DlqReplayResult result =
                service.replay(
                        eventId,
                        payload,
                        3
                );

        assertEquals(
                DlqReplayResult.REPLAYED,
                result
        );

        verify(outbox).save(
                eq(eventId),
                eq(portfolioId),
                eq(payload),
                eq(
                        Instant.parse(
                                "2026-09-30T10:00:00Z"
                        )
                )
        );
    }

    @Test
    void shouldRejectWhenMaximumReplayReached() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        when(
                attempts.claimNextReplay(
                        eq(eventId),
                        any(Instant.class),
                        eq(3)
                )
        ).thenReturn(0);

        DlqReplayResult result =
                service.replay(
                        eventId,
                        payload(
                                eventId,
                                portfolioId
                        ),
                        3
                );

        assertEquals(
                DlqReplayResult.REJECTED,
                result
        );

        verifyNoInteractions(outbox);
    }

    @Test
    void shouldPropagateOutboxFailure() {
        UUID eventId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        String payload =
                payload(
                        eventId,
                        portfolioId
                );

        when(
                attempts.claimNextReplay(
                        eq(eventId),
                        any(Instant.class),
                        eq(3)
                )
        ).thenReturn(1);

        doThrow(
                new IllegalStateException(
                        "Outbox persistence failure"
                )
        ).when(outbox).save(
                eq(eventId),
                eq(portfolioId),
                eq(payload),
                any(Instant.class)
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.replay(
                        eventId,
                        payload,
                        3
                )
        );
    }

    @Test
    void shouldRejectPayloadWithoutPortfolioId() {
        UUID eventId = UUID.randomUUID();

        String payload = """
                {
                  "eventId": "%s"
                }
                """.formatted(eventId);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.replay(
                        eventId,
                        payload,
                        3
                )
        );

        verifyNoInteractions(attempts);
        verifyNoInteractions(outbox);
    }

    @Test
    void shouldRejectInvalidPortfolioId() {
        UUID eventId = UUID.randomUUID();

        String payload = """
                {
                  "eventId": "%s",
                  "portfolioId": "invalid"
                }
                """.formatted(eventId);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.replay(
                        eventId,
                        payload,
                        3
                )
        );

        verifyNoInteractions(attempts);
        verifyNoInteractions(outbox);
    }

    @Test
    void shouldRejectBlankPayload() {
        UUID eventId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.replay(
                        eventId,
                        " ",
                        3
                )
        );

        verifyNoInteractions(attempts);
        verifyNoInteractions(outbox);
    }

    @Test
    void shouldRejectNullEventId() {
        assertThrows(
                NullPointerException.class,
                () -> service.replay(
                        null,
                        "payload",
                        3
                )
        );

        verifyNoInteractions(attempts);
        verifyNoInteractions(outbox);
    }

    @Test
    void shouldRejectNullPayload() {
        assertThrows(
                NullPointerException.class,
                () -> service.replay(
                        UUID.randomUUID(),
                        null,
                        3
                )
        );

        verifyNoInteractions(attempts);
        verifyNoInteractions(outbox);
    }

    @Test
    void shouldRejectInvalidMaximumReplay() {
        UUID eventId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.replay(
                        eventId,
                        "payload",
                        0
                )
        );

        verifyNoInteractions(attempts);
        verifyNoInteractions(outbox);
    }

    private String payload(
            UUID eventId,
            UUID portfolioId
    ) {
        return """
                {
                  "eventId": "%s",
                  "portfolioId": "%s",
                  "assetId": "%s",
                  "quantity": 10,
                  "averageCost": 100,
                  "currency": "EUR",
                  "executionSequence": 1,
                  "occurredAt": "2026-09-30T10:00:00Z"
                }
                """.formatted(
                eventId,
                portfolioId,
                UUID.randomUUID()
        );
    }
}