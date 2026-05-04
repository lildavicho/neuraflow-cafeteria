package com.ucacue.bar.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                cache("firebase-tokens", Duration.ofMinutes(10), 5_000),
                cache("products-list", Duration.ofSeconds(45), 1_000),
                cache("products-public", Duration.ofSeconds(60), 1_000),
                cache("products-low-stock", Duration.ofSeconds(45), 500),
                cache("dashboard-snapshots", Duration.ofSeconds(45), 250),
                cache("sales-reports", Duration.ofMinutes(5), 250),
                cache("sri-health", Duration.ofSeconds(30), 250),
                cache("ride-template", Duration.ofMinutes(2), 250),
                cache("accounting-reference", Duration.ofSeconds(60), 1_000),
                cache("tenant-plans", Duration.ofMinutes(5), 500),
                cache("cash-register-active", Duration.ofSeconds(10), 250)
        ));
        return manager;
    }

    private CaffeineCache cache(String name, Duration ttl, long maximumSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(ttl)
                .recordStats()
                .build());
    }
}
