package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentTransmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxDocumentTransmissionRepository extends JpaRepository<TaxDocumentTransmissionEntity, Long> {
    List<TaxDocumentTransmissionEntity> findByTaxDocumentIdOrderByAttemptedAtDesc(Long taxDocumentId);
    Optional<TaxDocumentTransmissionEntity> findByIdAndTaxDocumentId(Long id, Long taxDocumentId);
    Optional<TaxDocumentTransmissionEntity> findTopByTaxDocumentIdOrderByAttemptedAtDesc(Long taxDocumentId);
    Optional<TaxDocumentTransmissionEntity> findTopByTaxDocumentIdAndPhaseOrderByAttemptedAtDesc(
            Long taxDocumentId,
            TaxDocumentTransmissionEntity.TransmissionPhase phase);
    long countByTaxDocumentIdAndPhase(Long taxDocumentId, TaxDocumentTransmissionEntity.TransmissionPhase phase);
}
