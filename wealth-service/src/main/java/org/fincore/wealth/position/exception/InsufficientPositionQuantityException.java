package org.fincore.wealth.position.exception;

public class InsufficientPositionQuantityException extends RuntimeException {
    public InsufficientPositionQuantityException(String message) {
        super(message);
    }
}
