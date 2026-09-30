package org.fincore.wealth.profile.application.exception;

public class InvestorProfileAlreadyExistsException extends RuntimeException {
    public InvestorProfileAlreadyExistsException(String message) {
        super(message);
    }
}
