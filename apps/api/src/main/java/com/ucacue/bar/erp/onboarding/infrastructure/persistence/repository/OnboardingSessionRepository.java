package com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository;

import com.ucacue.bar.erp.onboarding.infrastructure.persistence.entity.OnboardingSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingSessionRepository extends JpaRepository<OnboardingSessionEntity, Long> {
    Optional<OnboardingSessionEntity> findByTenantId(Long tenantId);
}
