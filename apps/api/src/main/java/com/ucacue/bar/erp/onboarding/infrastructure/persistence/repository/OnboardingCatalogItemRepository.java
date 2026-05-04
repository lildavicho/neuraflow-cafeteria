package com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository;

import com.ucacue.bar.erp.onboarding.infrastructure.persistence.entity.OnboardingCatalogItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingCatalogItemRepository extends JpaRepository<OnboardingCatalogItemEntity, Long> {
    List<OnboardingCatalogItemEntity> findByCatalogTypeAndActiveTrueOrderByItemCodeAsc(String catalogType);
}
