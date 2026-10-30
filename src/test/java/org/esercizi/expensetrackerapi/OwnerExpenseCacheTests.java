package org.esercizi.expensetrackerapi;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.esercizi.expensetrackerapi.cache.OwnerExpenseCache;
import org.esercizi.expensetrackerapi.cache.OwnerExpensesChanged;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OwnerExpenseCacheTests {
    @Test
    void serializesTheResponseGraphUsedByRedis() {
        var expense = new org.esercizi.expensetrackerapi.dto.expense.ExpenseResponse(
                1L, "Spesa", java.math.BigDecimal.TEN, "",
                org.esercizi.expensetrackerapi.model.expense.ExpenseCategory.other,
                java.time.LocalDate.of(2026, 10, 30));
        var page = new org.esercizi.expensetrackerapi.dto.MovementPage<>(
                java.util.List.of(expense), 0, 10, 1, 1);
        var serializer = new org.springframework.data.redis.serializer.JdkSerializationRedisSerializer();
        assertThat(serializer.deserialize(serializer.serialize(page))).isEqualTo(page);
        var list = new java.util.ArrayList<>(java.util.List.of(expense));
        assertThat(serializer.deserialize(serializer.serialize(list))).isEqualTo(list);
    }

    @Test
    void cachesRepeatedReadsAndInvalidatesOnlyTheChangedOwner() {
        var manager = mock(RedisCacheManager.class);
        var caches = new java.util.HashMap<String, ConcurrentMapCache>();
        when(manager.getCache(anyString())).thenAnswer(call -> caches.computeIfAbsent(
                call.getArgument(0), ConcurrentMapCache::new));
        var cache = new OwnerExpenseCache(manager);
        ReflectionTestUtils.setField(cache, "enabled", true);
        var loads = new AtomicInteger();

        assertThat(cache.read("alice", "all", loads::incrementAndGet)).isEqualTo(1);
        assertThat(cache.read("alice", "all", loads::incrementAndGet)).isEqualTo(1);
        assertThat(cache.read("bob", "all", loads::incrementAndGet)).isEqualTo(2);
        assertThat(cache.read("alice", "page:1", loads::incrementAndGet)).isEqualTo(3);
        cache.invalidate(new OwnerExpensesChanged("alice"));
        assertThat(cache.read("bob", "all", loads::incrementAndGet)).isEqualTo(2);
        assertThat(cache.read("alice", "all", loads::incrementAndGet)).isEqualTo(4);
        assertThat(cache.read("alice", "page:1", loads::incrementAndGet)).isEqualTo(5);
    }

    @Test
    void readsDatabaseWhenRedisIsUnavailable() {
        var manager = mock(RedisCacheManager.class);
        var redisCache = mock(org.springframework.cache.Cache.class);
        when(manager.getCache(anyString())).thenReturn(redisCache);
        when(redisCache.get("all")).thenThrow(new RedisConnectionFailureException("offline"));
        var cache = new OwnerExpenseCache(manager);
        ReflectionTestUtils.setField(cache, "enabled", true);

        assertThat(cache.read("alice", "all", () -> "database")).isEqualTo("database");
        verify(redisCache, never()).put(any(), any());
    }
}
