package org.fincore.identity.auth.api;

import jakarta.validation.Valid;
import org.fincore.identity.auth.application.command.LoginCommand;
import org.fincore.identity.auth.application.result.LoginResult;
import org.fincore.identity.auth.application.usecase.LoginUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final LoginUseCase loginUseCase;

    public LoginController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResult result = loginUseCase.login(
                new LoginCommand(
                        request.username(),
                        request.password()
                )
        );

        return ResponseEntity.ok(
                new LoginResponse(
                        result.accessToken(),
                        "Bearer"
                )
        );
    }
}