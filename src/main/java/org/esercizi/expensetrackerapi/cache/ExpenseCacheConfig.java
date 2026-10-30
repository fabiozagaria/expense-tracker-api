package org.esercizi.expensetrackerapi.cache;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class ExpenseCacheConfig {
    @Bean
    public RedisCacheManager expenseCacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(
                        factory, BatchStrategies.scan(1000)))
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .prefixCacheNameWith("expense-tracker:v1:")
                        .entryTtl(Duration.ofMinutes(2))
                        .disableCachingNullValues())
                .build();
    }
}
