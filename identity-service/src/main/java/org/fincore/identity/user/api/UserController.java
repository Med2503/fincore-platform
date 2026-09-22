package org.fincore.identity.user.api;


import jakarta.validation.Valid;
import org.fincore.identity.user.application.usecase.CreateUserUseCase;
import org.fincore.identity.user.application.command.CreateUserCommand;
import org.fincore.identity.user.domain.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase) {
        this.createUserUseCase = createUserUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {

        CreateUserCommand command = new CreateUserCommand(request.username(), request.password());
        User user = createUserUseCase.create(command);
        return UserResponse.from(user);
    }

}
