package com.ucacue.bar.repository;

import com.ucacue.bar.entity.ModelMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelMetricRepository extends JpaRepository<ModelMetricEntity, Long> {

    // ── Tenant-filtered queries ──

    List<ModelMetricEntity> findByTenantId(Long tenantId);

    List<ModelMetricEntity> findByTenantIdAndModelName(Long tenantId, String modelName);
}
