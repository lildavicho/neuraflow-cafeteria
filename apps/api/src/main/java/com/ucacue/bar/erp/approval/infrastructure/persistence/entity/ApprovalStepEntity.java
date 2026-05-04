package com.ucacue.bar.erp.approval.infrastructure.persistence.entity;

import com.ucacue.bar.entity.TenantScoped;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "erp_approval_steps",
        uniqueConstraints = @UniqueConstraint(name = "uq_approval_step_order", columnNames = {"definition_id", "step_order"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStepEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false)
    private ApprovalDefinitionEntity definition;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "approver_role", nullable = false, length = 32)
    private String approverRole;

    @Column(name = "required_count", nullable = false)
    private Integer requiredCount = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
