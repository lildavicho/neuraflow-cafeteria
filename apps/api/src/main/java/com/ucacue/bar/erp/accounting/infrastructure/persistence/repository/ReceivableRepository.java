package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.ReceivableEntity;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.ReceivableStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReceivableRepository extends JpaRepository<ReceivableEntity, Long> {
    Optional<ReceivableEntity> findByTenantIdAndId(Long tenantId, Long id);

    List<ReceivableEntity> findByTenantIdOrderByIssueDateDescCreatedAtDesc(Long tenantId);

    List<ReceivableEntity> findByTenantIdOrderByIssueDateDescCreatedAtDesc(Long tenantId, Pageable pageable);

    Optional<ReceivableEntity> findByTenantIdAndSourceDocumentTypeAndSourceDocumentId(Long tenantId, String sourceDocumentType, Long sourceDocumentId);

    long countByTenantIdAndStatusIn(Long tenantId, Collection<ReceivableStatus> statuses);
}
