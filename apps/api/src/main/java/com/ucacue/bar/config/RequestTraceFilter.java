package com.ucacue.bar.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_KEY = "traceId";

    @Value("${app.performance.slow-request-threshold-ms:3000}")
    private long slowRequestThresholdMs;

    @Value("${app.performance.query-count-enabled:false}")
    private boolean queryCountEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        long startedAt = System.nanoTime();
        if (queryCountEnabled) {
            SqlQueryCounter.reset();
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            response.setHeader("X-Response-Time-ms", Long.toString(elapsedMs));
            if (elapsedMs >= slowRequestThresholdMs) {
                if (queryCountEnabled) {
                    log.warn("Slow request {} {} completed in {} ms status={} traceId={} user={} queries={}",
                            request.getMethod(),
                            request.getRequestURI(),
                            elapsedMs,
                            response.getStatus(),
                            traceId,
                            resolveUser(request),
                            SqlQueryCounter.getCount());
                } else {
                    log.warn("Slow request {} {} completed in {} ms status={} traceId={} user={}",
                            request.getMethod(),
                            request.getRequestURI(),
                            elapsedMs,
                            response.getStatus(),
                            traceId,
                            resolveUser(request));
                }
            }
            if (queryCountEnabled) {
                SqlQueryCounter.clear();
            }
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String resolveUser(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return principal != null && principal.getName() != null ? principal.getName() : "anonymous";
    }
}
