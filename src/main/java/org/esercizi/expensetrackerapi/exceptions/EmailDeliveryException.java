package org.esercizi.expensetrackerapi.exceptions;

public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException() {
        super("Invio email non disponibile. Registrazione non completata: riprova più tardi.");
    }
}
