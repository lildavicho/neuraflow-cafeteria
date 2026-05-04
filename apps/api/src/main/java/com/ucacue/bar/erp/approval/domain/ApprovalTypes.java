package com.ucacue.bar.erp.approval.domain;

public final class ApprovalTypes {

    private ApprovalTypes() {}

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED, CANCELLED
    }

    public enum ApprovalAction {
        APPROVE, REJECT, COMMENT
    }

    public enum EntityType {
        PURCHASE_ORDER, CREDIT_NOTE, INVENTORY_ADJUST, DISCOUNT, EXPENSE, OTHER
    }
}
