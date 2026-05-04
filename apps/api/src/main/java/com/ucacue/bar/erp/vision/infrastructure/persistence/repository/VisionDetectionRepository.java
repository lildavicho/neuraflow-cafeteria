package com.ucacue.bar.erp.vision.infrastructure.persistence.repository;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionDetectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface VisionDetectionRepository extends JpaRepository<VisionDetectionEntity, Long> {

    long countByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long tenantId,
                                                                         OffsetDateTime from,
                                                                         OffsetDateTime to);
}
