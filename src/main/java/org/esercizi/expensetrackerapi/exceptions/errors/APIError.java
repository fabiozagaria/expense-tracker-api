package org.esercizi.expensetrackerapi.exceptions.errors;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record APIError(
        String errorName,
        String details,
        String path,
        Instant timestamp,
        HttpStatus status
) {
}
