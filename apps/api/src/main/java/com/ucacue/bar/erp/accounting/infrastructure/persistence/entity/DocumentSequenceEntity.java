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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "acc_document_sequences", indexes = {
        @Index(name = "idx_acc_document_sequences_type", columnList = "document_type_id"),
        @Index(name = "idx_acc_document_sequences_tenant_codes", columnList = "tenant_id,document_type_id,establishment_code,emission_point_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class DocumentSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_type_id", nullable = false)
    private AccountingDocumentTypeEntity documentType;

    @Column(name = "establishment_code", nullable = false, length = 10)
    private String establishmentCode;

    @Column(name = "emission_point_code", nullable = false, length = 10)
    private String emissionPointCode;

    @Column(name = "current_number", nullable = false)
    private Long currentNumber = 0L;

    @Column(name = "increment_step", nullable = false)
    private Integer incrementStep = 1;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
