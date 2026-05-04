package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.RideTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RideTemplateRepository extends JpaRepository<RideTemplateEntity, Long> {

    Optional<RideTemplateEntity> findFirstByTenantIdAndActiveTrueOrderByCustomDescUpdatedAtDesc(Long tenantId);

    Optional<RideTemplateEntity> findByTenantIdAndTemplateCodeIgnoreCase(Long tenantId, String templateCode);

    List<RideTemplateEntity> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);
}
