package org.fincore.identity.auth.application.exception;

public class AccountPendingException extends RuntimeException {

    public AccountPendingException() {
        super("Account is pending activation");
    }
}
