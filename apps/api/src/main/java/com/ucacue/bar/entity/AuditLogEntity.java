package com.ucacue.bar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_log_user", columnList = "user_id"),
    @Index(name = "idx_audit_log_action", columnList = "action"),
    @Index(name = "idx_audit_log_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_log_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity implements TenantScoped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 120)
    private String userEmail;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(nullable = false, length = 60)
    private String entityType;

    @Column
    private Long entityId;

    @Column(columnDefinition = "text")
    private String details;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
