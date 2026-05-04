package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingDocumentStatus;
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
@Table(name = "acc_withholding_documents", indexes = {
        @Index(name = "idx_acc_withholding_docs_tenant_purchase", columnList = "tenant_id,purchase_id"),
        @Index(name = "idx_acc_withholding_docs_tenant_status", columnList = "tenant_id,status"),
        @Index(name = "uk_acc_withholding_docs_access_key", columnList = "access_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class WithholdingDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "purchase_id", nullable = false)
    private Long purchaseId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "supplier_identification", length = 30)
    private String supplierIdentification;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "establishment_code", length = 10)
    private String establishmentCode;

    @Column(name = "emission_point_code", length = 10)
    private String emissionPointCode;

    @Column(name = "sequential_number", length = 20)
    private String sequentialNumber;

    @Column(name = "access_key", length = 80, unique = true)
    private String accessKey;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WithholdingDocumentStatus status = WithholdingDocumentStatus.PROPOSED;

    @Column(name = "iva_base", nullable = false, precision = 14, scale = 2)
    private BigDecimal ivaBase = BigDecimal.ZERO;

    @Column(name = "iva_withheld", nullable = false, precision = 14, scale = 2)
    private BigDecimal ivaWithheld = BigDecimal.ZERO;

    @Column(name = "income_base", nullable = false, precision = 14, scale = 2)
    private BigDecimal incomeBase = BigDecimal.ZERO;

    @Column(name = "income_withheld", nullable = false, precision = 14, scale = 2)
    private BigDecimal incomeWithheld = BigDecimal.ZERO;

    @Column(name = "total_withheld", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalWithheld = BigDecimal.ZERO;

    @Column(name = "xml_payload", columnDefinition = "LONGTEXT")
    private String xmlPayload;

    @Column(name = "signed_xml_payload", columnDefinition = "LONGTEXT")
    private String signedXmlPayload;

    @Column(name = "authorization_code", length = 80)
    private String authorizationCode;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WithholdingLineEntity> lines = new ArrayList<>();
}
