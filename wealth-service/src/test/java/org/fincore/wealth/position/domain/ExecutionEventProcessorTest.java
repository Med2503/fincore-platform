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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private UUID portfolioId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        processor = new ExecutionEventProcessor(
                positionRepository,
                processedEvents,
                portfolioPositionLock
        );

        portfolioId = UUID.randomUUID();
        assetId = UUID.randomUUID();
    }

    @Test
    void shouldApplyFirstExecutionSequence() {
        ExecutionEvent event = buyEvent(
                UUID.randomUUID(),
                100L,
                "10",
                "100"
        );

        when(processedEvents.claim(event.eventId()))
                .thenReturn(true);

        when(positionRepository.findForUpdate(
                portfolioId,
                assetId
        )).thenReturn(Optional.empty());

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertThat(result)
                .isEqualTo(
                        ExecutionEventProcessor.ProcessingResult.APPLIED
                );

        verify(positionRepository)
                .save(any(Position.class));

        verify(portfolioPositionLock)
                .lockForPositionUpdate(portfolioId);
    }

    @Test
    void shouldApplyHigherExecutionSequence() {
        Position current = position(
                100L,
                "100",
                "90"
        );

        ExecutionEvent event = buyEvent(
                UUID.randomUUID(),
                101L,
                "20",
                "100"
        );

        when(processedEvents.claim(event.eventId()))
                .thenReturn(true);

        when(positionRepository.findForUpdate(
                portfolioId,
                assetId
        )).thenReturn(Optional.of(current));

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertThat(result)
                .isEqualTo(
                        ExecutionEventProcessor.ProcessingResult.APPLIED
                );

        verify(positionRepository)
                .save(any(Position.class));
    }

    @Test
    void shouldRejectLowerExecutionSequenceAsOutOfOrder() {
        Position current = position(
                100L,
                "100",
                "90"
        );

        ExecutionEvent event = buyEvent(
                UUID.randomUUID(),
                99L,
                "20",
                "100"
        );

        when(processedEvents.claim(event.eventId()))
                .thenReturn(true);

        when(positionRepository.findForUpdate(
                portfolioId,
                assetId
        )).thenReturn(Optional.of(current));

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertThat(result)
                .isEqualTo(
                        ExecutionEventProcessor.ProcessingResult.OUT_OF_ORDER
                );

        verify(positionRepository, never())
                .save(any());

        verify(positionRepository, never())
                .delete(any());
    }

    @Test
    void shouldRejectSameExecutionSequenceAsOutOfOrder() {
        Position current = position(
                100L,
                "100",
                "90"
        );

        ExecutionEvent event = buyEvent(
                UUID.randomUUID(),
                100L,
                "20",
                "100"
        );

        when(processedEvents.claim(event.eventId()))
                .thenReturn(true);

        when(positionRepository.findForUpdate(
                portfolioId,
                assetId
        )).thenReturn(Optional.of(current));

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertThat(result)
                .isEqualTo(
                        ExecutionEventProcessor.ProcessingResult.OUT_OF_ORDER
                );

        verify(positionRepository, never())
                .save(any());

        verify(positionRepository, never())
                .delete(any());
    }

    @Test
    void shouldReturnDuplicateWhenEventWasAlreadyClaimed() {
        ExecutionEvent event = buyEvent(
                UUID.randomUUID(),
                101L,
                "10",
                "100"
        );

        when(processedEvents.claim(event.eventId()))
                .thenReturn(false);

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertThat(result)
                .isEqualTo(
                        ExecutionEventProcessor.ProcessingResult.DUPLICATE
                );

        verifyNoInteractions(
                portfolioPositionLock,
                positionRepository
        );
    }

    @Test
    void shouldKeepPositionUnchangedWhenEventIsOutOfOrder() {
        Position current = position(
                100L,
                "100",
                "90"
        );

        ExecutionEvent event = buyEvent(
                UUID.randomUUID(),
                99L,
                "50",
                "200"
        );

        when(processedEvents.claim(event.eventId()))
                .thenReturn(true);

        when(positionRepository.findForUpdate(
                portfolioId,
                assetId
        )).thenReturn(Optional.of(current));

        ExecutionEventProcessor.ProcessingResult result =
                processor.process(event);

        assertThat(result)
                .isEqualTo(
                        ExecutionEventProcessor.ProcessingResult.OUT_OF_ORDER
                );

        assertThat(current.quantity())
                .isEqualByComparingTo("100");

        assertThat(current.averageCost())
                .isEqualByComparingTo("90");

        assertThat(current.lastExecutionSequence())
                .isEqualTo(100L);

        verify(positionRepository, never())
                .save(any());

        verify(positionRepository, never())
                .delete(any());
    }

    @Test
    void shouldRejectEventWithInvalidSequence() {
        ExecutionEvent event = buyEvent(
                UUID.randomUUID(),
                0L,
                "10",
                "100"
        );

        assertThatThrownBy(
                () -> processor.process(event)
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Execution sequence must be positive"
                );

        verifyNoInteractions(
                processedEvents,
                portfolioPositionLock,
                positionRepository
        );
    }

    @Test
    void shouldClaimBeforeLockingAndLoadingPosition() {
        ExecutionEvent event = buyEvent(
                UUID.randomUUID(),
                101L,
                "10",
                "100"
        );

        when(processedEvents.claim(event.eventId()))
                .thenReturn(true);

        when(positionRepository.findForUpdate(
                portfolioId,
                assetId
        )).thenReturn(Optional.empty());

        processor.process(event);

        InOrder order = inOrder(
                processedEvents,
                portfolioPositionLock,
                positionRepository
        );

        order.verify(processedEvents)
                .claim(event.eventId());

        order.verify(portfolioPositionLock)
                .lockForPositionUpdate(portfolioId);

        order.verify(positionRepository)
                .findForUpdate(
                        portfolioId,
                        assetId
                );
    }

    private ExecutionEvent buyEvent(
            UUID eventId,
            long executionSequence,
            String quantity,
            String price
    ) {
        return new ExecutionEvent(
                eventId,
                UUID.randomUUID(),
                portfolioId,
                assetId,
                ExecutionEvent.Side.BUY,
                new BigDecimal(quantity),
                new BigDecimal(price),
                BigDecimal.ZERO,
                "USD",
                Instant.parse(
                        "2026-10-02T10:00:00Z"
                ),
                executionSequence
        );
    }

    private Position position(
            long sequence,
            String quantity,
            String averageCost
    ) {
        return new Position(
                UUID.randomUUID(),
                portfolioId,
                assetId,
                new BigDecimal(quantity),
                new BigDecimal(averageCost),
                "USD",
                Instant.parse(
                        "2026-10-02T09:00:00Z"
                ),
                sequence
        );
    }
}
