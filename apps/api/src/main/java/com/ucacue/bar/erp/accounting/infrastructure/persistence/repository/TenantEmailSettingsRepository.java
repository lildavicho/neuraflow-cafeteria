package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TenantEmailSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantEmailSettingsRepository extends JpaRepository<TenantEmailSettingsEntity, Long> {
    Optional<TenantEmailSettingsEntity> findByTenantIdAndActiveTrue(Long tenantId);
}
