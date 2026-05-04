package com.ucacue.bar.erp.commercial.infrastructure.persistence.repository;

import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<TenantEntity, Long> {
    Optional<TenantEntity> findByTenantCodeIgnoreCase(String tenantCode);

    List<TenantEntity> findByActiveTrue();
}
