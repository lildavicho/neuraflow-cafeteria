package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tax_ride_templates", indexes = {
        @Index(name = "idx_tax_ride_templates_tenant_active", columnList = "tenant_id,active"),
        @Index(name = "uk_tax_ride_templates_tenant_code", columnList = "tenant_id,template_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class RideTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "template_code", nullable = false, length = 60)
    private String templateCode = "DEFAULT";

    @Column(name = "template_name", nullable = false, length = 120)
    private String templateName = "InsightVision RIDE";

    @Column(name = "layout_json", columnDefinition = "TEXT")
    private String layoutJson;

    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;

    @Column(name = "custom", nullable = false)
    private Boolean custom = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
