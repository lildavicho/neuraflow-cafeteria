package com.ucacue.bar.repository;

import com.ucacue.bar.entity.UserPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, Long> {
    Optional<UserPreferenceEntity> findByUserId(Long userId);

    // ── Tenant-filtered queries ──

    Optional<UserPreferenceEntity> findByTenantIdAndUserId(Long tenantId, Long userId);
}
