package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingSupplierRegime;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingTaxType;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingMatrixEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WithholdingMatrixRepository extends JpaRepository<WithholdingMatrixEntity, Long> {

    List<WithholdingMatrixEntity> findByTenantIdAndActiveTrueOrderByConceptCodeAsc(Long tenantId);

    List<WithholdingMatrixEntity> findByTenantIdAndSupplierRegimeAndActiveTrue(
            Long tenantId, WithholdingSupplierRegime regime);

    Optional<WithholdingMatrixEntity> findByTenantIdAndConceptCodeAndSupplierRegimeAndTaxType(
            Long tenantId, String conceptCode, WithholdingSupplierRegime regime, WithholdingTaxType taxType);
}
