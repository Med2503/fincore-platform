package org.fincore.identity.authorization.application;

import org.fincore.identity.auth.application.port.UserAuthoritiesProvider;
import org.fincore.identity.authorization.domain.repository.RoleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DefaultUserAuthoritiesProvider
        implements UserAuthoritiesProvider {

    private final RoleRepository roleRepository;

    public DefaultUserAuthoritiesProvider(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public List<String> getAuthorities(UUID userId) {
        return roleRepository.findPermissionNamesByUserId(userId);
    }
}