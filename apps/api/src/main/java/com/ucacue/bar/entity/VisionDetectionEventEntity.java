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
@Table(name = "vision_detection_events", indexes = {
        @Index(name = "idx_vision_detection_events_ts", columnList = "ts"),
        @Index(name = "idx_vision_detection_events_camera_ts", columnList = "camera_id,ts")
})
@Getter
@Setter
@NoArgsConstructor
public class VisionDetectionEventEntity implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camera_id", nullable = false)
    private CameraEntity camera;

    @Column(name = "event_key", nullable = false, length = 120)
    private String eventKey;

    @Column(name = "ts", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "people_count", nullable = false)
    private Integer peopleCount;

    @Column(name = "unique_people", nullable = false)
    private Integer uniquePeople;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "track_ids", columnDefinition = "TEXT")
    private String trackIds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
