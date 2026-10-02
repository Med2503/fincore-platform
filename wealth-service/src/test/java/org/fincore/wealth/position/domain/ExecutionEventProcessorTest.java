package org.fincore.wealth.position.domain;

import org.fincore.wealth.portfolio.application.port.PortfolioPositionLock;
import org.fincore.wealth.position.application.port.PositionRepository;
import org.fincore.wealth.position.application.port.ProcessedEventRepository;
import org.fincore.wealth.position.domain.ExecutionEvent;
import org.fincore.wealth.position.domain.Position;
import org.fincore.wealth.position.service.ExecutionEventProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionEventProcessorTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private ProcessedEventRepository processedEvents;

    @Mock
    private PortfolioPositionLock portfolioPositionLock;

    private ExecutionEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ExecutionEventProcessor(
                positionRepository,
                processedEvents,
                portfolioPositionLock
        );
    }

    @Test
    void shouldReturnDuplicateWithoutLockingPortfolio() {
        ExecutionEvent event = buyEvent();

        when(processedEvents.claim(event.eventId()))
                .thenReturn(false);

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertEquals(
                ExecutionEventProcessor.ProcessingResult.DUPLICATE,
                result
        );

        verify(processedEvents).claim(event.eventId());
        verifyNoInteractions(portfolioPositionLock);
        verifyNoInteractions(positionRepository);
    }

    @Test
    void shouldLockPortfolioBeforeReadingPosition() {
        ExecutionEvent event = buyEvent();

        when(processedEvents.claim(event.eventId()))
                .thenReturn(true);

        when(positionRepository.findForUpdate(
                event.portfolioId(),
                event.assetId()
        )).thenReturn(Optional.empty());

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertEquals(
                ExecutionEventProcessor.ProcessingResult.APPLIED,
                result
        );

        InOrder order = inOrder(
                processedEvents,
                portfolioPositionLock,
                positionRepository
        );

        order.verify(processedEvents).claim(event.eventId());

        order.verify(portfolioPositionLock)
                .lockForPositionUpdate(event.portfolioId());

        order.verify(positionRepository)
                .findForUpdate(
                        event.portfolioId(),
                        event.assetId()
                );

        verify(positionRepository).save(any(Position.class));
    }

    @Test
    void shouldNotLockPortfolioWhenEventIsDuplicate() {
        ExecutionEvent event = buyEvent();

        when(processedEvents.claim(event.eventId()))
                .thenReturn(false);

        processor.process(event);

        verify(portfolioPositionLock, never())
                .lockForPositionUpdate(any());

        verify(positionRepository, never())
                .findForUpdate(any(), any());
    }

    @Test
    void shouldNotApplyPositionUpdateWhenPortfolioLockFails() {
        ExecutionEvent event = buyEvent();

        when(processedEvents.claim(event.eventId()))
                .thenReturn(true);

        doThrow(new IllegalStateException("Portfolio lock failed"))
                .when(portfolioPositionLock)
                .lockForPositionUpdate(event.portfolioId());

        assertThrows(
                IllegalStateException.class,
                () -> processor.process(event)
        );

        verify(positionRepository, never())
                .findForUpdate(any(), any());

        verify(positionRepository, never())
                .save(any());
    }

    private ExecutionEvent buyEvent() {
        return new ExecutionEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ExecutionEvent.Side.BUY,
                new BigDecimal("10"),
                new BigDecimal("125.50"),
                new BigDecimal("2.00"),
                "USD",
                Instant.parse("2026-10-02T10:00:00Z")
        );
    }
}