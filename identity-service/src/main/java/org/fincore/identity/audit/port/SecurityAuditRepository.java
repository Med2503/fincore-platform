package org.fincore.identity.audit.port;

import org.fincore.identity.audit.domain.SecurityAuditEvent;

public interface SecurityAuditRepository {
    void append(SecurityAuditEvent event);
}
