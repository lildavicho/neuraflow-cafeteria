package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountCatalogRepository extends JpaRepository<AccountCatalogEntity, Long> {
    Optional<AccountCatalogEntity> findByTenantIdAndId(Long tenantId, Long id);

    List<AccountCatalogEntity> findByTenantIdOrderByAccountCodeAsc(Long tenantId);

    Optional<AccountCatalogEntity> findByTenantIdAndAccountCode(Long tenantId, String accountCode);
}
