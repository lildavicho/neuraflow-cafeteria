package com.ucacue.bar.erp.approval.infrastructure.persistence.repository;

import com.ucacue.bar.erp.approval.domain.ApprovalTypes;
import com.ucacue.bar.erp.approval.infrastructure.persistence.entity.ApprovalActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalActionRepository extends JpaRepository<ApprovalActionEntity, Long> {

    List<ApprovalActionEntity> findByInstanceIdOrderByCreatedAtAsc(Long instanceId);

    long countByInstanceIdAndStepOrderAndAction(Long instanceId, Integer stepOrder, ApprovalTypes.ApprovalAction action);
}
