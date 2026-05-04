package com.ucacue.bar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class DashboardSnapshotCacheStore {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DashboardSnapshotCacheStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> findFresh(Long tenantId, LocalDateTime now) {
        try {
            List<String> rows = jdbcTemplate.query(
                    "SELECT payload::text FROM dashboard_snapshot_cache WHERE tenant_id = ? AND expires_at > ?",
                    ps -> {
                        ps.setLong(1, tenantId);
                        ps.setObject(2, now);
                    },
                    (rs, rowNum) -> rs.getString(1));
            if (rows.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(rows.get(0), MAP_TYPE));
        } catch (Exception ex) {
            log.debug("Dashboard snapshot cache read skipped for tenant {}: {}", tenantId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Long tenantId,
                     String tenantCode,
                     Map<String, Object> payload,
                     LocalDateTime computedAt,
                     LocalDateTime expiresAt) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            jdbcTemplate.update("""
                    INSERT INTO dashboard_snapshot_cache
                        (tenant_id, tenant_code, payload, computed_at, expires_at, updated_at)
                    VALUES (?, ?, ?::jsonb, ?, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (tenant_id) DO UPDATE SET
                        tenant_code = EXCLUDED.tenant_code,
                        payload = EXCLUDED.payload,
                        computed_at = EXCLUDED.computed_at,
                        expires_at = EXCLUDED.expires_at,
                        updated_at = CURRENT_TIMESTAMP
                    """,
                    tenantId,
                    tenantCode,
                    json,
                    computedAt,
                    expiresAt);
        } catch (Exception ex) {
            log.debug("Dashboard snapshot cache write skipped for tenant {}: {}", tenantId, ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateAll() {
        try {
            jdbcTemplate.update("UPDATE dashboard_snapshot_cache SET expires_at = CURRENT_TIMESTAMP");
        } catch (DataAccessException ex) {
            log.debug("Dashboard snapshot cache invalidation skipped: {}", ex.getMessage());
        }
    }
}
