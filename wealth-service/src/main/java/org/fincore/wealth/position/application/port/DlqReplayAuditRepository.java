package org.fincore.wealth.position.application.port;

import java.time.Instant;
import java.util.UUID;

public interface DlqReplayAuditRepository {

    void record(
            UUID eventId,
            int replayCount,
            Instant replayedAt
    );
}
