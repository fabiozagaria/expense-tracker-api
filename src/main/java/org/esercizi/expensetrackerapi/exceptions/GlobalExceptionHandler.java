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

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<APIError> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity
                .status(status)
                .body(new APIError(
                        "INVALID_REFRESH_TOKEN",
                        exception.getMessage(),
                        request.getRequestURI(),
                        Instant.now(),
                        status
                ));

    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<APIError> handleInvalidVerificationToken(
            InvalidVerificationTokenException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity
                .badRequest()
                .body(new APIError(
                        "INVALID_VERIFICATION_TOKEN",
                        exception.getMessage(),
                        request.getRequestURI(),
                        Instant.now(),
                        status
                ));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<APIError> handleEmailNotVerifiedException(
            EmailNotVerifiedException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity
                .status(status)
                .body(new APIError(
                        "EMAIL_NOT_VERIFIED",
                        exception.getMessage(),
                        request.getRequestURI(),
                        Instant.now(),
                        status
                ));
    }
}
