package com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository;

import com.ucacue.bar.erp.onboarding.infrastructure.persistence.entity.OnboardingAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingAuditEventRepository extends JpaRepository<OnboardingAuditEventEntity, Long> {
}
