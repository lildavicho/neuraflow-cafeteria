package com.ucacue.bar.security;

import com.ucacue.bar.config.RateLimitingConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingConfig rateLimitingConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/actuator/health")
            || path.startsWith("/swagger")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/css")
            || path.startsWith("/js")
            || path.startsWith("/images")
            || path.startsWith("/ws")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = rateLimitKey(request, path);
        Bucket bucket = rateLimitingConfig.resolveBucket(key);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for {}", key);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds(probe)));
            response.setStatus(429);
        }
    }

    private long retryAfterSeconds(ConsumptionProbe probe) {
        long nanosToWait = probe.getNanosToWaitForRefill();
        if (nanosToWait <= 0) {
            return 60;
        }
        long seconds = Duration.ofNanos(nanosToWait).toSeconds();
        if (Duration.ofNanos(nanosToWait).minusSeconds(seconds).isZero()) {
            return Math.max(1, seconds);
        }
        return Math.max(1, seconds + 1);
    }

    private String rateLimitKey(HttpServletRequest request, String path) {
        if (("/vision/events".equals(path) || "/api/vision/events".equals(path))
                && "POST".equalsIgnoreCase(request.getMethod())) {
            String apiKey = request.getHeader("X-Vision-Api-Key");
            if (apiKey != null && !apiKey.isBlank()) {
                return "vision:" + sha256Prefix(apiKey.trim());
            }
            return "vision:missing:" + request.getRemoteAddr();
        }
        return request.getRemoteAddr() + ":" + path;
    }

    private String sha256Prefix(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception ex) {
            return "hash-unavailable";
        }
    }
}
