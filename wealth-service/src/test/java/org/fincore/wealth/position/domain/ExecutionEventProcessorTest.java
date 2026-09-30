package org.fincore.wealth.position.domain;

import org.fincore.wealth.position.application.port.ExecutionEventProcessor;
import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.application.port.ProcessedEventRepository;
import org.fincore.wealth.position.domain.ExecutionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutionEventProcessorTest {

    private PositionRepository positions;
    private ProcessedEventRepository processedEvents;
    private ExecutionEventProcessor processor;

    private final UUID portfolioId = UUID.randomUUID();
    private final UUID assetId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-30T10:00:00Z");

    @BeforeEach
    void setUp() {
        positions = mock(PositionRepository.class);
        processedEvents = mock(ProcessedEventRepository.class);
        processor = new ExecutionEventProcessor(positions, processedEvents);
    }

    @Test
    void duplicateEventIsIgnored() {
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(true);

        assertEquals(
                ExecutionEventProcessor.ProcessingResult.DUPLICATE,
                processor.process(event(ExecutionEvent.Side.BUY, "2", "100"))
        );

        verifyNoInteractions(positions);
        verify(processedEvents, never()).markProcessed(any());
    }

    @Test
    void buyCreatesPositionWhenAbsent() {
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);
        when(positions.findForUpdate(portfolioId, assetId))
                .thenReturn(Optional.empty());

        processor.process(event(ExecutionEvent.Side.BUY, "2", "100"));

        verify(positions).save(argThat(position ->
                position.quantity().compareTo(new BigDecimal("2")) == 0
                        && position.averageCost()
                        .compareTo(new BigDecimal("100")) == 0
        ));
        verify(processedEvents).markProcessed(eventId);
    }

    @Test
    void sellWithoutPositionFailsAndIsNotMarkedProcessed() {
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);
        when(positions.findForUpdate(portfolioId, assetId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> processor.process(
                        event(ExecutionEvent.Side.SELL, "1", "110")
                )
        );

        verify(processedEvents, never()).markProcessed(eventId);
    }

    private ExecutionEvent event(
            ExecutionEvent.Side side,
            String quantity,
            String price
    ) {
        return new ExecutionEvent(
                eventId,
                UUID.randomUUID(),
                portfolioId,
                assetId,
                side,
                new BigDecimal(quantity),
                new BigDecimal(price),
                BigDecimal.ZERO,
                "USD",
                now
        );
    }
}