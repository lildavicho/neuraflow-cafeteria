package com.ucacue.bar.erp.vision.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "vision_events", indexes = {
        @Index(name = "idx_vision_events_tenant_camera_time", columnList = "tenant_id,camera_id,event_timestamp"),
        @Index(name = "idx_vision_events_tenant_time", columnList = "tenant_id,event_timestamp"),
        @Index(name = "idx_vision_events_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class VisionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "event_timestamp", nullable = false)
    private OffsetDateTime eventTimestamp;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "frame_id", length = 180)
    private String frameId;

    @Column(name = "idempotency_key", length = 250)
    private String idempotencyKey;

    @Column(nullable = false, length = 120)
    private String source = "yolo-service";

    @Column(name = "model_name", length = 120)
    private String modelName;

    @Column(name = "model_version", length = 120)
    private String modelVersion;

    @Column(name = "confidence_threshold", precision = 5, scale = 4)
    private BigDecimal confidenceThreshold;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "image_uri", columnDefinition = "TEXT")
    private String imageUri;

    @Column(name = "total_detections", nullable = false)
    private Integer totalDetections = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.RECEIVED;

    @Column(name = "processing_ms")
    private Integer processingMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private Map<String, Object> metadata = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Status {
        RECEIVED,
        PROCESSED,
        DUPLICATED,
        FAILED
    }
}
