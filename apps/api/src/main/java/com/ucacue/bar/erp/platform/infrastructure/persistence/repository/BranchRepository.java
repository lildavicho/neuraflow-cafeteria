package com.ucacue.bar.erp.platform.infrastructure.persistence.repository;

import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.BranchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, Long> {

    Page<BranchEntity> findByTenantIdAndActiveTrue(Long tenantId, Pageable pageable);

    Page<BranchEntity> findByTenantId(Long tenantId, Pageable pageable);

    List<BranchEntity> findAllByTenantIdAndActiveTrue(Long tenantId);

    Optional<BranchEntity> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT b FROM BranchEntity b WHERE b.tenantId = :tenantId AND LOWER(b.code) = LOWER(:code)")
    Optional<BranchEntity> findByTenantIdAndCodeIgnoreCase(@Param("tenantId") Long tenantId,
                                                            @Param("code") String code);

    Optional<BranchEntity> findByTenantIdAndIsMainTrue(Long tenantId);

    boolean existsByTenantIdAndIsMainTrue(Long tenantId);

    @Query("SELECT COUNT(b) FROM BranchEntity b WHERE b.tenantId = :tenantId AND LOWER(b.code) = LOWER(:code) AND (:excludeId IS NULL OR b.id <> :excludeId)")
    long countByTenantIdAndCodeExcluding(@Param("tenantId") Long tenantId,
                                          @Param("code") String code,
                                          @Param("excludeId") Long excludeId);
}
