package org.fincore.identity.authorization.domain.repository;

import org.fincore.identity.authorization.domain.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {

    Optional<Role> findById(UUID id);

    Optional<Role> findByName(String name);

    List<Role> findByUserId(UUID userId);

    List<String> findPermissionNamesByUserId(UUID userId);
}