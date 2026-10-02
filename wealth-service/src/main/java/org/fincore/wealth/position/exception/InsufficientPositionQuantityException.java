package org.fincore.wealth.position.exception;

import java.math.BigDecimal;

public class InsufficientPositionQuantityException extends RuntimeException {
    public InsufficientPositionQuantityException(BigDecimal quantity) {
        System.out.printf("Insuffisant quantiti %d", quantity);
    }
}
