package com.ucacue.bar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_stock_movement_product", columnList = "product_id"),
    @Index(name = "idx_stock_movement_created", columnList = "created")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    public enum MovementType {
        IN, OUT, ADJUST
    }
}
