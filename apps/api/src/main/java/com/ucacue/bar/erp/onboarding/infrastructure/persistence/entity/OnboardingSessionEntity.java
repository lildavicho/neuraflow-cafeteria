package com.ucacue.bar.erp.onboarding.infrastructure.persistence.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "tenant_onboarding_sessions", indexes = {
        @Index(name = "idx_tenant_onboarding_sessions_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class OnboardingSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "current_step", nullable = false, length = 80)
    private String currentStep = "business-discovery";

    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent = 0;

    @Column(name = "vertical_template", length = 80)
    private String verticalTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_profile", nullable = false)
    private Map<String, Object> businessProfile = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "legal_entity", nullable = false)
    private Map<String, Object> legalEntity = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_profile", nullable = false)
    private Map<String, Object> taxProfile = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "certificate_status", nullable = false)
    private Map<String, Object> certificateStatus = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "emission_setup", nullable = false)
    private Map<String, Object> emissionSetup = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "accounting_setup", nullable = false)
    private Map<String, Object> accountingSetup = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sales_setup", nullable = false)
    private Map<String, Object> salesSetup = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "purchases_setup", nullable = false)
    private Map<String, Object> purchasesSetup = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inventory_setup", nullable = false)
    private Map<String, Object> inventorySetup = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "growth_setup", nullable = false)
    private Map<String, Object> growthSetup = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "brand_setup", nullable = false)
    private Map<String, Object> brandSetup = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions_setup", nullable = false)
    private Map<String, Object> permissionsSetup = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> checklist = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Map<String, Object>> warnings = new ArrayList<>();

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
