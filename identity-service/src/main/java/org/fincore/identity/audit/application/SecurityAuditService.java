package org.fincore.identity.audit.application;

import org.fincore.identity.audit.domain.*;
import org.fincore.identity.audit.port.SecurityAuditRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
public class SecurityAuditService {

    private final SecurityAuditRepository repository;
    private final Clock clock;

    public SecurityAuditService(
            SecurityAuditRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(
            UUID userId,
            String eventType,
            String correlationId,
            String sourceIp,
            String userAgent
    ) {
        repository.append(new SecurityAuditEvent(
                UUID.randomUUID(),
                userId,
                eventType,
                clock.instant(),
                correlationId,
                sourceIp,
                userAgent,
                null
        ));
    }
}
