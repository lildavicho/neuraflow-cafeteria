package com.ucacue.bar.erp.approval.application;

import com.ucacue.bar.erp.approval.domain.ApprovalTypes.ApprovalStatus;

public record ApprovalStatusChangedEvent(
        Long tenantId,
        Long instanceId,
        String entityType,
        Long entityId,
        ApprovalStatus status
) {}
