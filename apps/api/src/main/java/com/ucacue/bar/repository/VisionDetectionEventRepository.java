package com.ucacue.bar.repository;

import com.ucacue.bar.entity.VisionDetectionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VisionDetectionEventRepository extends JpaRepository<VisionDetectionEventEntity, Long> {

    Optional<VisionDetectionEventEntity> findByCameraIdAndEventKey(Long cameraId, String eventKey);

    @Query("select max(v.timestamp) from VisionDetectionEventEntity v where v.camera.id = :cameraId")
    LocalDateTime findLastTimestampByCameraId(@Param("cameraId") Long cameraId);

    // ── Tenant-filtered queries ──

    Optional<VisionDetectionEventEntity> findByTenantIdAndCameraIdAndEventKey(Long tenantId, Long cameraId, String eventKey);

    @Query("select max(v.timestamp) from VisionDetectionEventEntity v where v.tenantId = :tenantId and v.camera.id = :cameraId")
    LocalDateTime findLastTimestampByTenantIdAndCameraId(@Param("tenantId") Long tenantId, @Param("cameraId") Long cameraId);
}
