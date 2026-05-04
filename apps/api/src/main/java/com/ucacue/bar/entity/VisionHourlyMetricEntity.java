package com.ucacue.bar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vision_hourly_metrics", indexes = {
        @Index(name = "idx_vision_hourly_metrics_hour_start", columnList = "hour_start"),
        @Index(name = "idx_vision_hourly_metrics_camera_hour_start", columnList = "camera_id,hour_start")
})
@Getter
@Setter
@NoArgsConstructor
public class VisionHourlyMetricEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camera_id", nullable = false)
    private CameraEntity camera;

    @Column(name = "hour_start", nullable = false)
    private LocalDateTime hourStart;

    @Column(name = "unique_people", nullable = false)
    private Integer uniquePeople;

    @Column(name = "event_count", nullable = false)
    private Integer eventCount;

    @Column(name = "last_event_at")
    private LocalDateTime lastEventAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
