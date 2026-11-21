package io.github.fabiozagaria.expensetracker.exceptions;

public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Troppe richieste: riprova tra " + retryAfterSeconds + " secondi");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
