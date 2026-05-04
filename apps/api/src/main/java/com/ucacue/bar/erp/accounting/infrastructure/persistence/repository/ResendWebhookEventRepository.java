package com.ucacue.bar.erp.accounting.infrastructure.persistence.repository;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.ResendWebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResendWebhookEventRepository extends JpaRepository<ResendWebhookEventEntity, Long> {
}
