package org.esercizi.expensetrackerapi.security.ratelimit;

import java.time.Duration;
import java.util.Optional;

/**
 * Soglie per categoria di endpoint pubblico, per indirizzo IP. Sono "token bucket": la capacità è
 * la raffica consentita, il rifornimento è distribuito nel tempo (capacity token per period).
 */
public enum RateLimitPolicy {
    /** Tentativi di password: 10 al minuto per IP. */
    LOGIN(10, Duration.ofMinutes(1), "/auth/login"),
    /** Accesso alla demo (credenziali pubbliche, ma ogni visitatore lo usa): più largo. */
    DEMO(30, Duration.ofMinutes(1), "/auth/demo"),
    /** Endpoint che inviano email (quota del provider, spam verso terzi): 5 all'ora per IP. */
    MAIL(5, Duration.ofHours(1), "/auth/register", "/auth/resend-verification", "/auth/forgot-password"),
    /** Endpoint che consumano un token segreto: frenano i tentativi a indovinare. */
    TOKEN(10, Duration.ofMinutes(1), "/auth/verify-email", "/auth/reset-password"),
    /** Il frontend rinnova la sessione da solo: soglia larga, serve solo contro i loop. */
    SESSION(30, Duration.ofMinutes(1), "/auth/refresh", "/auth/logout");

    private final int capacity;
    private final Duration period;
    private final String[] paths;

    RateLimitPolicy(int capacity, Duration period, String... paths) {
        this.capacity = capacity;
        this.period = period;
        this.paths = paths;
    }

    public int capacity() {
        return capacity;
    }

    public Duration period() {
        return period;
    }

    /** La categoria a cui appartiene il percorso (senza context path), se è limitato. */
    public static Optional<RateLimitPolicy> forPath(String path) {
        for (RateLimitPolicy policy : values()) {
            for (String candidate : policy.paths) {
                if (candidate.equals(path)) {
                    return Optional.of(policy);
                }
            }
        }
        return Optional.empty();
    }
}
