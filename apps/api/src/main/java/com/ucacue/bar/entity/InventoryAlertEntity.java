package com.ucacue.bar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_alerts", indexes = {
    @Index(name = "idx_inventory_alert_product", columnList = "product_id"),
    @Index(name = "idx_inventory_alert_ack", columnList = "acknowledged")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAlertEntity implements TenantScoped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false, length = 30)
    private String alertType;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String severity = "WARNING";

    @Column(nullable = false)
    @Builder.Default
    private Boolean acknowledged = false;

    @Column
    private Long acknowledgedBy;

    @Column
    private LocalDateTime acknowledgedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
