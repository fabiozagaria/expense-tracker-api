package org.esercizi.expensetrackerapi.exceptions;

public class NotFoundExpenseException extends RuntimeException {
    public NotFoundExpenseException(String message) {
        super(message);
    }
}
