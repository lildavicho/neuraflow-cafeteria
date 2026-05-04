package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingEntryLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AccountingEntryLineRepository extends JpaRepository<AccountingEntryLineEntity, Long> {
    List<AccountingEntryLineEntity> findByEntryIdOrderByLineNumberAsc(Long entryId);

    List<AccountingEntryLineEntity> findByEntryIdInOrderByEntryIdAscLineNumberAsc(Collection<Long> entryIds);
}
