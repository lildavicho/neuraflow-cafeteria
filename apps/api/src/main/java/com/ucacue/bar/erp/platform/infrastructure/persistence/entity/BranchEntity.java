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
@Table(name = "erp_branches", indexes = {
        @Index(name = "idx_erp_branches_tenant", columnList = "tenant_id"),
        @Index(name = "idx_erp_branches_active", columnList = "tenant_id, active")
})
@Getter
@Setter
@NoArgsConstructor
public class BranchEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 300)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String province;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(name = "sri_establishment_code", length = 10)
    private String sriEstablishmentCode;

    @Column(name = "is_main", nullable = false)
    private Boolean isMain = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
