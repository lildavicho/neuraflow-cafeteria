package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.ProductTaxRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductTaxRuleRepository extends JpaRepository<ProductTaxRuleEntity, Long> {
    List<ProductTaxRuleEntity> findByTenantIdAndActiveTrue(Long tenantId);

    List<ProductTaxRuleEntity> findByTenantIdAndProductIdInAndActiveTrue(Long tenantId, List<Long> productIds);

    Optional<ProductTaxRuleEntity> findByTenantIdAndProductIdAndActiveTrue(Long tenantId, Long productId);
}
