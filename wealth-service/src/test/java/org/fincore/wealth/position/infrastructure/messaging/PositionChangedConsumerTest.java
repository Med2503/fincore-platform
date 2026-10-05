package org.fincore.wealth.position.infrastructure.messaging;

import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.fincore.wealth.position.application.port.ProcessedPositionChangedEventRepository;
import org.fincore.wealth.position.application.service.PositionChangedProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionChangedConsumerTest {

    @Mock
    private ProcessedPositionChangedEventRepository processedEvents;

    @Mock
    private PositionChangedProjectionService projectionService;

    private PositionChangedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PositionChangedConsumer(
                processedEvents,
                projectionService
        );
    }

    @Test
    void shouldProcessNewEvent() {
        PositionChangedEvent event = validEvent();

        when(processedEvents.claim(event.eventId()))
                .thenReturn(true);

        consumer.consume(event);

        verify(processedEvents)
                .claim(event.eventId());

        verify(projectionService)
                .apply(event);
    }

    @Test
    void shouldIgnoreDuplicateEvent() {
        PositionChangedEvent event = validEvent();

        when(processedEvents.claim(event.eventId()))
                .thenReturn(false);

        consumer.consume(event);

        verify(processedEvents)
                .claim(event.eventId());

        verifyNoInteractions(projectionService);
    }

    @Test
    void shouldNotClaimInvalidEvent() {
        PositionChangedEvent event =
                new PositionChangedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("-1"),
                        BigDecimal.TEN,
                        "EUR",
                        1,
                        Instant.now()
                );

        org.assertj.core.api.Assertions
                .assertThatThrownBy(
                        () -> consumer.consume(event)
                )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Quantity cannot be negative"
                );

        verifyNoInteractions(processedEvents);
        verifyNoInteractions(projectionService);
    }

    @Test
    void shouldProcessClosedPosition() {
        PositionChangedEvent event =
                new PositionChangedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "EUR",
                        10,
                        Instant.parse(
                                "2026-10-05T10:00:00Z"
                        )
                );

        when(processedEvents.claim(event.eventId()))
                .thenReturn(true);

        consumer.consume(event);

        verify(projectionService)
                .apply(event);
    }

    private PositionChangedEvent validEvent() {
        return new PositionChangedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.TEN,
                new BigDecimal("100.00"),
                "EUR",
                1,
                Instant.parse(
                        "2026-10-05T10:00:00Z"
                )
        );
    }
}