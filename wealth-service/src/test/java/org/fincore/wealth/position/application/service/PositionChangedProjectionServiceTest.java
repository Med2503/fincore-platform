package org.fincore.wealth.position.application.service;

import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.fincore.wealth.position.application.port.PositionProjectionRepository;
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
class PositionChangedProjectionServiceTest {

    @Mock
    private PositionProjectionRepository repository;

    private PositionChangedProjectionService service;

    @BeforeEach
    void setUp() {
        service = new PositionChangedProjectionService(repository);
    }

    @Test
    void shouldSavePositionProjection() {
        PositionChangedEvent event = event(
                new BigDecimal("10.0000000000"),
                new BigDecimal("125.5000000000"),
                5
        );

        service.apply(event);

        verify(repository).save(argThat(projection ->
                projection.portfolioId().equals(
                        event.portfolioId()
                )
                        && projection.assetId().equals(
                        event.assetId()
                )
                        && projection.quantity().equals(
                        event.quantity()
                )
                        && projection.averageCost().equals(
                        event.averageCost()
                )
                        && projection.currency().equals(
                        event.currency()
                )
                        && projection.executionSequence() == 5
                        && projection.updatedAt().equals(
                        event.occurredAt()
                )
        ));

        verify(repository, never())
                .delete(any(), any());
    }

    @Test
    void shouldDeleteProjectionWhenPositionIsClosed() {
        PositionChangedEvent event = event(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                8
        );

        service.apply(event);

        verify(repository).delete(
                event.portfolioId(),
                event.assetId()
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldNotDeleteWhenQuantityIsPositive() {
        PositionChangedEvent event = event(
                BigDecimal.ONE,
                new BigDecimal("100"),
                3
        );

        service.apply(event);

        verify(repository).save(any());
        verify(repository, never()).delete(any(), any());
    }

    private PositionChangedEvent event(
            BigDecimal quantity,
            BigDecimal averageCost,
            long sequence
    ) {
        return new PositionChangedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                quantity,
                averageCost,
                "EUR",
                sequence,
                Instant.parse(
                        "2026-10-05T10:00:00Z"
                )
        );
    }
}
