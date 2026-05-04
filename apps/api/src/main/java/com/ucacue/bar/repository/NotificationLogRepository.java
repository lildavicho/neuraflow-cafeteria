package com.ucacue.bar.repository;

import com.ucacue.bar.entity.NotificationLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, Long> {

    // ── Tenant-filtered queries ──

    Page<NotificationLogEntity> findByTenantIdOrderBySentAtDesc(Long tenantId, Pageable pageable);
}
