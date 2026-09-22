package org.fincore.identity.user.application.usecase;

import java.util.UUID;

public interface ResetLoginFailuresUseCase {

    void reset(UUID userId);
}
