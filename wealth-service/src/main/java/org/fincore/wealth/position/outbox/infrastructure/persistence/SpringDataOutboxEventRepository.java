package org.fincore.wealth.position.outbox.infrastructure.persistence;

import org.fincore.wealth.position.outbox.infrastructure.persistence.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataOutboxEventRepository
        extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE status = :status
              AND (
                  next_attempt_at IS NULL
                  OR next_attempt_at <= :now
              )
            ORDER BY occurred_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventJpaEntity> findPendingForUpdate(
            @Param("status") String status,
            @Param("now") Instant now,
            @Param("batchSize") int batchSize
    );
}