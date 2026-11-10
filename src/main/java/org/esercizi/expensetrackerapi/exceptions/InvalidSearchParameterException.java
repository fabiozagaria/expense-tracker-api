package org.esercizi.expensetrackerapi.exceptions;

/**
 * Parametro di paginazione o ricerca non valido. Il messaggio è scritto per il client.
 * Estende IllegalArgumentException per restare compatibile con chi già la intercetta.
 */
public class InvalidSearchParameterException extends IllegalArgumentException {
    public InvalidSearchParameterException(String message) {
        super(message);
    }
}
