package com.ucacue.bar.erp.platform.infrastructure.persistence.entity;

import com.ucacue.bar.entity.TenantScoped;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "erp_sequences",
        uniqueConstraints = @UniqueConstraint(name = "uq_erp_sequence_tenant_branch_code",
                columnNames = {"tenant_id", "branch_id", "code"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SequenceEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "prefix", length = 20)
    private String prefix;

    @Column(name = "suffix", length = 20)
    private String suffix;

    @Column(name = "pad_length", nullable = false)
    private Integer padLength = 7;

    @Column(name = "current_number", nullable = false)
    private Long currentNumber = 0L;

    @Column(name = "increment_step", nullable = false)
    private Integer incrementStep = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "reset_policy", nullable = false, length = 20)
    private ResetPolicy resetPolicy = ResetPolicy.NEVER;

    @Column(name = "last_reset_at")
    private LocalDateTime lastResetAt;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum ResetPolicy {
        NEVER, YEARLY, MONTHLY
    }
}
