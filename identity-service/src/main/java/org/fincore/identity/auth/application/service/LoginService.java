package org.fincore.identity.auth.application.service;

import org.fincore.identity.auth.application.command.LoginCommand;
import org.fincore.identity.auth.application.exception.AccountDisabledException;
import org.fincore.identity.auth.application.exception.AccountLockedException;
import org.fincore.identity.auth.application.exception.AccountPendingException;
import org.fincore.identity.auth.application.exception.InvalidCredentialsException;
import org.fincore.identity.auth.application.port.TokenIssuer;
import org.fincore.identity.auth.application.result.LoginResult;
import org.fincore.identity.auth.application.usecase.LoginUseCase;
import org.fincore.identity.user.application.port.PasswordHasher;
import org.fincore.identity.user.application.service.UsernameNormalizer;
import org.fincore.identity.user.application.usecase.RecordLoginFailureUseCase;
import org.fincore.identity.user.application.usecase.ResetLoginFailuresUseCase;
import org.fincore.identity.user.domain.model.User;
import org.fincore.identity.user.domain.repository.UserRepository;

public class LoginService implements LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final RecordLoginFailureUseCase recordLoginFailureUseCase;
    private final ResetLoginFailuresUseCase resetLoginFailuresUseCase;
    private final TokenIssuer tokenIssuer;
    private final UsernameNormalizer usernameNormalizer;

    public LoginService(UserRepository userRepository, PasswordHasher passwordHasher, RecordLoginFailureUseCase recordLoginFailureUseCase, ResetLoginFailuresUseCase resetLoginFailuresUseCase, TokenIssuer tokenIssuer, UsernameNormalizer usernameNormalizer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.recordLoginFailureUseCase = recordLoginFailureUseCase;
        this.resetLoginFailuresUseCase = resetLoginFailuresUseCase;
        this.tokenIssuer = tokenIssuer;
        this.usernameNormalizer = usernameNormalizer;
    }


    @Override
    public LoginResult login(LoginCommand loginCommand) {

        String username = usernameNormalizer.normalize(loginCommand.username());

        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);
        unlockExpiredAccount(user);
        validateAccountStatus(user);

        if (!passwordHasher.matches(loginCommand.password(), user.getPasswordHash())) {
            recordLoginFailureUseCase.record(user.getId());
            throw new InvalidCredentialsException();
        }
        resetLoginFailuresUseCase.reset(user.getId());
        String accessToken = tokenIssuer.issue(user);
        return new LoginResult(accessToken);

    }

    private void validateAccountStatus(User user) {
        if (user.isDisabled()) {
            throw new AccountDisabledException();
        }
        if (user.isPending()) {
            throw new AccountPendingException();
        }
        if (user.isLocked()) {
            throw new AccountLockedException(user.getLockedUntil());
        }
    }

    private void unlockExpiredAccount(User user) {
        if (user.isLockExpired()) {
            user.unlockIfExpired();
            userRepository.save(user);
        }
    }
}
