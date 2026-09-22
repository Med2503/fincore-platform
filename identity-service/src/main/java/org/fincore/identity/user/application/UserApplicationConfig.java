package org.fincore.identity.user.application;


import org.fincore.identity.user.application.port.PasswordHasher;
import org.fincore.identity.user.application.security.AccountLockPolicy;
import org.fincore.identity.user.application.service.RecordLoginFailureService;
import org.fincore.identity.user.application.service.ResetLoginFailuresService;
import org.fincore.identity.user.application.service.UsernameNormalizer;
import org.fincore.identity.user.application.usecase.RecordLoginFailureUseCase;
import org.fincore.identity.user.application.usecase.ResetLoginFailuresUseCase;
import org.fincore.identity.user.domain.repository.UserRepository;
import org.fincore.identity.user.application.service.CreateUserService;
import org.fincore.identity.user.application.usecase.CreateUserUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserApplicationConfig {


    @Bean
    CreateUserUseCase createUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher, UsernameNormalizer normalizer) {
        return new CreateUserService(userRepository, passwordHasher, normalizer);
    }

    @Bean
    public RecordLoginFailureUseCase recordLoginFailureUseCase(
            UserRepository userRepository,
            AccountLockPolicy lockPolicy
    ) {
        return new RecordLoginFailureService(
                userRepository,
                lockPolicy
        );
    }

    @Bean
    public ResetLoginFailuresUseCase resetLoginFailuresUseCase(
            UserRepository userRepository
    ) {
        return new ResetLoginFailuresService(
                userRepository
        );
    }
}

