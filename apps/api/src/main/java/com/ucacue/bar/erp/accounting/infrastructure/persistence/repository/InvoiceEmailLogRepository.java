package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.InvoiceEmailLogEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceEmailLogRepository extends JpaRepository<InvoiceEmailLogEntity, Long> {

    List<InvoiceEmailLogEntity> findByTenantIdAndTaxDocumentIdOrderByCreatedAtDesc(Long tenantId, Long taxDocumentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InvoiceEmailLogEntity> findFirstByProviderMessageIdOrderByCreatedAtDesc(String providerMessageId);

    long countByTenantIdAndTaxDocumentId(Long tenantId, Long taxDocumentId);

    @Query("select l from InvoiceEmailLogEntity l " +
            "where l.tenantId = :tenantId " +
            "and (:status is null or l.status = :status) " +
            "and (:from is null or l.createdAt >= :from) " +
            "and (:to is null or l.createdAt < :to) " +
            "order by l.createdAt desc")
    List<InvoiceEmailLogEntity> searchByTenant(@Param("tenantId") Long tenantId,
                                               @Param("from") OffsetDateTime from,
                                               @Param("to") OffsetDateTime to,
                                               @Param("status") String status,
                                               Pageable pageable);

    @Query("select l.status, count(l) from InvoiceEmailLogEntity l " +
            "where l.tenantId = :tenantId " +
            "and l.createdAt >= :from and l.createdAt < :to " +
            "group by l.status")
    List<Object[]> countByStatus(@Param("tenantId") Long tenantId,
                                 @Param("from") OffsetDateTime from,
                                 @Param("to") OffsetDateTime to);

    @Query(value = "select cast(created_at as date) as day, count(*) as total " +
            "from invoice_email_logs " +
            "where tenant_id = :tenantId " +
            "and created_at >= :from and created_at < :to " +
            "group by cast(created_at as date) " +
            "order by cast(created_at as date)", nativeQuery = true)
    List<Object[]> countByDay(@Param("tenantId") Long tenantId,
                              @Param("from") OffsetDateTime from,
                              @Param("to") OffsetDateTime to);
}
