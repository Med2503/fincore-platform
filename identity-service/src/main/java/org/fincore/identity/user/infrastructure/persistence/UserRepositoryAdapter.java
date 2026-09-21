package org.fincore.identity.user.infrastructure.persistence;

import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.model.UserStatus;
import org.fincore.identity.user.domain.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository repository;

    public UserRepositoryAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = repository.save(entity);

        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                UserStatus.valueOf(entity.getStatus().name()),
                entity.getFailedLoginAttempts(),
                entity.getLockedUntil(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    private UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();

        entity.setId(user.getId());
        entity.setUsername(user.getUsername());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setStatus(
                UserStatusJpa.valueOf(user.getStatus().name())
        );
        entity.setFailedLoginAttempts(
                user.getFailedLoginAttempts()
        );
        entity.setLockedUntil(
                user.getLockedUntil()
        );
        entity.setCreatedAt(
                user.getCreatedAt()
        );
        entity.setUpdatedAt(
                user.getUpdatedAt()
        );
        entity.setVersion(
                user.getVersion()
        );

        return entity;
    }
}