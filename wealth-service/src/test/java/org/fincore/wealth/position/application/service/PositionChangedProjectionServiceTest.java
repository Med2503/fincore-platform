package org.fincore.wealth.position.application.service;


import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.fincore.wealth.position.application.port.PositionProjectionRepository;
import org.fincore.wealth.position.application.projection.PositionProjection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PositionChangedProjectionServiceTest {

    private final PositionProjectionRepository repository =
            mock(PositionProjectionRepository.class);

    private final PositionChangedProjectionService service =
            new PositionChangedProjectionService(repository);

    @Test
    void shouldSaveOpenPosition() {
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PositionChangedEvent event = event(
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100.50"),
                10
        );

        service.apply(event);

        verify(repository).save(
                argThat(projection ->
                        projection.portfolioId().equals(portfolioId)
                                && projection.assetId().equals(assetId)
                                && projection.quantity()
                                .compareTo(new BigDecimal("10")) == 0
                                && projection.executionSequence() == 10
                )
        );
    }

    @Test
    void shouldKeepTombstoneWhenPositionIsClosed() {
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PositionChangedEvent event = event(
                portfolioId,
                assetId,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                11
        );

        service.apply(event);

        verify(repository).save(
                argThat(projection ->
                        projection.portfolioId().equals(portfolioId)
                                && projection.assetId().equals(assetId)
                                && projection.quantity().signum() == 0
                                && projection.averageCost().signum() == 0
                                && projection.executionSequence() == 11
                )
        );
    }

    @Test
    void shouldPreserveSequenceOfClosedPosition() {
        PositionChangedEvent event = event(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                25
        );

        service.apply(event);

        verify(repository).save(
                argThat(projection ->
                        projection.executionSequence() == 25
                )
        );
    }

    private PositionChangedEvent event(
            UUID portfolioId,
            UUID assetId,
            BigDecimal quantity,
            BigDecimal averageCost,
            long sequence
    ) {
        return new PositionChangedEvent(

                UUID.randomUUID(),
                portfolioId,
                assetId,
                quantity,
                averageCost,
                "EUR",
                sequence,
                Instant.parse("2026-09-30T10:00:00Z")
        );
    }

    @Test
    void shouldIgnoreOlderEventAfterPositionClosure() {
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        repository.save(new PositionProjection(
                portfolioId,
                assetId,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "EUR",
                11,
                Instant.parse("2026-09-30T11:00:00Z")
        ));

        repository.save(new PositionProjection(
                portfolioId,
                assetId,
                new BigDecimal("10"),
                new BigDecimal("100"),
                "EUR",
                10,
                Instant.parse("2026-09-30T10:00:00Z")
        ));

        PositionProjection projection =
                repository.find(portfolioId, assetId).orElseThrow();

        assertEquals(0, projection.quantity().signum());
        assertEquals(11, projection.executionSequence());
    }
}