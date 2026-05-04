package com.ucacue.bar.erp.commercial.infrastructure.persistence.entity;

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
@Table(name = "erp_tenants", indexes = {
        @Index(name = "idx_erp_tenants_code", columnList = "tenant_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", nullable = false, length = 50, unique = true)
    private String tenantCode;

    @Column(name = "legal_name", nullable = false, length = 150)
    private String legalName;

    @Column(name = "trade_name", length = 150)
    private String tradeName;

    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode = "EC";

    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone = "America/Guayaquil";

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode = "USD";

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
