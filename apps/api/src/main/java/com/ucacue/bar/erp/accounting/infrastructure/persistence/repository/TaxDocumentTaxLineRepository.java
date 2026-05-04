package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentTaxLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxDocumentTaxLineRepository extends JpaRepository<TaxDocumentTaxLineEntity, Long> {
    List<TaxDocumentTaxLineEntity> findByTaxDocumentId(Long taxDocumentId);
}
