package org.fincore.identity.refresh.exceptions;

public class RefreshTokenReuseDetectedException extends RuntimeException {
    public RefreshTokenReuseDetectedException(String message) {
        super(message);
    }
}
