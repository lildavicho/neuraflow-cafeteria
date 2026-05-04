package com.ucacue.bar.erp.vision.infrastructure.persistence.repository;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface VisionEventRepository extends JpaRepository<VisionEventEntity, Long> {

    Optional<VisionEventEntity> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);

    Optional<VisionEventEntity> findByTenantIdAndCameraIdAndFrameId(Long tenantId, Long cameraId, String frameId);

    @Query("select e from VisionEventEntity e " +
            "where e.tenantId = :tenantId " +
            "order by e.eventTimestamp desc, e.id desc")
    List<VisionEventEntity> findRecentByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    long countByTenantIdAndEventTimestampGreaterThanEqualAndEventTimestampLessThan(Long tenantId,
                                                                                  OffsetDateTime from,
                                                                                  OffsetDateTime to);
}
