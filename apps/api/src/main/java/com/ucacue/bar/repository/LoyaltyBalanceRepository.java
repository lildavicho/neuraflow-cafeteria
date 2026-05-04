package com.ucacue.bar.repository;

import com.ucacue.bar.entity.LoyaltyBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoyaltyBalanceRepository extends JpaRepository<LoyaltyBalanceEntity, Long> {
    Optional<LoyaltyBalanceEntity> findByUserId(Long userId);

    // ── Tenant-filtered queries ──

    Optional<LoyaltyBalanceEntity> findByTenantIdAndUserId(Long tenantId, Long userId);
}
