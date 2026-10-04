package org.fincore.wealth.position.outbox.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataOutboxEventRepository
        extends JpaRepository<
                OutboxEventJpaEntity,
                UUID
                > {
}