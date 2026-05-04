package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingDocumentTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountingDocumentTypeRepository extends JpaRepository<AccountingDocumentTypeEntity, Long> {
    Optional<AccountingDocumentTypeEntity> findByTenantIdAndId(Long tenantId, Long id);

    List<AccountingDocumentTypeEntity> findByTenantIdOrderByDocumentCodeAsc(Long tenantId);

    Optional<AccountingDocumentTypeEntity> findByTenantIdAndDocumentCode(Long tenantId, String documentCode);
}
