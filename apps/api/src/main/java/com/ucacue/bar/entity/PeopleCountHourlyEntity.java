package com.ucacue.bar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "people_counts_hourly", indexes = {
    @Index(name = "idx_people_counts_hourly_hour_start", columnList = "hour_start")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_people_counts_hourly_tenant_hour", columnNames = {"tenant_id", "hour_start"})
})
@Getter
@Setter
@NoArgsConstructor
public class PeopleCountHourlyEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "hour_start", nullable = false)
    private LocalDateTime hourStart;

    @Column(name = "avg_count", nullable = false, precision = 10, scale = 2)
    private BigDecimal averageCount;
}
