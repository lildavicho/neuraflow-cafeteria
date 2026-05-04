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
@Table(name = "acc_journal_entry_lines", indexes = {
        @Index(name = "idx_acc_journal_entry_lines_entry", columnList = "entry_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AccountingEntryLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private AccountingEntryEntity entry;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "tax_rule_id")
    private Long taxRuleId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(length = 255)
    private String description;

    @Column(name = "reference_code", length = 80)
    private String referenceCode;
}
