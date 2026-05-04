package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.SriConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SriConfigurationRepository extends JpaRepository<SriConfigurationEntity, Long> {
    Optional<SriConfigurationEntity> findByTenantId(Long tenantId);
}
