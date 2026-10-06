package org.fincore.wealth.position.application.service;

import org.fincore.wealth.position.application.port.DlqReplayAttemptRepository;
import org.fincore.wealth.position.application.port.DlqReplayOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class DlqEventReplayService {

    private final DlqReplayAttemptRepository attempts;
    private final DlqReplayOutboxRepository outbox;
    private final Clock clock;

    public DlqEventReplayService(
            DlqReplayAttemptRepository attempts,
            DlqReplayOutboxRepository outbox,
            Clock clock
    ) {
        this.attempts = attempts;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public DlqReplayResult replay(
            UUID eventId,
            String payload,
            int maxReplays
    ) {
        validate(
                eventId,
                payload,
                maxReplays
        );

        Instant now = clock.instant();

        int claimed =
                attempts.claimNextReplay(
                        eventId,
                        now,
                        maxReplays
                );

        if (claimed == 0) {
            return DlqReplayResult.REJECTED;
        }

        outbox.save(
                eventId,
                payload,
                now
        );

        return DlqReplayResult.REPLAYED;
    }

    private void validate(
            UUID eventId,
            String payload,
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

        if (maxReplays <= 0) {
            throw new IllegalArgumentException(
                    "Maximum replays must be positive"
            );
        }
    }
}