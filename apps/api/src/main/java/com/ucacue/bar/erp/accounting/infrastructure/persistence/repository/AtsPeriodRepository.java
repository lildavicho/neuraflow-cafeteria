package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AtsPeriodEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtsPeriodRepository extends JpaRepository<AtsPeriodEntity, Long> {

    Optional<AtsPeriodEntity> findByTenantIdAndId(Long tenantId, Long id);

    Optional<AtsPeriodEntity> findByTenantIdAndFiscalYearAndFiscalMonth(Long tenantId, Integer fiscalYear, Integer fiscalMonth);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AtsPeriodEntity p where p.tenantId = :tenantId " +
           "and p.fiscalYear = :fiscalYear and p.fiscalMonth = :fiscalMonth")
    Optional<AtsPeriodEntity> findForUpdate(@Param("tenantId") Long tenantId,
                                            @Param("fiscalYear") Integer fiscalYear,
                                            @Param("fiscalMonth") Integer fiscalMonth);

    List<AtsPeriodEntity> findByTenantIdOrderByFiscalYearDescFiscalMonthDesc(Long tenantId);
}
