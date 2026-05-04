package com.ucacue.bar.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_registers", indexes = {
        @Index(name = "idx_cash_reg_status", columnList = "status"),
        @Index(name = "idx_cash_reg_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class CashRegisterEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RegisterStatus status = RegisterStatus.OPEN;

    @Column(name = "opening_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingAmount = BigDecimal.ZERO;

    @Column(name = "closing_amount", precision = 12, scale = 2)
    private BigDecimal closingAmount;

    @Column(name = "cash_sales_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal cashSalesTotal = BigDecimal.ZERO;

    @Column(name = "card_sales_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal cardSalesTotal = BigDecimal.ZERO;

    @Column(name = "transfer_sales_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal transferSalesTotal = BigDecimal.ZERO;

    @Column(name = "total_sales_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSalesAmount = BigDecimal.ZERO;

    @Column(name = "total_orders", nullable = false)
    private Integer totalOrders = 0;

    @Column(name = "expected_cash", precision = 12, scale = 2)
    private BigDecimal expectedCash;

    @Column(name = "actual_cash", precision = 12, scale = 2)
    private BigDecimal actualCash;

    @Column(name = "difference", precision = 12, scale = 2)
    private BigDecimal difference;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public enum RegisterStatus {
        OPEN,
        CLOSED
    }
}
