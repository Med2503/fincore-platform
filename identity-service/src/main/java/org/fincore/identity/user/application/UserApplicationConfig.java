package org.fincore.identity.user.application;


import org.fincore.identity.user.application.port.PasswordHasher;
import org.fincore.identity.user.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserApplicationConfig {


    @Bean
    CreateUserUseCase createUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        return new CreateUserService(userRepository, passwordHasher);
    }
}
