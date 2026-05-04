package com.ucacue.bar.erp.vision.infrastructure.persistence.repository;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisionAuditLogRepository extends JpaRepository<VisionAuditLogEntity, Long> {
}
