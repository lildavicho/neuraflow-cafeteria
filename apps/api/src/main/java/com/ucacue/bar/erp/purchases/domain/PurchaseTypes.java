package com.ucacue.bar.erp.purchases.domain;

public final class PurchaseTypes {

    private PurchaseTypes() {
    }

    public enum PurchaseStatus {
        PENDING_APPROVAL,
        DRAFT,
        RECEIVED,
        PAID,
        CANCELLED
    }
}
