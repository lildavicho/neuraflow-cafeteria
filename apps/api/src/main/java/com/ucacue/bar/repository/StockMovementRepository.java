package com.ucacue.bar.repository;

import com.ucacue.bar.entity.StockMovementEntity;
import com.ucacue.bar.entity.StockMovementEntity.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovementEntity, Long> {
    Page<StockMovementEntity> findByProductId(Long productId, Pageable pageable);

    Page<StockMovementEntity> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<StockMovementEntity> findByCreatedAtBetween(LocalDateTime startDate,
                                                    LocalDateTime endDate,
                                                    Pageable pageable);

    Page<StockMovementEntity> findByTypeOrderByCreatedAtDesc(MovementType type, Pageable pageable);

    Page<StockMovementEntity> findByWarehouseOrderByCreatedAtDesc(String warehouse, Pageable pageable);

    Page<StockMovementEntity> findByProductIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long productId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT sm FROM StockMovementEntity sm WHERE " +
           "(:productId IS NULL OR sm.product.id = :productId) AND " +
           "(:type IS NULL OR sm.type = :type) AND " +
           "(:warehouse IS NULL OR sm.warehouse = :warehouse) AND " +
           "(:from IS NULL OR sm.createdAt >= :from) AND " +
           "(:to IS NULL OR sm.createdAt <= :to) " +
           "ORDER BY sm.createdAt DESC")
    Page<StockMovementEntity> findFiltered(
            @Param("productId") Long productId,
            @Param("type") MovementType type,
            @Param("warehouse") String warehouse,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT DISTINCT sm.warehouse FROM StockMovementEntity sm WHERE sm.warehouse IS NOT NULL")
    List<String> findDistinctWarehouses();

    List<StockMovementEntity> findByBatchCode(String batchCode);

    // ── Tenant-filtered queries ──

    @EntityGraph(attributePaths = {"product", "user"})
    Page<StockMovementEntity> findByTenantId(Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "user"})
    Page<StockMovementEntity> findByTenantIdAndProductIdOrderByCreatedAtDesc(Long tenantId, Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "user"})
    Page<StockMovementEntity> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime startDate,
                                                                 LocalDateTime endDate, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "user"})
    Page<StockMovementEntity> findByTenantIdAndTypeOrderByCreatedAtDesc(Long tenantId, MovementType type, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "user"})
    Page<StockMovementEntity> findByTenantIdAndWarehouseOrderByCreatedAtDesc(Long tenantId, String warehouse, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "user"})
    @Query("SELECT sm FROM StockMovementEntity sm WHERE sm.tenantId = :tenantId AND " +
           "(:productId IS NULL OR sm.product.id = :productId) AND " +
           "(:type IS NULL OR sm.type = :type) AND " +
           "(:warehouse IS NULL OR sm.warehouse = :warehouse) AND " +
           "(:from IS NULL OR sm.createdAt >= :from) AND " +
           "(:to IS NULL OR sm.createdAt <= :to) " +
           "ORDER BY sm.createdAt DESC")
    Page<StockMovementEntity> findFilteredByTenantId(
            @Param("tenantId") Long tenantId,
            @Param("productId") Long productId,
            @Param("type") MovementType type,
            @Param("warehouse") String warehouse,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT DISTINCT sm.warehouse FROM StockMovementEntity sm WHERE sm.tenantId = :tenantId AND sm.warehouse IS NOT NULL")
    List<String> findDistinctWarehousesByTenantId(@Param("tenantId") Long tenantId);

    List<StockMovementEntity> findByTenantIdAndBatchCode(Long tenantId, String batchCode);
}
