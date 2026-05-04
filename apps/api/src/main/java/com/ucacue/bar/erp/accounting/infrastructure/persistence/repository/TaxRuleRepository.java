package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxScope;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxRuleRepository extends JpaRepository<TaxRuleEntity, Long> {
    Optional<TaxRuleEntity> findByTenantIdAndId(Long tenantId, Long id);

    List<TaxRuleEntity> findByTenantIdOrderByTaxCodeAsc(Long tenantId);

    List<TaxRuleEntity> findByTenantIdAndActiveTrueOrderByTaxCodeAsc(Long tenantId);

    List<TaxRuleEntity> findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(Long tenantId, TaxScope taxScope);

    Optional<TaxRuleEntity> findByTenantIdAndTaxCode(Long tenantId, String taxCode);
}
