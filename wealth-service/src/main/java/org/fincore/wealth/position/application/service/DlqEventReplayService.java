package org.fincore.wealth.position.application.service;

import org.fincore.wealth.position.application.port.DlqReplayAuditRepository;
import org.fincore.wealth.position.application.port.PositionChangedEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class DlqEventReplayService {

    private final PositionChangedEventPublisher publisher;
    private final DlqReplayAuditRepository auditRepository;
    private final Clock clock;

    public DlqEventReplayService(
            PositionChangedEventPublisher publisher,
            DlqReplayAuditRepository auditRepository,
            Clock clock
    ) {
        this.publisher = publisher;
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    public DlqReplayResult replay(
            UUID eventId,
            String payload,
            int replayCount,
            int maxReplays
    ) {
        Objects.requireNonNull(
                eventId,
                "Event ID is required"
        );

        Objects.requireNonNull(
                payload,
                "Payload is required"
        );

        if (payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Payload cannot be blank"
            );
        }

        if (replayCount < 0) {
            throw new IllegalArgumentException(
                    "Replay count cannot be negative"
            );
        }

        if (maxReplays <= 0) {
            throw new IllegalArgumentException(
                    "Maximum replays must be positive"
            );
        }

        if (replayCount >= maxReplays) {
            return DlqReplayResult.REJECTED;
        }

        boolean published =
                publisher.publish(payload);

        if (!published) {
            throw new IllegalStateException(
                    "Unable to publish DLQ event"
            );
        }

        auditRepository.record(
                eventId,
                replayCount + 1,
                Instant.now(clock)
        );

        return DlqReplayResult.REPLAYED;
    }
}