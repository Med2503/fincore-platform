package org.fincore.wealth.position.infrastructure.persistence;

import org.fincore.wealth.position.infrastructure.persistence.ProcessedWealthEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SpringDataProcessedEventRepository
        extends JpaRepository<ProcessedWealthEventJpaEntity, UUID> {
}