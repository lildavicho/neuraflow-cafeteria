package com.ucacue.bar.erp.commercial.infrastructure.persistence.entity;

import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "erp_tenant_feature_flags", indexes = {
        @Index(name = "idx_erp_tenant_feature_flags_tenant", columnList = "tenant_id"),
        @Index(name = "idx_erp_tenant_feature_flags_module", columnList = "module_code")
})
@Getter
@Setter
@NoArgsConstructor
public class TenantFeatureFlagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", nullable = false, length = 40)
    private ModuleCode moduleCode;

    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "reason_text", length = 255)
    private String reasonText;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
