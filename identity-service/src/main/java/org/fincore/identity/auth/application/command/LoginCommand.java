package org.fincore.identity.auth.application.command;

public record LoginCommand(
        String username,
        String password
) {
}
