package com.ucacue.bar.erp.commercial.infrastructure.persistence.repository;

import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantPlanSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantPlanSubscriptionRepository extends JpaRepository<TenantPlanSubscriptionEntity, Long> {
    Optional<TenantPlanSubscriptionEntity> findFirstByTenantIdAndStatusOrderByStartsAtDesc(
            Long tenantId,
            TenantPlanSubscriptionEntity.SubscriptionStatus status);
}
