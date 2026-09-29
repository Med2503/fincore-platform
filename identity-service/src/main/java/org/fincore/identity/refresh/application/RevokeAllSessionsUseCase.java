package org.fincore.identity.refresh.application;

import java.util.UUID;

public interface RevokeAllSessionsUseCase {
    int revokeAll(UUID userId);
}
