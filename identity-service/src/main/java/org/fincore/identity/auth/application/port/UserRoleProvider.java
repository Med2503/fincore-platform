package org.fincore.identity.auth.application.port;

import org.fincore.identity.user.domain.model.User;

import java.util.List;

public interface UserRoleProvider {

    List<String> getRoles(User user);
}
