package io.github.fabiozagaria.expensetracker.exceptions;

public class NotFoundExpenseException extends RuntimeException {
    public NotFoundExpenseException(String message) {
        super(message);
    }
}
