package org.fincore.identity.user.application.command;

public record CreateUserCommand(
        String username,
        String password
) {
}
