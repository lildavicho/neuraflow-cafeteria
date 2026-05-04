package com.ucacue.bar.repository;

import com.ucacue.bar.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    Page<AuditLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AuditLogEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId, Pageable pageable);

    Page<AuditLogEntity> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    Page<AuditLogEntity> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    Page<AuditLogEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ── Tenant-filtered queries ──

    Page<AuditLogEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<AuditLogEntity> findByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, Long userId, Pageable pageable);

    Page<AuditLogEntity> findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            Long tenantId, String entityType, Long entityId, Pageable pageable);

    Page<AuditLogEntity> findByTenantIdAndEntityTypeOrderByCreatedAtDesc(Long tenantId, String entityType, Pageable pageable);

    Page<AuditLogEntity> findByTenantIdAndActionOrderByCreatedAtDesc(Long tenantId, String action, Pageable pageable);

    Page<AuditLogEntity> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long tenantId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
