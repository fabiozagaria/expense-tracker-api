package org.esercizi.expensetrackerapi.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Limitatore in memoria, per processo: un bucket per (categoria, client).
 * Con più istanze del backend ogni istanza conta per conto suo (il limite effettivo si moltiplica);
 * per un deploy a istanza singola, come quello attuale, è esatto.
 */
public class RateLimiter {
    /** Più lungo del periodo massimo (1 ora): un bucket inattivo così a lungo sarebbe comunque tornato pieno. */
    private static final Duration IDLE_EVICTION = Duration.ofHours(2);
    /** Tetto alla memoria: oltre questa soglia i bucket meno usati vengono scartati (si "perdona" qualcuno, non si va in OOM). */
    private static final long MAX_TRACKED_CLIENTS = 100_000;

    public record Decision(boolean allowed, long retryAfterSeconds) {
        static Decision allow() {
            return new Decision(true, 0);
        }
    }

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(IDLE_EVICTION)
            .maximumSize(MAX_TRACKED_CLIENTS)
            .build();
    private final TimeMeter timeMeter;

    public RateLimiter() {
        this(TimeMeter.SYSTEM_MILLISECONDS);
    }

    /** Per i test: un orologio controllabile al posto di quello di sistema. */
    public RateLimiter(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    public Decision tryAcquire(RateLimitPolicy policy, String clientKey) {
        Bucket bucket = buckets.get(policy.name() + ":" + clientKey, key -> newBucket(policy));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return Decision.allow();
        }
        long seconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
        // Arrotondato per eccesso a 1 secondo: Retry-After: 0 inviterebbe il client a riprovare subito.
        return new Decision(false, Math.max(1, seconds + 1));
    }

    private Bucket newBucket(RateLimitPolicy policy) {
        return Bucket.builder()
                .withCustomTimePrecision(timeMeter)
                .addLimit(limit -> limit.capacity(policy.capacity()).refillGreedy(policy.capacity(), policy.period()))
                .build();
    }
}
