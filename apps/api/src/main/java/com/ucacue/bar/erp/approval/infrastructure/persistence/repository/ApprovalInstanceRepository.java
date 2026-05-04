package com.ucacue.bar.erp.approval.infrastructure.persistence.repository;

import com.ucacue.bar.erp.approval.domain.ApprovalTypes.ApprovalStatus;
import com.ucacue.bar.erp.approval.infrastructure.persistence.entity.ApprovalInstanceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstanceEntity, Long> {

    Optional<ApprovalInstanceEntity> findByIdAndTenantId(Long id, Long tenantId);

    Optional<ApprovalInstanceEntity> findByTenantIdAndEntityTypeAndEntityId(
            Long tenantId, String entityType, Long entityId);

    Page<ApprovalInstanceEntity> findByTenantIdAndStatus(
            Long tenantId, ApprovalStatus status, Pageable pageable);

    @Query("""
        SELECT i FROM ApprovalInstanceEntity i
        JOIN FETCH i.definition d
        WHERE i.tenantId = :tenantId
          AND i.status = :status
        ORDER BY i.createdAt DESC
    """)
    Page<ApprovalInstanceEntity> findPendingWithDefinition(
            @Param("tenantId") Long tenantId,
            @Param("status") ApprovalStatus status,
            Pageable pageable);

    long countByTenantIdAndStatus(Long tenantId, ApprovalStatus status);
}
