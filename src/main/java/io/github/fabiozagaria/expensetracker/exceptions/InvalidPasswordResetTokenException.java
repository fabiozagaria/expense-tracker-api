package io.github.fabiozagaria.expensetracker.exceptions;

public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() {
        // Messaggio unico: non distingue token inesistente, scaduto o già usato.
        super("Link di reimpostazione non valido o scaduto");
    }
}
