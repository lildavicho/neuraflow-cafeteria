package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingDocumentStatus;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WithholdingDocumentRepository extends JpaRepository<WithholdingDocumentEntity, Long> {

    Optional<WithholdingDocumentEntity> findByTenantIdAndId(Long tenantId, Long id);

    List<WithholdingDocumentEntity> findByTenantIdAndPurchaseId(Long tenantId, Long purchaseId);

    List<WithholdingDocumentEntity> findByTenantIdAndStatusOrderByIssueDateDesc(
            Long tenantId, WithholdingDocumentStatus status);

    List<WithholdingDocumentEntity> findByTenantIdOrderByIssueDateDesc(Long tenantId);

    List<WithholdingDocumentEntity> findByTenantIdAndIssueDateBetweenOrderByIssueDateAsc(
            Long tenantId, java.time.LocalDate from, java.time.LocalDate to);
}
