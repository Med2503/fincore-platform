package org.fincore.wealth.position.application.service;

import org.fincore.wealth.position.application.port.DlqReplayAttemptRepository;
import org.fincore.wealth.position.application.port.PositionChangedEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DlqEventReplayServiceTest {

    private DlqReplayAttemptRepository attempts;

    private PositionChangedEventPublisher publisher;

    private DlqEventReplayService service;

    @BeforeEach
    void setUp() {
        attempts = mock(
                DlqReplayAttemptRepository.class
        );

        publisher = mock(
                PositionChangedEventPublisher.class
        );

        Clock clock =
                Clock.fixed(
                        Instant.parse(
                                "2026-09-30T10:00:00Z"
                        ),
                        ZoneOffset.UTC
                );

        service = new DlqEventReplayService(
                publisher,
                attempts,
                clock
        );
    }

    @Test
    void shouldReplayWhenAttemptIsClaimed() {

        UUID eventId = UUID.randomUUID();

        when(
                attempts.claimNextReplay(
                        eq(eventId),
                        any(),
                        eq(3)
                )
        ).thenReturn(1);

        when(
                publisher.publish("payload")
        ).thenReturn(true);

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
                publisher
        ).publish("payload");
    }

    @Test
    void shouldRejectWhenMaximumReplayReached() {

        UUID eventId = UUID.randomUUID();

        when(
                attempts.claimNextReplay(
                        eq(eventId),
                        any(),
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

        verifyNoInteractions(publisher);
    }

    @Test
    void shouldRollbackWhenPublicationFails() {

        UUID eventId = UUID.randomUUID();

        when(
                attempts.claimNextReplay(
                        eq(eventId),
                        any(),
                        eq(3)
                )
        ).thenReturn(1);

        when(
                publisher.publish("payload")
        ).thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> service.replay(
                        eventId,
                        "payload",
                        3
                )
        );

        verify(
                publisher
        ).publish("payload");
    }

    @Test
    void shouldRejectBlankPayload() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.replay(
                        UUID.randomUUID(),
                        " ",
                        3
                )
        );

        verifyNoInteractions(attempts);
        verifyNoInteractions(publisher);
    }
}