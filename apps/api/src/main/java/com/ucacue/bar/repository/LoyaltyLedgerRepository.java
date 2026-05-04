package com.ucacue.bar.repository;

import com.ucacue.bar.entity.LoyaltyLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyLedgerRepository extends JpaRepository<LoyaltyLedgerEntity, Long> {
    List<LoyaltyLedgerEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    // ── Tenant-filtered queries ──

    List<LoyaltyLedgerEntity> findByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, Long userId);
}
