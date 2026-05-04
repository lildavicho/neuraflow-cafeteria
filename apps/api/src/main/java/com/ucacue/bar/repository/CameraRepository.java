package com.ucacue.bar.repository;

import com.ucacue.bar.entity.CameraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CameraRepository extends JpaRepository<CameraEntity, Long> {
    boolean existsByNameIgnoreCase(String name);

    List<CameraEntity> findAllByOrderByNameAsc();

    // ── Tenant-filtered queries ──

    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    List<CameraEntity> findByTenantIdOrderByNameAsc(Long tenantId);
    java.util.Optional<CameraEntity> findByTenantIdAndId(Long tenantId, Long id);
}
