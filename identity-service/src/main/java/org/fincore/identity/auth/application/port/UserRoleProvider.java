package org.fincore.identity.auth.application.port;

import org.fincore.identity.user.domain.model.User;

public interface UserRoleProvider {

    String getPrimaryRole(User user);
}
