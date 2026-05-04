package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingTaxType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "acc_withholding_lines", indexes = {
        @Index(name = "idx_acc_withholding_lines_document", columnList = "document_id")
})
@Getter
@Setter
@NoArgsConstructor
public class WithholdingLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private WithholdingDocumentEntity document;

    @Column(name = "matrix_id")
    private Long matrixId;

    @Column(name = "concept_code", nullable = false, length = 10)
    private String conceptCode;

    @Column(name = "concept_name", nullable = false, length = 200)
    private String conceptName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 20)
    private WithholdingTaxType taxType;

    @Column(name = "base_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @Column(name = "rate_percentage", nullable = false, precision = 7, scale = 4)
    private BigDecimal ratePercentage = BigDecimal.ZERO;

    @Column(name = "withheld_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal withheldAmount = BigDecimal.ZERO;

    @Column(name = "related_document_code", length = 20)
    private String relatedDocumentCode;

    @Column(name = "related_document_serial", length = 40)
    private String relatedDocumentSerial;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
