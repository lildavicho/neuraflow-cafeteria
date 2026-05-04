package com.ucacue.bar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_stock_movement_product", columnList = "product_id"),
    @Index(name = "idx_stock_movement_created_at", columnList = "created_at"),
    @Index(name = "idx_stock_movement_batch", columnList = "batch_code"),
    @Index(name = "idx_stock_movement_warehouse", columnList = "warehouse"),
    @Index(name = "idx_stock_movement_type", columnList = "type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementEntity implements TenantScoped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer stockBefore;

    @Column(nullable = false)
    private Integer stockAfter;

    @Column(length = 200)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(length = 50)
    private String referenceType;

    @Column
    private Long referenceId;

    @Column(length = 60)
    private String batchCode;

    @Column(length = 60)
    private String serialNumber;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalCost;

    @Column(length = 60)
    private String warehouse = "PRINCIPAL";

    @Column
    private LocalDate expiryDate;

    @Column(length = 60)
    private String documentNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MovementType {
        IN, OUT, ADJUST, RESERVED, COMMITTED, RELEASED, REFUND
    }
}
