package com.ucacue.bar.erp.platform.infrastructure.persistence.entity;

import com.ucacue.bar.entity.TenantScoped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "erp_warehouses", indexes = {
        @Index(name = "idx_erp_warehouses_tenant", columnList = "tenant_id"),
        @Index(name = "idx_erp_warehouses_branch", columnList = "branch_id"),
        @Index(name = "idx_erp_warehouses_active", columnList = "tenant_id, active")
})
@Getter
@Setter
@NoArgsConstructor
public class WarehouseEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WarehouseType type = WarehouseType.STORE;

    @Column(length = 300)
    private String address;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum WarehouseType {
        STORE,
        WAREHOUSE,
        TRANSIT
    }
}
