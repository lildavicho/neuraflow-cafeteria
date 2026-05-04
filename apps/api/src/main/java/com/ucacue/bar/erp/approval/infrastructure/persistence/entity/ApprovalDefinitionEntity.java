package com.ucacue.bar.erp.approval.infrastructure.persistence.entity;

import com.ucacue.bar.entity.TenantScoped;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "erp_approval_definitions",
        uniqueConstraints = @UniqueConstraint(name = "uq_approval_def_tenant_code", columnNames = {"tenant_id", "code"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDefinitionEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "condition_expr", columnDefinition = "TEXT")
    private String conditionExpr;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<ApprovalStepEntity> steps = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
