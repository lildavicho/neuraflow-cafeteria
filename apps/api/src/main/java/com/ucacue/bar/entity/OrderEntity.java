package com.ucacue.bar.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_created_at", columnList = "created_at"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false, length = 30)
    private TransactionStatus transactionStatus = TransactionStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 100)
    private String paymentReference;

    @Column(name = "payment_breakdown_json", length = 2000)
    private String paymentBreakdownJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 30)
    private PaymentStatus paymentStatus;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false, length = 30)
    private InventoryStatus inventoryStatus = InventoryStatus.RESERVED;

    @Column(length = 500)
    private String notes;

    @Column(name = "customer_email", length = 180)
    private String customerEmail;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_identification", length = 20)
    private String customerIdentification;

    @Column(name = "customer_identification_type", length = 10)
    private String customerIdentificationType;

    @Column(name = "customer_address", length = 300)
    private String customerAddress;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType = DocumentType.NOTA_VENTA;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private SaleEntity sale;

    @Column(name = "loyalty_points_awarded")
    private Integer loyaltyPointsAwarded;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "preparation_start_at")
    private LocalDateTime preparationStartAt;

    @Column(name = "estimated_ready_at")
    private LocalDateTime estimatedReadyAt;

    @Column(name = "actual_ready_at")
    private LocalDateTime actualReadyAt;

    public BigDecimal getEffectiveSubtotal() {
        return subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public BigDecimal getEffectiveTotal() {
        return total != null ? total : BigDecimal.ZERO;
    }

    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
    }

    public enum OrderStatus {
        PENDING, // Pendiente
        CONFIRMED, // Confirmada
        PREPARING, // En preparación
        READY, // Lista para entrega
        DELIVERED, // Entregada
        COMPLETED, // ✅ Agregar este valor - Completada/Pagada
        CANCELLED // Cancelada
    }

    public enum PaymentMethod {
        CASH,
        CARD,
        TRANSFER
    }

    public enum PaymentStatus {
        PENDING,
        PAID,
        FAILED,
        REFUNDED
    }

    public enum TransactionStatus {
        DRAFT,
        PENDING_PAYMENT,
        PAID,
        CANCELLED,
        REFUNDED
    }

    public enum InventoryStatus {
        RESERVED,
        COMMITTED,
        RELEASED
    }

    public enum DocumentType {
        NOTA_VENTA,
        FACTURA
    }
}
