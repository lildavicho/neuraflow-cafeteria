package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.DocumentDirection;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.FiscalDomain;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "acc_document_types", indexes = {
        @Index(name = "idx_acc_document_types_tenant_code", columnList = "tenant_id,document_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class AccountingDocumentTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "document_code", nullable = false, length = 30)
    private String documentCode;

    @Column(name = "document_name", nullable = false, length = 120)
    private String documentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "fiscal_domain", nullable = false, length = 20)
    private FiscalDomain fiscalDomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_direction", nullable = false, length = 20)
    private DocumentDirection documentDirection;

    @Column(name = "requires_sequence", nullable = false)
    private Boolean requiresSequence = Boolean.TRUE;

    @Column(name = "requires_counterparty", nullable = false)
    private Boolean requiresCounterparty = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
