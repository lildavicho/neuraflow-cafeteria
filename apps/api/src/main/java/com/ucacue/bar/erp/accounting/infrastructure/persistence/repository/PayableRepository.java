package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.PayableEntity;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.PayableStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PayableRepository extends JpaRepository<PayableEntity, Long> {
    Optional<PayableEntity> findByTenantIdAndId(Long tenantId, Long id);

    List<PayableEntity> findByTenantIdOrderByIssueDateDescCreatedAtDesc(Long tenantId);

    List<PayableEntity> findByTenantIdOrderByIssueDateDescCreatedAtDesc(Long tenantId, Pageable pageable);

    Optional<PayableEntity> findByTenantIdAndSourceDocumentTypeAndSourceDocumentId(Long tenantId, String sourceDocumentType, Long sourceDocumentId);

    long countByTenantIdAndStatusIn(Long tenantId, Collection<PayableStatus> statuses);
}
