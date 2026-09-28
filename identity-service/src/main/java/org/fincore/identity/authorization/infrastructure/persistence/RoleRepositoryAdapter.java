package org.fincore.identity.authorization.infrastructure.persistence;

import org.fincore.identity.authorization.domain.model.Role;
import org.fincore.identity.authorization.domain.repository.RoleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository roleRepository;
    private final JdbcTemplate jdbcTemplate;

    public RoleRepositoryAdapter(
            RoleJpaRepository roleRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.roleRepository = roleRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return roleRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(normalize(name))
                .map(this::toDomain);
    }

    @Override
    public List<Role> findByUserId(UUID userId) {
        String sql = """
                SELECT r.id, r.name, r.description, r.created_at
                FROM roles r
                JOIN user_roles ur ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.name
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new Role(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                userId
        );
    }

    @Override
    public List<String> findPermissionNamesByUserId(UUID userId) {
        String sql = """
                SELECT DISTINCT p.name
                FROM permissions p
                JOIN role_permissions rp ON rp.permission_id = p.id
                JOIN user_roles ur ON ur.role_id = rp.role_id
                WHERE ur.user_id = ?
                ORDER BY p.name
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("name"),
                userId
        );
    }

    private Role toDomain(RoleJpaEntity entity) {
        return new Role(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    private String normalize(String name) {
        return name.trim().toUpperCase(java.util.Locale.ROOT);
    }
}