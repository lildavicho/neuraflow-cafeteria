package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingEntryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountingEntryRepository extends JpaRepository<AccountingEntryEntity, Long> {
    List<AccountingEntryEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<AccountingEntryEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Optional<AccountingEntryEntity> findByTenantIdAndSourceTypeAndSourceId(Long tenantId, String sourceType, Long sourceId);
}
