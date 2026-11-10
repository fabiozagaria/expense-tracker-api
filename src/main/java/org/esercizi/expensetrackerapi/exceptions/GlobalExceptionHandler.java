package org.esercizi.expensetrackerapi.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.esercizi.expensetrackerapi.exceptions.errors.APIError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundIncomeException.class)
    public ResponseEntity<APIError> handleNotFoundIncome(NotFoundIncomeException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(new APIError(
                "NOT_FOUND_INCOME", exception.getMessage(), request.getRequestURI(), Instant.now(), status));
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<APIError> handleEmailDelivery(EmailDeliveryException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(new APIError(
                "EMAIL_DELIVERY_FAILED", exception.getMessage(), request.getRequestURI(), Instant.now(), status));
    }


    @ExceptionHandler(RegistrationConflictException.class)
    public ResponseEntity<APIError> handleRegistrationConflict(
            RegistrationConflictException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new APIError(
                exception.getErrorName(), exception.getMessage(), request.getRequestURI(), Instant.now(), status));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<APIError> handleBadCredentials(BadCredentialsException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(new APIError(
                "INVALID_CREDENTIALS", "Credenziali non valide", request.getRequestURI(), Instant.now(), status));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIError> handleDataConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new APIError(
                "DATA_CONFLICT", "Dati in conflitto con una risorsa esistente", request.getRequestURI(), Instant.now(), status));
    }

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

    /**
     * Solo gli errori di input previsti dall'applicazione (messaggio scritto per il client).
     * Un IllegalArgumentException imprevisto NON arriva qui: è un bug interno e non deve
     * esporre il proprio messaggio. Il nome ILLEGAL_ARGUMENT resta per compatibilità col client.
     */
    @ExceptionHandler(InvalidSearchParameterException.class)
    public ResponseEntity<APIError> handleInvalidSearchParameter(
            InvalidSearchParameterException exception,
            HttpServletRequest request
    ) {
        return badRequest("ILLEGAL_ARGUMENT", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        if (details.isEmpty()) {
            details = "Richiesta non valida";
        }
        return badRequest("VALIDATION_ERROR", details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIError> handleUnreadableBody(HttpServletRequest request) {
        // Il messaggio dell'eccezione contiene dettagli di Jackson: non va esposto.
        return badRequest("MALFORMED_REQUEST", "Corpo della richiesta mancante o non valido", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return badRequest("INVALID_PARAMETER", "Parametro non valido: " + exception.getName(), request);
    }

    private static ResponseEntity<APIError> badRequest(String errorName, String details, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new APIError(
                errorName, details, request.getRequestURI(), Instant.now(), status));
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

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<APIError> handleInvalidPasswordResetToken(
            InvalidPasswordResetTokenException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new APIError(
                "INVALID_PASSWORD_RESET_TOKEN", exception.getMessage(),
                request.getRequestURI(), Instant.now(), status));
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


