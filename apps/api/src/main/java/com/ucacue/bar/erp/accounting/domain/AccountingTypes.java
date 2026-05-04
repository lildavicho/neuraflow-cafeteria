package com.ucacue.bar.erp.accounting.domain;

public final class AccountingTypes {

    private AccountingTypes() {
    }

    public enum AccountType {
        ASSET,
        LIABILITY,
        EQUITY,
        REVENUE,
        EXPENSE
    }

    public enum AccountNature {
        DEBIT,
        CREDIT
    }

    public enum AccountingEntryStatus {
        DRAFT,
        POSTED,
        REVERSED,
        CANCELLED
    }

    public enum AccountingSourceModule {
        POS,
        SALES,
        PURCHASES,
        CASH,
        ACCOUNTING,
        TAXATION,
        SRI
    }

    public enum ReceivableStatus {
        OPEN,
        PARTIALLY_PAID,
        PAID,
        OVERDUE,
        CANCELLED
    }

    public enum PayableStatus {
        OPEN,
        PARTIALLY_PAID,
        PAID,
        OVERDUE,
        CANCELLED
    }

    public enum TaxScope {
        PRODUCT,
        DOCUMENT
    }

    public enum TaxComputationType {
        PERCENTAGE,
        FIXED
    }

    public enum FiscalDomain {
        ACCOUNTING,
        TAXATION,
        SRI
    }

    public enum DocumentDirection {
        OUTBOUND,
        INBOUND,
        INTERNAL
    }

    public enum TaxDocumentStatus {
        DRAFT,
        PENDING_VALIDATION,
        READY_TO_SEND,
        SENT,
        AUTHORIZED,
        REJECTED,
        CANCELLED
    }

    public enum WithholdingTaxType {
        INCOME,
        VAT
    }

    public enum WithholdingSupplierRegime {
        NATURAL_PERSON,
        COMPANY,
        RIMPE,
        FOREIGN
    }

    public enum WithholdingDocumentStatus {
        PROPOSED,
        CONFIRMED,
        ISSUED,
        AUTHORIZED,
        REJECTED,
        CANCELLED
    }

    public enum AtsPeriodStatus {
        DRAFT,
        GENERATED,
        SUBMITTED,
        CLOSED
    }
}
