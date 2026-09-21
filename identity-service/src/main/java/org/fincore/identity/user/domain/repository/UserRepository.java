package org.fincore.identity.user.domain.repository;

import org.fincore.identity.user.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);


}
