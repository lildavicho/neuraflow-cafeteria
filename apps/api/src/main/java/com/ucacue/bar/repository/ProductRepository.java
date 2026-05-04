package com.ucacue.bar.repository;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.entity.ProductEntity.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
        // ── Tenant-filtered queries ──

        Optional<ProductEntity> findByTenantIdAndCode(Long tenantId, String code);

       boolean existsByTenantIdAndCode(Long tenantId, String code);

       Page<ProductEntity> findByTenantId(Long tenantId, Pageable pageable);

       Page<ProductEntity> findByTenantIdAndStatus(Long tenantId, ProductStatus status, Pageable pageable);

       Page<ProductEntity> findByTenantIdAndCategoryId(Long tenantId, Long categoryId, Pageable pageable);

       Page<ProductEntity> findByTenantIdAndPrepared(Long tenantId, Boolean prepared, Pageable pageable);

        Page<ProductEntity> findByTenantIdAndCategoryIdAndPrepared(Long tenantId, Long categoryId, Boolean prepared, Pageable pageable);

        Optional<ProductEntity> findByTenantIdAndId(Long tenantId, Long id);

        @Query("SELECT p FROM ProductEntity p WHERE p.tenantId = :tenantId AND p.stock <= p.minStock AND p.status IN ('AVAILABLE', 'ACTIVE') ORDER BY (p.stock - p.minStock) ASC, p.name ASC")
        List<ProductEntity> findLowStockProductsByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

        @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.tenantId = :tenantId AND p.stock <= p.minStock AND p.status IN ('AVAILABLE', 'ACTIVE')")
        long countLowStockProductsByTenantId(@Param("tenantId") Long tenantId);

        @Query(value = "WITH candidates AS ( " +
                       "  SELECT p.id, p.name, p.stock, p.min_stock " +
                       "  FROM products p " +
                       "  WHERE p.tenant_id = :tenantId " +
                       "    AND p.stock <= p.min_stock " +
                       "    AND p.status IN ('AVAILABLE', 'ACTIVE') " +
                       "), reserved AS ( " +
                       "  SELECT oi.product_id, COALESCE(SUM(oi.quantity), 0) AS reserved_qty " +
                       "  FROM order_items oi " +
                       "  JOIN orders o ON o.id = oi.order_id " +
                       "  JOIN candidates c ON c.id = oi.product_id " +
                       "  WHERE oi.tenant_id = :tenantId AND o.inventory_status = 'RESERVED' " +
                       "  GROUP BY oi.product_id " +
                       "), filtered AS ( " +
                       "  SELECT c.id, c.name, GREATEST(c.stock - COALESCE(r.reserved_qty, 0), 0) AS available_stock, c.min_stock " +
                       "  FROM candidates c " +
                       "  LEFT JOIN reserved r ON r.product_id = c.id " +
                       "  WHERE GREATEST(c.stock - COALESCE(r.reserved_qty, 0), 0) <= c.min_stock " +
                       ") " +
                       "SELECT id, name, available_stock, min_stock, COUNT(*) OVER() AS total_count " +
                       "FROM filtered " +
                       "ORDER BY (available_stock - min_stock) ASC, name ASC " +
                       "LIMIT :limit",
               nativeQuery = true)
        List<Object[]> findDashboardLowStockRowsByTenantId(@Param("tenantId") Long tenantId,
                                                           @Param("limit") int limit);

       @Query("SELECT p FROM ProductEntity p WHERE p.tenantId = :tenantId AND " +
                     "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
       Page<ProductEntity> searchProductsByTenantId(@Param("tenantId") Long tenantId,
                     @Param("search") String search, Pageable pageable);

       @Query("SELECT p FROM ProductEntity p WHERE p.tenantId = :tenantId AND " +
                     "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                     "(:prepared IS NULL OR p.prepared = :prepared)")
       Page<ProductEntity> searchProductsWithPreparedByTenantId(@Param("tenantId") Long tenantId,
                     @Param("search") String search,
                     @Param("prepared") Boolean prepared,
                     Pageable pageable);

       @Query("SELECT p FROM ProductEntity p WHERE p.tenantId = :tenantId AND p.status IN ('AVAILABLE', 'ACTIVE') AND p.stock > 0")
       List<ProductEntity> findAvailableProductsByTenantId(@Param("tenantId") Long tenantId);

       @Query("SELECT p FROM ProductEntity p WHERE p.tenantId = :tenantId AND p.status IN ('AVAILABLE', 'ACTIVE') AND p.prepared = true ORDER BY p.category.id, p.name")
       Page<ProductEntity> findAvailablePreparedProductsByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @Query("SELECT p FROM ProductEntity p WHERE p.tenantId = :tenantId AND p.id IN :ids ORDER BY p.id")
       List<ProductEntity> findAllByTenantIdAndIdInOrderByIdForUpdate(@Param("tenantId") Long tenantId,
                     @Param("ids") List<Long> ids);
}
