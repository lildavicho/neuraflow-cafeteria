package com.ucacue.bar.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Component
public class RateLimitingConfig {
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, this::newBucket);
    }
    
    private Bucket newBucket(String key) {
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
