package org.fincore.wealth.position.application.service;

import org.fincore.wealth.position.application.event.PositionChangedEvent;
import org.fincore.wealth.position.application.port.PositionProjectionRepository;
import org.fincore.wealth.position.application.projection.PositionProjection;
import org.springframework.stereotype.Service;

@Service
public class PositionChangedProjectionService {

    private final PositionProjectionRepository repository;

    public PositionChangedProjectionService(
            PositionProjectionRepository repository
    ) {
        this.repository = repository;
    }

    public void apply(PositionChangedEvent event) {
        repository.save(
                new PositionProjection(
                        event.portfolioId(),
                        event.assetId(),
                        event.quantity(),
                        event.averageCost(),
                        event.currency(),
                        event.executionSequence(),
                        event.occurredAt()
                )
        );
    }
}