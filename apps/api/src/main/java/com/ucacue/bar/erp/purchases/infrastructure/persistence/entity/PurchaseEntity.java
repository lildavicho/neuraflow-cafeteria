package com.ucacue.bar.erp.purchases.infrastructure.persistence.entity;

import com.ucacue.bar.erp.purchases.domain.PurchaseTypes.PurchaseStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "erp_purchases", indexes = {
        @Index(name = "idx_erp_purchases_tenant_status", columnList = "tenant_id,status")
})
@Getter
@Setter
@NoArgsConstructor
public class PurchaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "internal_number", length = 40)
    private String internalNumber;

    @Column(name = "approval_instance_id")
    private Long approvalInstanceId;

    @Column(name = "supplier_name", nullable = false, length = 150)
    private String supplierName;

    @Column(name = "supplier_identification", length = 30)
    private String supplierIdentification;

    @Column(name = "supplier_email", length = 120)
    private String supplierEmail;

    @Column(name = "external_document_number", length = 60)
    private String externalDocumentNumber;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseStatus status = PurchaseStatus.DRAFT;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "accounting_entry_id")
    private Long accountingEntryId;

    @Column(name = "payment_accounting_entry_id")
    private Long paymentAccountingEntryId;

    @Column(name = "payable_id")
    private Long payableId;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItemEntity> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addItem(PurchaseItemEntity item) {
        items.add(item);
        item.setPurchase(this);
    }
}
