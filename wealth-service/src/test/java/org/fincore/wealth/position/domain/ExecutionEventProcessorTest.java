package org.fincore.wealth.position.domain;

import org.fincore.wealth.portfolio.application.port.PortfolioPositionLock;
import org.fincore.wealth.position.application.event.PositionOutboxEventFactory;
import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.application.port.ProcessedEventRepository;
import org.fincore.wealth.position.domain.ExecutionEvent;
import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.outbox.application.port.OutboxEventRepository;
import org.fincore.wealth.position.outbox.domain.OutboxEvent;
import org.fincore.wealth.position.service.ExecutionEventProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExecutionEventProcessorTest {

    private PositionRepository positionRepository;
    private ProcessedEventRepository processedEvents;
    private PortfolioPositionLock portfolioPositionLock;
    private OutboxEventRepository outboxEvents;
    private PositionOutboxEventFactory outboxEventFactory;

    private ExecutionEventProcessor processor;

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-30T10:00:00Z"
            );

    @BeforeEach
    void setUp() {
        positionRepository =
                mock(PositionRepository.class);

        processedEvents =
                mock(ProcessedEventRepository.class);

        portfolioPositionLock =
                mock(PortfolioPositionLock.class);

        outboxEvents =
                mock(OutboxEventRepository.class);

        outboxEventFactory =
                mock(PositionOutboxEventFactory.class);

        processor =
                new ExecutionEventProcessor(
                        positionRepository,
                        processedEvents,
                        portfolioPositionLock,
                        outboxEvents,
                        outboxEventFactory
                );
    }

    @Test
    void shouldOpenPositionOnFirstBuy() {
        ExecutionEvent event =
                buyEvent(
                        UUID.randomUUID(),
                        1
                );

        Position position =
                position(
                        event.portfolioId(),
                        event.assetId(),
                        "10",
                        "100",
                        1
                );

        OutboxEvent outbox =
                outboxEvent(
                        event,
                        position
                );

        when(
                processedEvents.claim(
                        event.eventId()
                )
        ).thenReturn(true);

        when(
                positionRepository.findForUpdate(
                        event.portfolioId(),
                        event.assetId()
                )
        ).thenReturn(Optional.empty());

        when(
                positionRepository.save(
                        any(Position.class)
                )
        ).thenReturn(position);

        when(
                outboxEventFactory.create(
                        eq(position),
                        eq(1L),
                        eq(NOW)
                )
        ).thenReturn(outbox);

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertEquals(
                ExecutionEventProcessor.ProcessingResult.APPLIED,
                result
        );

        verify(
                portfolioPositionLock
        ).lockForPositionUpdate(
                event.portfolioId()
        );

        verify(
                positionRepository
        ).save(
                any(Position.class)
        );

        verify(
                outboxEvents
        ).save(outbox);
    }

    @Test
    void shouldIncreaseExistingPositionOnBuy() {
        ExecutionEvent event =
                buyEvent(
                        UUID.randomUUID(),
                        2
                );

        Position current =
                position(
                        event.portfolioId(),
                        event.assetId(),
                        "10",
                        "100",
                        1
                );

        Position updated =
                position(
                        event.portfolioId(),
                        event.assetId(),
                        "20",
                        "100",
                        2
                );

        OutboxEvent outbox =
                outboxEvent(
                        event,
                        updated
                );

        when(
                processedEvents.claim(
                        event.eventId()
                )
        ).thenReturn(true);

        when(
                positionRepository.findForUpdate(
                        event.portfolioId(),
                        event.assetId()
                )
        ).thenReturn(Optional.of(current));

        when(
                positionRepository.save(
                        any(Position.class)
                )
        ).thenReturn(updated);

        when(
                outboxEventFactory.create(
                        eq(updated),
                        eq(2L),
                        eq(NOW)
                )
        ).thenReturn(outbox);

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertEquals(
                ExecutionEventProcessor.ProcessingResult.APPLIED,
                result
        );

        verify(
                positionRepository
        ).save(
                any(Position.class)
        );

        verify(
                outboxEvents
        ).save(outbox);
    }

    @Test
    void shouldReducePositionOnPartialSell() {
        ExecutionEvent event =
                sellEvent(
                        UUID.randomUUID(),
                        2,
                        "4"
                );

        Position current =
                position(
                        event.portfolioId(),
                        event.assetId(),
                        "10",
                        "100",
                        1
                );

        Position remaining =
                position(
                        event.portfolioId(),
                        event.assetId(),
                        "6",
                        "100",
                        2
                );

        OutboxEvent outbox =
                outboxEvent(
                        event,
                        remaining
                );

        when(
                processedEvents.claim(
                        event.eventId()
                )
        ).thenReturn(true);

        when(
                positionRepository.findForUpdate(
                        event.portfolioId(),
                        event.assetId()
                )
        ).thenReturn(Optional.of(current));

        when(
                positionRepository.save(
                        any(Position.class)
                )
        ).thenReturn(remaining);

        when(
                outboxEventFactory.create(
                        eq(remaining),
                        eq(2L),
                        eq(NOW)
                )
        ).thenReturn(outbox);

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertEquals(
                ExecutionEventProcessor.ProcessingResult.APPLIED,
                result
        );

        verify(
                positionRepository
        ).save(
                any(Position.class)
        );

        verify(
                positionRepository,
                never()
        ).delete(
                any(Position.class)
        );

        verify(
                outboxEvents
        ).save(outbox);
    }

    @Test
    void shouldDeletePositionAndCreateTombstoneOnFullSell() {
        ExecutionEvent event =
                sellEvent(
                        UUID.randomUUID(),
                        2,
                        "10"
                );

        Position current =
                position(
                        event.portfolioId(),
                        event.assetId(),
                        "10",
                        "100",
                        1
                );

        OutboxEvent outbox =
                mock(OutboxEvent.class);

        when(
                processedEvents.claim(
                        event.eventId()
                )
        ).thenReturn(true);

        when(
                positionRepository.findForUpdate(
                        event.portfolioId(),
                        event.assetId()
                )
        ).thenReturn(Optional.of(current));

        when(
                outboxEventFactory.createClosed(
                        event.portfolioId(),
                        event.assetId(),
                        event.currency(),
                        event.executionSequence(),
                        event.occurredAt()
                )
        ).thenReturn(outbox);

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertEquals(
                ExecutionEventProcessor.ProcessingResult.APPLIED,
                result
        );

        verify(
                positionRepository
        ).delete(current);

        verify(
                positionRepository,
                never()
        ).save(
                any(Position.class)
        );

        verify(
                outboxEventFactory
        ).createClosed(
                event.portfolioId(),
                event.assetId(),
                event.currency(),
                event.executionSequence(),
                event.occurredAt()
        );

        verify(
                outboxEvents
        ).save(outbox);
    }

    @Test
    void shouldReturnDuplicateWithoutProcessing() {
        ExecutionEvent event =
                buyEvent(
                        UUID.randomUUID(),
                        1
                );

        when(
                processedEvents.claim(
                        event.eventId()
                )
        ).thenReturn(false);

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertEquals(
                ExecutionEventProcessor.ProcessingResult.DUPLICATE,
                result
        );

        verifyNoInteractions(
                portfolioPositionLock
        );

        verifyNoInteractions(
                positionRepository
        );

        verifyNoInteractions(
                outboxEvents
        );

        verifyNoInteractions(
                outboxEventFactory
        );
    }

    @Test
    void shouldReturnOutOfOrderWhenSequenceIsOlder() {
        ExecutionEvent event =
                buyEvent(
                        UUID.randomUUID(),
                        2
                );

        Position current =
                position(
                        event.portfolioId(),
                        event.assetId(),
                        "10",
                        "100",
                        5
                );

        when(
                processedEvents.claim(
                        event.eventId()
                )
        ).thenReturn(true);

        when(
                positionRepository.findForUpdate(
                        event.portfolioId(),
                        event.assetId()
                )
        ).thenReturn(Optional.of(current));

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertEquals(
                ExecutionEventProcessor.ProcessingResult.OUT_OF_ORDER,
                result
        );

        verify(
                positionRepository,
                never()
        ).save(
                any(Position.class)
        );

        verify(
                positionRepository,
                never()
        ).delete(
                any(Position.class)
        );

        verifyNoInteractions(
                outboxEvents
        );

        verifyNoInteractions(
                outboxEventFactory
        );
    }

    @Test
    void shouldRejectSellWithoutPosition() {
        ExecutionEvent event =
                sellEvent(
                        UUID.randomUUID(),
                        1,
                        "10"
                );

        when(
                processedEvents.claim(
                        event.eventId()
                )
        ).thenReturn(true);

        when(
                positionRepository.findForUpdate(
                        event.portfolioId(),
                        event.assetId()
                )
        ).thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> processor.process(event)
        );

        verify(
                positionRepository,
                never()
        ).save(
                any(Position.class)
        );

        verifyNoInteractions(
                outboxEvents
        );
    }

    @Test
    void shouldRejectNullEvent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(null)
        );

        verifyNoInteractions(
                processedEvents
        );

        verifyNoInteractions(
                portfolioPositionLock
        );
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        ExecutionEvent event =
                new ExecutionEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ExecutionEvent.Side.BUY,
                        BigDecimal.ZERO,
                        new BigDecimal("100"),
                        BigDecimal.ZERO,
                        "EUR",
                        NOW,
                        1
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(event)
        );

        verifyNoInteractions(
                processedEvents
        );
    }

    @Test
    void shouldRejectNegativePrice() {
        ExecutionEvent event =
                new ExecutionEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ExecutionEvent.Side.BUY,
                        new BigDecimal("10"),
                        new BigDecimal("-1"),
                        BigDecimal.ZERO,
                        "EUR",
                        NOW,
                        1
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(event)
        );

        verifyNoInteractions(
                processedEvents
        );
    }

    @Test
    void shouldRejectNegativeFees() {
        ExecutionEvent event =
                new ExecutionEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ExecutionEvent.Side.BUY,
                        new BigDecimal("10"),
                        new BigDecimal("100"),
                        new BigDecimal("-1"),
                        "EUR",
                        NOW,
                        1
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(event)
        );

        verifyNoInteractions(
                processedEvents
        );
    }

    @Test
    void shouldRejectInvalidSequence() {
        ExecutionEvent event =
                new ExecutionEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ExecutionEvent.Side.BUY,
                        new BigDecimal("10"),
                        new BigDecimal("100"),
                        BigDecimal.ZERO,
                        "EUR",
                        NOW,
                        0
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(event)
        );

        verifyNoInteractions(
                processedEvents
        );
    }

    private ExecutionEvent buyEvent(
            UUID eventId,
            long sequence
    ) {
        return new ExecutionEvent(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ExecutionEvent.Side.BUY,
                new BigDecimal("10"),
                new BigDecimal("100"),
                new BigDecimal("2"),
                "EUR",
                NOW,
                sequence
        );
    }

    private ExecutionEvent sellEvent(
            UUID eventId,
            long sequence,
            String quantity
    ) {
        return new ExecutionEvent(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ExecutionEvent.Side.SELL,
                new BigDecimal(quantity),
                new BigDecimal("120"),
                new BigDecimal("2"),
                "EUR",
                NOW,
                sequence
        );
    }

    private Position position(
            UUID portfolioId,
            UUID assetId,
            String quantity,
            String averageCost,
            long sequence
    ) {
        return new Position(
                UUID.randomUUID(),
                portfolioId,
                assetId,
                new BigDecimal(quantity),
                new BigDecimal(averageCost),
                "EUR",
                NOW,
                sequence
        );
    }

    private OutboxEvent outboxEvent(
            ExecutionEvent event,
            Position position
    ) {
        return OutboxEvent.pending(
                event.eventId(),
                position.portfolioId(),
                "PositionChanged",
                "{}",
                event.occurredAt()
        );
    }
}