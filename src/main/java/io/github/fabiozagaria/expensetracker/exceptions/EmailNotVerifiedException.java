package io.github.fabiozagaria.expensetracker.exceptions;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
