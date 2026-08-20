package org.esercizi.expensetrackerapi.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.esercizi.expensetrackerapi.exceptions.errors.APIError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundExpenseException.class)
    public ResponseEntity<APIError> handleNotFoundExpense(
            NotFoundExpenseException expenseException,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity
                .status(status)
                .body(new APIError(
                        "NOT_FOUND_EXPENSE",
                        expenseException.getMessage(),
                        request.getRequestURI(),
                        Instant.now(),
                        status

                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIError> handleIllegalArgument(
            IllegalArgumentException illegalArgumentException,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity
                .status(status)
                .body(new APIError(
                        "ILLEGAL_ARGUMENT",
                        illegalArgumentException.getMessage(),
                        request.getRequestURI(),
                        Instant.now(),
                        status
                ));
    }
}
