package com.ucacue.bar.erp.commercial.infrastructure.persistence.entity;

import com.ucacue.bar.erp.shared.domain.CommercialTypes.CommercialPlanCode;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "erp_tenant_plan_subscriptions", indexes = {
        @Index(name = "idx_erp_tenant_plan_subscriptions_tenant", columnList = "tenant_id"),
        @Index(name = "idx_erp_tenant_plan_subscriptions_status", columnList = "subscription_status")
})
@Getter
@Setter
@NoArgsConstructor
public class TenantPlanSubscriptionEntity {

    public enum SubscriptionStatus {
        ACTIVE,
        SUSPENDED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "commercial_plan", nullable = false, length = 30)
    private CommercialPlanCode commercialPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 30)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
