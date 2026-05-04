package com.ucacue.bar.entity;

import com.ucacue.bar.entity.UserEntity.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Asocia un {@link PermissionEntity} a un {@link UserRole}.
 * {@code tenantId} NULL representa una asignación default global
 * (aplica a todos los tenants). Un registro con tenantId específico
 * sobrescribe el default para ese tenant.
 */
@Entity
@Table(name = "role_permissions",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_permissions", columnNames = {"role", "permission_id", "tenant_id"}),
    indexes = {
        @Index(name = "idx_role_permissions_role",   columnList = "role"),
        @Index(name = "idx_role_permissions_tenant", columnList = "tenant_id"),
        @Index(name = "idx_role_permissions_lookup", columnList = "role, tenant_id")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionEntity permission;

    /** NULL = default global (aplica a todos los tenants). */
    @Column(name = "tenant_id")
    private Long tenantId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
