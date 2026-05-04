package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxComputationType;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxScope;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "acc_tax_rules", indexes = {
        @Index(name = "idx_acc_tax_rules_tenant_code", columnList = "tenant_id,tax_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class TaxRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tax_code", nullable = false, length = 30)
    private String taxCode;

    @Column(name = "tax_name", nullable = false, length = 120)
    private String taxName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_scope", nullable = false, length = 20)
    private TaxScope taxScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "computation_type", nullable = false, length = 20)
    private TaxComputationType computationType;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "tax_authority_code", length = 30)
    private String taxAuthorityCode;

    @Column(name = "taxable_object_code", length = 30)
    private String taxableObjectCode;

    @Column(name = "included_in_price", nullable = false)
    private Boolean includedInPrice = Boolean.FALSE;

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
