package com.ucacue.bar.erp.commercial.infrastructure.persistence.repository;

import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantFeatureFlagEntity;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantFeatureFlagRepository extends JpaRepository<TenantFeatureFlagEntity, Long> {
    List<TenantFeatureFlagEntity> findAllByTenantId(Long tenantId);

    Optional<TenantFeatureFlagEntity> findByTenantIdAndModuleCode(Long tenantId, ModuleCode moduleCode);
}
