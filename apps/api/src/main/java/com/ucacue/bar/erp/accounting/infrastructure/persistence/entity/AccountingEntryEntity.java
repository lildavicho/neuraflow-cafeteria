package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountingEntryStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountingSourceModule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "acc_journal_entries", indexes = {
        @Index(name = "idx_acc_journal_entries_tenant_entry", columnList = "tenant_id,entry_number", unique = true),
        @Index(name = "idx_acc_journal_entries_source", columnList = "source_type,source_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AccountingEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "entry_number", nullable = false, length = 40)
    private String entryNumber;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountingEntryStatus status = AccountingEntryStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_module", nullable = false, length = 20)
    private AccountingSourceModule sourceModule;

    @Column(name = "source_type", nullable = false, length = 40)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "external_reference", length = 80)
    private String externalReference;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
