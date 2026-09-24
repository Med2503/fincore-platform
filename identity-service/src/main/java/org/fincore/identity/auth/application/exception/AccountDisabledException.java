package org.fincore.identity.auth.application.exception;

public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("Account is disabled");
    }
}
