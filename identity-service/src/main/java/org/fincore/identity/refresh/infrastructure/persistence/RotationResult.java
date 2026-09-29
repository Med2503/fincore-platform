package org.fincore.identity.refresh.infrastructure.persistence;


import java.util.UUID;

public record RotationResult(
            RotationStatus status,
            UUID userId,
            String rawRefreshToken
    ) {
        public static RotationResult invalid() {
            return new RotationResult(RotationStatus.INVALID, null, null);
        }

        public static RotationResult reuseDetected() {
            return new RotationResult(RotationStatus.REUSE_DETECTED, null, null);
        }
    }
