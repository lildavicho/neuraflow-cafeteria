package com.ucacue.bar.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Configuration
@Component
public class RateLimitingConfig {

    private static final int DEFAULT_VISION_RATE_LIMIT_PER_MINUTE = 1800;

    @Value("${vision.rate-limit.per-minute:${VISION_RATE_LIMIT_PER_MINUTE:1800}}")
    private int visionRateLimitPerMinute = DEFAULT_VISION_RATE_LIMIT_PER_MINUTE;

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(15))
            .maximumSize(50_000)
            .build();
    
    public Bucket resolveBucket(String key) {
        return cache.get(key, this::newBucket);
    }
    
    private Bucket newBucket(String key) {
        // Vision ingestion can be high-volume but should still be bounded per API key.
        if (key.startsWith("vision:")) {
            int limitPerMinute = Math.max(1, visionRateLimitPerMinute);
            Bandwidth limit = Bandwidth.builder()
                .capacity(limitPerMinute)
                .refillGreedy(limitPerMinute, Duration.ofMinutes(1))
                .build();
            return Bucket.builder()
                .addLimit(limit)
                .build();
        }

        // 5 requests per minute for login endpoints
        if (key.contains("login")) {
            Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();
            return Bucket.builder()
                .addLimit(limit)
                .build();
        }

        if (key.contains("/public/leads")) {
            Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(1))
                .build();
            return Bucket.builder()
                .addLimit(limit)
                .build();
        }

        // Dashboard can be refreshed manually, but should not be hammered.
        if (key.contains("/dashboard")) {
            Bandwidth limit = Bandwidth.builder()
                .capacity(50)
                .refillGreedy(50, Duration.ofMinutes(1))
                .build();
            return Bucket.builder()
                .addLimit(limit)
                .build();
        }

        // Reports aggregate over historical ranges; keep them below general API traffic.
        if (key.contains("/erp/reports")) {
            Bandwidth limit = Bandwidth.builder()
                .capacity(100)
                .refillGreedy(100, Duration.ofMinutes(1))
                .build();
            return Bucket.builder()
                .addLimit(limit)
                .build();
        }
        
        // 100 requests per minute for general API calls
        Bandwidth limit = Bandwidth.builder()
            .capacity(100)
            .refillGreedy(100, Duration.ofMinutes(1))
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
}
