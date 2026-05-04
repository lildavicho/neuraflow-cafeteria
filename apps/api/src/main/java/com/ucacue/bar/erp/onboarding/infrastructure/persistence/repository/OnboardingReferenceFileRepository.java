package com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository;

import com.ucacue.bar.erp.onboarding.infrastructure.persistence.entity.OnboardingReferenceFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingReferenceFileRepository extends JpaRepository<OnboardingReferenceFileEntity, Long> {
}
