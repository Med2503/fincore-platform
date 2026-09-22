package org.fincore.identity.user.application.usecase;

import java.util.UUID;

public interface RecordLoginFailureUseCase {
    void record(UUID userId);
}
