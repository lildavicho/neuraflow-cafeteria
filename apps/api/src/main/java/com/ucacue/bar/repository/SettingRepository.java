package com.ucacue.bar.repository;

import com.ucacue.bar.entity.SettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettingRepository extends JpaRepository<SettingEntity, Long> {
    Optional<SettingEntity> findByKey(String key);

    // ── Tenant-filtered queries ──

    Optional<SettingEntity> findByTenantIdAndKey(Long tenantId, String key);

    List<SettingEntity> findByTenantId(Long tenantId);
}
