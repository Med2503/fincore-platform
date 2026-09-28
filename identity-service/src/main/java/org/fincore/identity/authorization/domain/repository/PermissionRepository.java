package org.fincore.identity.authorization.domain.repository;

import org.fincore.identity.authorization.domain.model.Permission;

import java.util.List;
import java.util.UUID;

public interface PermissionRepository {
    List<Permission> findByRoleId(UUID roleId);
}
