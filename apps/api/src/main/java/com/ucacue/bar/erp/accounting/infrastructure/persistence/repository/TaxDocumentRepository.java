package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaxDocumentRepository extends JpaRepository<TaxDocumentEntity, Long> {
    Optional<TaxDocumentEntity> findByTenantIdAndId(Long tenantId, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TaxDocumentEntity t where t.tenantId = :tenantId and t.id = :id")
    Optional<TaxDocumentEntity> findByTenantIdAndIdForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @EntityGraph(attributePaths = "documentType")
    List<TaxDocumentEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @EntityGraph(attributePaths = "documentType")
    List<TaxDocumentEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    List<TaxDocumentEntity> findByTenantIdAndStatusAndIssueDateBetweenOrderByIssueDateAsc(
            Long tenantId, TaxDocumentStatus status, LocalDate from, LocalDate to);

    Optional<TaxDocumentEntity> findByTenantIdAndSourceTypeAndSourceId(Long tenantId, String sourceType, Long sourceId);

    Optional<TaxDocumentEntity> findTopByTenantIdOrderByCreatedAtDesc(Long tenantId);

    long countByTenantIdAndStatusIn(Long tenantId, Collection<TaxDocumentStatus> statuses);

    long countByTenantIdAndStatus(Long tenantId, TaxDocumentStatus status);

    long countByTenantIdAndRideEmailStatus(Long tenantId, String rideEmailStatus);

    long countByTenantIdAndStatusAndRidePdfPathIsNull(Long tenantId, TaxDocumentStatus status);

    @Query("select t.status, count(t) from TaxDocumentEntity t where t.tenantId = :tenantId group by t.status")
    List<Object[]> countByStatusGrouped(@Param("tenantId") Long tenantId);

    @Query("select t.rideEmailStatus, count(t) from TaxDocumentEntity t " +
           "where t.tenantId = :tenantId and t.rideEmailStatus in :statuses " +
           "group by t.rideEmailStatus")
    List<Object[]> countByRideEmailStatusGrouped(@Param("tenantId") Long tenantId,
                                                 @Param("statuses") Collection<String> statuses);

    @Query(value = "SELECT " +
                   "COALESCE(SUM(CASE WHEN status = 'DRAFT' THEN 1 ELSE 0 END), 0) AS draft_count, " +
                   "COALESCE(SUM(CASE WHEN status = 'PENDING_VALIDATION' THEN 1 ELSE 0 END), 0) AS pending_validation_count, " +
                   "COALESCE(SUM(CASE WHEN status = 'READY_TO_SEND' THEN 1 ELSE 0 END), 0) AS ready_to_send_count, " +
                   "COALESCE(SUM(CASE WHEN status = 'SENT' THEN 1 ELSE 0 END), 0) AS sent_count, " +
                   "COALESCE(SUM(CASE WHEN status = 'AUTHORIZED' THEN 1 ELSE 0 END), 0) AS authorized_count, " +
                   "COALESCE(SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END), 0) AS rejected_count, " +
                   "COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled_count, " +
                   "COALESCE(SUM(CASE WHEN status = 'AUTHORIZED' AND ride_pdf_path IS NULL THEN 1 ELSE 0 END), 0) AS authorized_awaiting_pdf, " +
                   "COALESCE(SUM(CASE WHEN ride_email_status = 'RETRY_PENDING' THEN 1 ELSE 0 END), 0) AS email_retry_pending, " +
                   "COALESCE(SUM(CASE WHEN ride_email_status = 'PDF_FAILED' THEN 1 ELSE 0 END), 0) AS email_pdf_failed, " +
                   "COALESCE(SUM(CASE WHEN ride_email_status = 'NO_CUSTOMER_EMAIL' THEN 1 ELSE 0 END), 0) AS email_no_customer_email, " +
                   "COALESCE(SUM(CASE WHEN ride_email_status = 'SENT' THEN 1 ELSE 0 END), 0) AS email_sent " +
                   "FROM tax_documents " +
                   "WHERE tenant_id = :tenantId",
           nativeQuery = true)
    Object[] healthSummaryByTenantId(@Param("tenantId") Long tenantId);

    @EntityGraph(attributePaths = "documentType")
    List<TaxDocumentEntity> findTop5ByTenantIdAndStatusInOrderByCreatedAtDesc(Long tenantId, Collection<TaxDocumentStatus> statuses);

    @EntityGraph(attributePaths = "documentType")
    List<TaxDocumentEntity> findTop20ByTenantIdAndStatusInAndLastStatusAtBeforeOrderByLastStatusAtAsc(
            Long tenantId, Collection<TaxDocumentStatus> statuses, LocalDateTime threshold);

    List<TaxDocumentEntity> findTop20ByTenantIdAndStatusAndRidePdfPathIsNullOrderByCreatedAtAsc(
            Long tenantId, TaxDocumentStatus status);

    List<TaxDocumentEntity> findTop20ByRideEmailStatusAndRideEmailNextRetryAtLessThanEqualOrderByRideEmailNextRetryAtAsc(
            String rideEmailStatus,
            LocalDateTime now);
}
