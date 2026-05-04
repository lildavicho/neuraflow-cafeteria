package com.ucacue.bar.repository;

import com.ucacue.bar.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    Optional<CategoryEntity> findByName(String name);
    boolean existsByName(String name);
    List<CategoryEntity> findByActive(Boolean active);

    // ── Tenant-filtered queries ──

    Optional<CategoryEntity> findByTenantIdAndName(Long tenantId, String name);
    boolean existsByTenantIdAndName(Long tenantId, String name);
    List<CategoryEntity> findByTenantIdAndActive(Long tenantId, Boolean active);
    List<CategoryEntity> findByTenantId(Long tenantId);
    Optional<CategoryEntity> findByTenantIdAndId(Long tenantId, Long id);
}
