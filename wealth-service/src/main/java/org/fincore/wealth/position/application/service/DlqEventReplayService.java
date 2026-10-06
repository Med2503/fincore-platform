package org.fincore.wealth.position.application.service;

import org.fincore.wealth.position.application.port.DlqReplayAttemptRepository;
import org.fincore.wealth.position.application.port.PositionChangedEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class DlqEventReplayService {

    private final PositionChangedEventPublisher publisher;
    private final DlqReplayAttemptRepository attempts;
    private final Clock clock;

    public DlqEventReplayService(
            PositionChangedEventPublisher publisher,
            DlqReplayAttemptRepository attempts,
            Clock clock
    ) {
        this.publisher = publisher;
        this.attempts = attempts;
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

        boolean published =
                publisher.publish(payload);

        if (!published) {
            throw new IllegalStateException(
                    "Unable to publish DLQ event"
            );
        }

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