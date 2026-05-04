package com.ucacue.bar.repository;

import com.ucacue.bar.entity.InventoryAlertEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAlertRepository extends JpaRepository<InventoryAlertEntity, Long> {
    Page<InventoryAlertEntity> findByAcknowledgedFalseOrderByCreatedAtDesc(Pageable pageable);

    List<InventoryAlertEntity> findByProductIdAndAcknowledgedFalse(Long productId);

    long countByAcknowledgedFalse();

    // ── Tenant-filtered queries ──

    @EntityGraph(attributePaths = "product")
    Page<InventoryAlertEntity> findByTenantIdAndAcknowledgedFalseOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    List<InventoryAlertEntity> findByTenantIdAndProductIdAndAcknowledgedFalse(Long tenantId, Long productId);

    java.util.Optional<InventoryAlertEntity> findByTenantIdAndId(Long tenantId, Long id);

    long countByTenantIdAndAcknowledgedFalse(Long tenantId);
}
