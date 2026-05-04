package com.ucacue.bar.erp.approval.infrastructure.persistence.repository;

import com.ucacue.bar.erp.approval.infrastructure.persistence.entity.ApprovalDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalDefinitionRepository extends JpaRepository<ApprovalDefinitionEntity, Long> {

    List<ApprovalDefinitionEntity> findByTenantIdAndActiveTrue(Long tenantId);

    Optional<ApprovalDefinitionEntity> findByIdAndTenantId(Long id, Long tenantId);

    Optional<ApprovalDefinitionEntity> findByTenantIdAndCode(Long tenantId, String code);

    @Query("""
        SELECT d FROM ApprovalDefinitionEntity d
        LEFT JOIN FETCH d.steps
        WHERE d.tenantId = :tenantId
          AND d.entityType = :entityType
          AND d.active = true
    """)
    List<ApprovalDefinitionEntity> findActiveByEntityType(
            @Param("tenantId") Long tenantId,
            @Param("entityType") String entityType);
}
