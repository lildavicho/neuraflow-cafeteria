package com.ucacue.bar.repository;

import com.ucacue.bar.entity.VisionHourlyMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisionHourlyMetricRepository extends JpaRepository<VisionHourlyMetricEntity, Long> {

    Optional<VisionHourlyMetricEntity> findByCameraIdAndHourStart(Long cameraId, LocalDateTime hourStart);

    List<VisionHourlyMetricEntity> findByHourStartBetweenOrderByHourStartAsc(LocalDateTime from, LocalDateTime to);

    List<VisionHourlyMetricEntity> findByCameraIdAndHourStartBetweenOrderByHourStartAsc(Long cameraId,
                                                                                         LocalDateTime from,
                                                                                         LocalDateTime to);

    // ── Tenant-filtered queries ──

    Optional<VisionHourlyMetricEntity> findByTenantIdAndCameraIdAndHourStart(Long tenantId, Long cameraId, LocalDateTime hourStart);

    List<VisionHourlyMetricEntity> findByTenantIdAndHourStartBetweenOrderByHourStartAsc(Long tenantId, LocalDateTime from, LocalDateTime to);

    List<VisionHourlyMetricEntity> findByTenantIdAndCameraIdAndHourStartBetweenOrderByHourStartAsc(Long tenantId, Long cameraId,
                                                                                                     LocalDateTime from,
                                                                                                     LocalDateTime to);
}
