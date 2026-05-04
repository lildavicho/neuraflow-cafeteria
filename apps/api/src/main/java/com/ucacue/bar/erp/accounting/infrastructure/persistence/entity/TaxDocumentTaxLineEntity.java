package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

@Entity
@Table(name = "tax_document_tax_lines", indexes = {
        @Index(name = "idx_tax_document_tax_lines_document", columnList = "tax_document_id")
})
@Getter
@Setter
@NoArgsConstructor
public class TaxDocumentTaxLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_document_id", nullable = false)
    private TaxDocumentEntity taxDocument;

    @Column(name = "tax_rule_id")
    private Long taxRuleId;

    @Column(name = "taxable_base", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableBase = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal percentage = BigDecimal.ZERO;
}
