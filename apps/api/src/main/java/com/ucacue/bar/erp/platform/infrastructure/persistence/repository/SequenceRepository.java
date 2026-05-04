package com.ucacue.bar.erp.platform.infrastructure.persistence.repository;

import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.SequenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SequenceRepository extends JpaRepository<SequenceEntity, Long> {

    List<SequenceEntity> findByTenantIdAndActiveTrueOrderByCodeAsc(Long tenantId);

    List<SequenceEntity> findByTenantIdOrderByCodeAsc(Long tenantId);

    Optional<SequenceEntity> findByIdAndTenantId(Long id, Long tenantId);

    Optional<SequenceEntity> findByTenantIdAndBranchIdAndCode(Long tenantId, Long branchId, String code);

    /** Global sequence (no branch) */
    @Query("SELECT s FROM SequenceEntity s WHERE s.tenantId = :tenantId AND s.branch IS NULL AND s.code = :code AND s.active = true")
    Optional<SequenceEntity> findGlobalByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    /** For atomic next-number generation with pessimistic lock */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SequenceEntity s WHERE s.id = :id AND s.tenantId = :tenantId")
    Optional<SequenceEntity> findWithLock(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /** Find best match: branch-specific first, then global fallback */
    @Query("""
        SELECT s FROM SequenceEntity s
        WHERE s.tenantId = :tenantId
          AND s.code = :code
          AND s.active = true
          AND (s.branch.id = :branchId OR s.branch IS NULL)
        ORDER BY s.branch.id DESC NULLS LAST
    """)
    List<SequenceEntity> findByCodeWithBranchFallback(
            @Param("tenantId") Long tenantId,
            @Param("code") String code,
            @Param("branchId") Long branchId);
}
