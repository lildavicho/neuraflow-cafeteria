package com.ucacue.bar.erp.vision.infrastructure.persistence.repository;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionProcessingLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisionProcessingLogRepository extends JpaRepository<VisionProcessingLogEntity, Long> {
}
