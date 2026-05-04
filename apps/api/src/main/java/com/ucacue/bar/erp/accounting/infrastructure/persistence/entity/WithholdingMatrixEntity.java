package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingSupplierRegime;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingTaxType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "acc_withholding_matrix",
        indexes = @Index(name = "idx_acc_withholding_matrix_tenant_active", columnList = "tenant_id,active"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_acc_withholding_matrix_tenant_code_regime",
                columnNames = {"tenant_id", "concept_code", "supplier_regime", "tax_type"}))
@Getter
@Setter
@NoArgsConstructor
public class WithholdingMatrixEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "concept_code", nullable = false, length = 10)
    private String conceptCode;

    @Column(name = "concept_name", nullable = false, length = 200)
    private String conceptName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 20)
    private WithholdingTaxType taxType;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_regime", nullable = false, length = 40)
    private WithholdingSupplierRegime supplierRegime;

    @Column(name = "rate_percentage", nullable = false, precision = 7, scale = 4)
    private BigDecimal ratePercentage = BigDecimal.ZERO;

    @Column(name = "min_base_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal minBaseAmount = BigDecimal.ZERO;

    @Column(name = "sri_form_code", length = 10)
    private String sriFormCode;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
