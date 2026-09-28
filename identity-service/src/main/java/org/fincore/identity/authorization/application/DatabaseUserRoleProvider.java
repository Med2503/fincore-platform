package org.fincore.identity.authorization.application;

import org.fincore.identity.auth.application.port.UserRoleProvider;
import org.fincore.identity.authorization.domain.repository.RoleRepository;
import org.fincore.identity.user.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseUserRoleProvider implements UserRoleProvider {

    private final RoleRepository roleRepository;

    public DatabaseUserRoleProvider(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public List<String> getRoles(User user) {
        return roleRepository.findByUserId(user.getId())
                .stream()
                .map(role -> role.name())
                .toList();
    }
}