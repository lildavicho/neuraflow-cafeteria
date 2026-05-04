package com.ucacue.bar.erp.vision.infrastructure.persistence.entity;

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
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "vision_detections", indexes = {
        @Index(name = "idx_vision_detections_tenant_event", columnList = "tenant_id,event_id"),
        @Index(name = "idx_vision_detections_tenant_camera_label", columnList = "tenant_id,camera_id,label"),
        @Index(name = "idx_vision_detections_tenant_label_created", columnList = "tenant_id,label,created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class VisionDetectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "camera_id", nullable = false)
    private Long cameraId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false, precision = 6, scale = 5)
    private BigDecimal confidence;

    @Column(name = "bbox_x", nullable = false, precision = 12, scale = 4)
    private BigDecimal bboxX;

    @Column(name = "bbox_y", nullable = false, precision = 12, scale = 4)
    private BigDecimal bboxY;

    @Column(name = "bbox_width", nullable = false, precision = 12, scale = 4)
    private BigDecimal bboxWidth;

    @Column(name = "bbox_height", nullable = false, precision = 12, scale = 4)
    private BigDecimal bboxHeight;

    @Column(name = "tracking_id", length = 180)
    private String trackingId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private Map<String, Object> metadata = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
