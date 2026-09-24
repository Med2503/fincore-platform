package org.fincore.identity.auth.application.usecase;

import org.fincore.identity.auth.application.command.LoginCommand;
import org.fincore.identity.auth.application.result.LoginResult;

public interface LoginUseCase {
    LoginResult login(LoginCommand loginCommand);
}
