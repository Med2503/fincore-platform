package org.fincore.identity.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record SecurityAuditEvent(
        UUID id,
        UUID userId,
        String eventType,
        Instant occurredAt,
        String correlationId,
        String sourceIp,
        String userAgent,
        String details
) {
}
