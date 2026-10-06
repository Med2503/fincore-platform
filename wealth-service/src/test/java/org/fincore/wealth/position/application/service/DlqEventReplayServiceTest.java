package org.fincore.wealth.position.application.service;

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
                clock
        );
    }

    @Test
    void shouldCreateReplayOutboxWhenAttemptIsClaimed() {

        UUID eventId = UUID.randomUUID();

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
                        "payload",
                        3
                );

        assertEquals(
                DlqReplayResult.REPLAYED,
                result
        );

        verify(
                attempts
        ).claimNextReplay(
                eq(eventId),
                eq(Instant.parse(
                        "2026-09-30T10:00:00Z"
                )),
                eq(3)
        );

        verify(
                outbox
        ).save(
                eq(eventId),
                eq("payload"),
                eq(Instant.parse(
                        "2026-09-30T10:00:00Z"
                ))
        );
    }

    @Test
    void shouldRejectWhenMaximumReplayReached() {

        UUID eventId = UUID.randomUUID();

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
                        "payload",
                        3
                );

        assertEquals(
                DlqReplayResult.REJECTED,
                result
        );

        verify(
                attempts
        ).claimNextReplay(
                eq(eventId),
                any(Instant.class),
                eq(3)
        );

        verifyNoInteractions(outbox);
    }

    @Test
    void shouldPropagateOutboxFailure() {

        UUID eventId = UUID.randomUUID();

        when(
                attempts.claimNextReplay(
                        eq(eventId),
                        any(Instant.class),
                        eq(3)
                )
        ).thenReturn(1);

        org.mockito.Mockito.doThrow(
                new IllegalStateException(
                        "Outbox persistence failure"
                )
        ).when(outbox).save(
                eq(eventId),
                eq("payload"),
                any(Instant.class)
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.replay(
                        eventId,
                        "payload",
                        3
                )
        );

        verify(
                attempts
        ).claimNextReplay(
                eq(eventId),
                any(Instant.class),
                eq(3)
        );

        verify(
                outbox
        ).save(
                eq(eventId),
                eq("payload"),
                any(Instant.class)
        );
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
}