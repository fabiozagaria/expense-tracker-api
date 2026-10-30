package org.esercizi.expensetrackerapi.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OwnerExpenseCache {
    private final RedisCacheManager expenseCacheManager;
    @Value("${app.cache.expenses-enabled:true}")
    private boolean enabled;

    @SuppressWarnings("unchecked")
    public <T> T read(String username, String key, Supplier<T> loader) {
        if (!enabled) return loader.get();
        Cache cache = ownerCache(username);
        try {
            Cache.ValueWrapper cached = cache.get(key);
            if (cached != null) return (T) cached.get();
        } catch (DataAccessException error) {
            log.warn("Redis non disponibile per la lettura della cache spese");
            return loader.get();
        }
        T result = loader.get();
        try {
            cache.put(key, result);
        } catch (DataAccessException error) {
            log.warn("Redis non disponibile per la scrittura della cache spese");
        }
        return result;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidate(OwnerExpensesChanged event) {
        if (!enabled) return;
        try {
            ownerCache(event.username()).clear();
        } catch (DataAccessException error) {
            log.warn("Invalidazione Redis delle spese fallita; scadenza massima cache: due minuti");
        }
    }

    private Cache ownerCache(String username) {
        try {
            String owner = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(username.getBytes(StandardCharsets.UTF_8)));
            return Objects.requireNonNull(expenseCacheManager.getCache("owner-expenses-" + owner));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 non disponibile", error);
        }
    }
}
