package io.github.fabiozagaria.expensetracker.exceptions;

public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException() {
        super("Invio email non riuscito. Riprova più tardi.");
    }
}

