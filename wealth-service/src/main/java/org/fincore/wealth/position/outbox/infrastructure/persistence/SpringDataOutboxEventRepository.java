package org.fincore.wealth.position.outbox.infrastructure.persistence;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataOutboxEventRepository
        extends JpaRepository<
        OutboxEventJpaEntity,
        UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = """
        SELECT *
        FROM outbox_events
        WHERE
            (
                status = 'PENDING'
                AND (
                    next_attempt_at IS NULL
                    OR next_attempt_at <= :now
                )
            )
            OR
            (
                status = 'PROCESSING'
                AND locked_until <= :now
            )
        ORDER BY occurred_at ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEventJpaEntity> findClaimable(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize
    );
}