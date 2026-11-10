package org.esercizi.expensetrackerapi.security.ratelimit;

import io.github.bucket4j.TimeMeter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/** Logica del limitatore con un orologio finto: nessuna attesa reale. */
class RateLimiterTests {

    /** Orologio controllabile: il tempo passa solo quando lo si fa avanzare. */
    static final class FakeClock implements TimeMeter {
        private final AtomicLong nanos = new AtomicLong();

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }

        @Override
        public long currentTimeNanos() {
            return nanos.get();
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }
    }

    FakeClock clock;
    RateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
        limiter = new RateLimiter(clock);
    }

    private int consumeAll(RateLimitPolicy policy, String client, int attempts) {
        int allowed = 0;
        for (int i = 0; i < attempts; i++) {
            if (limiter.tryAcquire(policy, client).allowed()) allowed++;
        }
        return allowed;
    }

    @Test
    void allowsTheBurstThenRejectsWithARetryAfter() {
        assertThat(consumeAll(RateLimitPolicy.LOGIN, "1.1.1.1", 10)).isEqualTo(10);

        RateLimiter.Decision rejected = limiter.tryAcquire(RateLimitPolicy.LOGIN, "1.1.1.1");

        assertThat(rejected.allowed()).isFalse();
        // 10 token al minuto = uno ogni 6 secondi: l'attesa è al massimo di 6 secondi (+1 di arrotondamento).
        assertThat(rejected.retryAfterSeconds()).isBetween(1L, 7L);
    }

    @Test
    void tokensComeBackGraduallyAndNotAllAtOnce() {
        consumeAll(RateLimitPolicy.LOGIN, "1.1.1.1", 10);
        assertThat(limiter.tryAcquire(RateLimitPolicy.LOGIN, "1.1.1.1").allowed()).isFalse();

        clock.advance(Duration.ofSeconds(6));

        assertThat(limiter.tryAcquire(RateLimitPolicy.LOGIN, "1.1.1.1").allowed()).as("un token rifornito").isTrue();
        assertThat(limiter.tryAcquire(RateLimitPolicy.LOGIN, "1.1.1.1").allowed()).as("uno solo").isFalse();
    }

    @Test
    void aFullMinuteRestoresTheWholeBurst() {
        consumeAll(RateLimitPolicy.LOGIN, "1.1.1.1", 10);

        clock.advance(Duration.ofMinutes(1));

        assertThat(consumeAll(RateLimitPolicy.LOGIN, "1.1.1.1", 10)).isEqualTo(10);
    }

    @Test
    void clientsAreIndependent() {
        consumeAll(RateLimitPolicy.LOGIN, "1.1.1.1", 10);

        assertThat(limiter.tryAcquire(RateLimitPolicy.LOGIN, "1.1.1.1").allowed()).isFalse();
        assertThat(limiter.tryAcquire(RateLimitPolicy.LOGIN, "2.2.2.2").allowed()).isTrue();
    }

    @Test
    void policiesAreIndependentForTheSameClient() {
        consumeAll(RateLimitPolicy.LOGIN, "1.1.1.1", 10);

        assertThat(limiter.tryAcquire(RateLimitPolicy.LOGIN, "1.1.1.1").allowed()).isFalse();
        assertThat(limiter.tryAcquire(RateLimitPolicy.SESSION, "1.1.1.1").allowed()).isTrue();
    }

    @Test
    void emailEndpointsAreLimitedToFiveAnHour() {
        assertThat(consumeAll(RateLimitPolicy.MAIL, "1.1.1.1", 6)).isEqualTo(5);

        clock.advance(Duration.ofMinutes(30));
        assertThat(limiter.tryAcquire(RateLimitPolicy.MAIL, "1.1.1.1").allowed())
                .as("dopo mezz'ora è rientrato 2,5 token: ne basta uno").isTrue();
    }

    @Test
    void everyPublicAuthEndpointBelongsToOneCategoryAndOthersAreNotLimited() {
        assertThat(RateLimitPolicy.forPath("/auth/login")).contains(RateLimitPolicy.LOGIN);
        assertThat(RateLimitPolicy.forPath("/auth/demo")).contains(RateLimitPolicy.DEMO);
        assertThat(RateLimitPolicy.forPath("/auth/register")).contains(RateLimitPolicy.MAIL);
        assertThat(RateLimitPolicy.forPath("/auth/resend-verification")).contains(RateLimitPolicy.MAIL);
        assertThat(RateLimitPolicy.forPath("/auth/forgot-password")).contains(RateLimitPolicy.MAIL);
        assertThat(RateLimitPolicy.forPath("/auth/verify-email")).contains(RateLimitPolicy.TOKEN);
        assertThat(RateLimitPolicy.forPath("/auth/reset-password")).contains(RateLimitPolicy.TOKEN);
        assertThat(RateLimitPolicy.forPath("/auth/refresh")).contains(RateLimitPolicy.SESSION);
        assertThat(RateLimitPolicy.forPath("/auth/logout")).contains(RateLimitPolicy.SESSION);
        assertThat(RateLimitPolicy.forPath("/auth/qualcosa-d-altro")).isEmpty();
        assertThat(RateLimitPolicy.forPath("/api/expenses")).isEmpty();
    }
}
