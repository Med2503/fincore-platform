package org.fincore.identity.shared.error;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        ErrorCode code,
        String message,
        String path,
        String correlationId
) {
}
