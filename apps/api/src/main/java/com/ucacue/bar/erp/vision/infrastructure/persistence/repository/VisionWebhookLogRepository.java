package com.ucacue.bar.erp.vision.infrastructure.persistence.repository;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionWebhookLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisionWebhookLogRepository extends JpaRepository<VisionWebhookLogEntity, Long> {
}
