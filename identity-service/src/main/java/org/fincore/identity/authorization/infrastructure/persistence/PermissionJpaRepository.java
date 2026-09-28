package org.fincore.identity.authorization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermissionJpaRepository
        extends JpaRepository<PermissionJpaEntity, UUID> {
}
