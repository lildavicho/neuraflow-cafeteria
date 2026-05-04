package com.ucacue.bar.erp.platform.infrastructure.persistence.repository;

import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.WarehouseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<WarehouseEntity, Long> {

    Page<WarehouseEntity> findByTenantIdAndActiveTrue(Long tenantId, Pageable pageable);

    Page<WarehouseEntity> findByTenantId(Long tenantId, Pageable pageable);

    Page<WarehouseEntity> findByTenantIdAndBranchId(Long tenantId, Long branchId, Pageable pageable);

    List<WarehouseEntity> findAllByTenantIdAndActiveTrue(Long tenantId);

    List<WarehouseEntity> findAllByTenantIdAndBranchIdAndActiveTrue(Long tenantId, Long branchId);

    Optional<WarehouseEntity> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT w FROM WarehouseEntity w WHERE w.tenantId = :tenantId AND LOWER(w.code) = LOWER(:code)")
    Optional<WarehouseEntity> findByTenantIdAndCodeIgnoreCase(@Param("tenantId") Long tenantId,
                                                               @Param("code") String code);

    @Query("SELECT COUNT(w) FROM WarehouseEntity w WHERE w.tenantId = :tenantId AND LOWER(w.code) = LOWER(:code) AND (:excludeId IS NULL OR w.id <> :excludeId)")
    long countByTenantIdAndCodeExcluding(@Param("tenantId") Long tenantId,
                                          @Param("code") String code,
                                          @Param("excludeId") Long excludeId);
}
