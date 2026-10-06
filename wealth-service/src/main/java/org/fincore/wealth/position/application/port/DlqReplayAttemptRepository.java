package org.fincore.wealth.position.application.port;

import java.time.Instant;
import java.util.UUID;

public interface DlqReplayAttemptRepository {

    int claimNextReplay(
            UUID eventId,
            Instant now,
            int maxReplays
    );
}