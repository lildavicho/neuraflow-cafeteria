package com.ucacue.bar.erp.party.infrastructure.persistence.entity;

import com.ucacue.bar.entity.TenantScoped;
import com.ucacue.bar.erp.party.domain.PartyTypes.RoleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "erp_party_roles", indexes = {
        @Index(name = "idx_erp_party_roles_tenant_role", columnList = "tenant_id, role_type"),
        @Index(name = "idx_erp_party_roles_party", columnList = "party_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PartyRoleEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private PartyEntity party;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 30)
    private RoleType roleType;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
