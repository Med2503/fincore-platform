package org.fincore.identity.authorization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleJpaRepository
        extends JpaRepository<RoleJpaEntity, UUID> {

    Optional<RoleJpaEntity> findByName(String name);
}