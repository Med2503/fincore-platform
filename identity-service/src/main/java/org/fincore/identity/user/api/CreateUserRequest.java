package org.fincore.identity.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "Username is required")
        @Size(
                min = 3,
                max = 100,
                message = "Username must contain between 3 and 100 characters"
        )
        String username,

        @NotBlank(message = "Password is required")
        @Size(
                min = 8,
                max = 100,
                message = "Password must contain between 8 and 100 characters"
        )
        String password
) {
}
