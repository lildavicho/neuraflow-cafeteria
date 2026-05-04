package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AtsPeriodStatus;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "acc_ats_periods",
        uniqueConstraints = @UniqueConstraint(name = "uk_acc_ats_periods_tenant_year_month",
                columnNames = {"tenant_id", "fiscal_year", "fiscal_month"}),
        indexes = @Index(name = "idx_acc_ats_periods_tenant_status", columnList = "tenant_id,status"))
@Getter
@Setter
@NoArgsConstructor
public class AtsPeriodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "fiscal_month", nullable = false)
    private Integer fiscalMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AtsPeriodStatus status = AtsPeriodStatus.DRAFT;

    @Column(name = "sales_count", nullable = false)
    private Integer salesCount = 0;

    @Column(name = "purchases_count", nullable = false)
    private Integer purchasesCount = 0;

    @Column(name = "withholdings_count", nullable = false)
    private Integer withholdingsCount = 0;

    @Column(name = "total_sales", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "total_purchases", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPurchases = BigDecimal.ZERO;

    @Column(name = "total_withheld", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalWithheld = BigDecimal.ZERO;

    @Column(name = "xml_payload", columnDefinition = "LONGTEXT")
    private String xmlPayload;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
