package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.DocumentSequenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequenceEntity, Long> {
    Optional<DocumentSequenceEntity> findByTenantIdAndId(Long tenantId, Long id);

    @EntityGraph(attributePaths = "documentType")
    List<DocumentSequenceEntity> findByTenantIdOrderByDocumentTypeDocumentCodeAscEstablishmentCodeAscEmissionPointCodeAsc(Long tenantId);

    Optional<DocumentSequenceEntity> findByTenantIdAndDocumentTypeDocumentCodeAndActiveTrue(Long tenantId, String documentCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentSequenceEntity> findWithLockByTenantIdAndId(Long tenantId, Long id);
}
