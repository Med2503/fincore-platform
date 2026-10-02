package org.fincore.wealth.position.domain;


import org.fincore.wealth.position.application.port.ExecutionEventProcessor;
import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.application.port.ProcessedEventRepository;
import org.fincore.wealth.position.domain.ExecutionEvent;
import org.fincore.wealth.position.domain.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExecutionEventProcessorTest {

    private PositionRepository positions;
    private ProcessedEventRepository processedEvents;
    private ExecutionEventProcessor processor;

    private final UUID portfolioId = UUID.randomUUID();
    private final UUID assetId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-02T10:00:00Z");

    @BeforeEach
    void setUp() {
        positions = mock(PositionRepository.class);
        processedEvents = mock(ProcessedEventRepository.class);
        processor = new ExecutionEventProcessor(positions, processedEvents);
    }

    @Test
    void duplicateEventDoesNotReadOrModifyPosition() {
        when(processedEvents.claim(eventId)).thenReturn(false);

        var result = processor.process(
                event(ExecutionEvent.Side.BUY, "2", "100")
        );

        assertEquals(
                ExecutionEventProcessor.ProcessingResult.DUPLICATE,
                result
        );

        verify(processedEvents).claim(eventId);
        verifyNoInteractions(positions);
    }

    @Test
    void buyCreatesPositionWhenAbsent() {
        when(processedEvents.claim(eventId)).thenReturn(true);
        when(positions.findForUpdate(portfolioId, assetId))
                .thenReturn(Optional.empty());

        var result = processor.process(
                event(ExecutionEvent.Side.BUY, "2", "100")
        );

        assertEquals(
                ExecutionEventProcessor.ProcessingResult.APPLIED,
                result
        );

        verify(positions).save(argThat(position ->
                position.portfolioId().equals(portfolioId)
                        && position.assetId().equals(assetId)
                        && position.quantity()
                        .compareTo(new BigDecimal("2")) == 0
                        && position.averageCost()
                        .compareTo(new BigDecimal("100")) == 0
        ));
    }

    @Test
    void buyUpdatesExistingPosition() {
        Position existing = Position.open(
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                "USD",
                now
        );

        when(processedEvents.claim(eventId)).thenReturn(true);
        when(positions.findForUpdate(portfolioId, assetId))
                .thenReturn(Optional.of(existing));

        processor.process(event(ExecutionEvent.Side.BUY, "10", "120"));

        verify(positions).save(argThat(position ->
                position.quantity().compareTo(new BigDecimal("20")) == 0
                        && position.averageCost()
                        .compareTo(new BigDecimal("110")) == 0
        ));
    }

    @Test
    void partialSaleSavesRemainingPosition() {
        Position existing = Position.open(
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                "USD",
                now
        );

        when(processedEvents.claim(eventId)).thenReturn(true);
        when(positions.findForUpdate(portfolioId, assetId))
                .thenReturn(Optional.of(existing));

        processor.process(event(ExecutionEvent.Side.SELL, "4", "130"));

        verify(positions).save(argThat(position ->
                position.quantity().compareTo(new BigDecimal("6")) == 0
        ));
        verify(positions, never()).delete(any());
    }

    @Test
    void fullSaleDeletesPosition() {
        Position existing = Position.open(
                portfolioId,
                assetId,
                new BigDecimal("5"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                "USD",
                now
        );

        when(processedEvents.claim(eventId)).thenReturn(true);
        when(positions.findForUpdate(portfolioId, assetId))
                .thenReturn(Optional.of(existing));

        processor.process(event(ExecutionEvent.Side.SELL, "5", "130"));

        verify(positions).delete(existing);
        verify(positions, never()).save(any());
    }

    @Test
    void saleWithoutPositionFails() {
        when(processedEvents.claim(eventId)).thenReturn(true);
        when(positions.findForUpdate(portfolioId, assetId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> processor.process(
                        event(ExecutionEvent.Side.SELL, "1", "110")
                )
        );

        verify(positions, never()).save(any());
        verify(positions, never()).delete(any());
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