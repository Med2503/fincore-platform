package org.fincore.identity.user.application.usecase;

import org.fincore.identity.user.application.command.CreateUserCommand;
import org.fincore.identity.user.domain.model.User;

public interface CreateUserUseCase {

    User create(CreateUserCommand command);
}
