package com.ucacue.bar.erp.vision.infrastructure.persistence.repository;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionCameraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VisionCameraRepository extends JpaRepository<VisionCameraEntity, Long> {

    @Query("select c from VisionCameraEntity c where c.tenantId = :tenantId and lower(c.code) = lower(:code)")
    Optional<VisionCameraEntity> findByTenantIdAndCodeIgnoreCase(@Param("tenantId") Long tenantId,
                                                                 @Param("code") String code);

    Optional<VisionCameraEntity> findByTenantIdAndCodeIgnoreCaseAndStatus(Long tenantId,
                                                                          String code,
                                                                          VisionCameraEntity.Status status);

    long countByTenantIdAndStatus(Long tenantId, VisionCameraEntity.Status status);
}
