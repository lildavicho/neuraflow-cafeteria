package com.ucacue.bar.repository;

import com.ucacue.bar.entity.PushTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushTokenEntity, Long> {
    Optional<PushTokenEntity> findByFcmToken(String fcmToken);
    Optional<PushTokenEntity> findByTenantIdAndFcmToken(Long tenantId, String fcmToken);

    List<PushTokenEntity> findByUserId(Long userId);

    List<PushTokenEntity> findByUserIdAndActiveTrue(Long userId);

    // ── Tenant-filtered queries ──

    List<PushTokenEntity> findByTenantIdAndUserIdAndActiveTrue(Long tenantId, Long userId);
}
