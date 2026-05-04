package com.ucacue.bar.repository;

import com.ucacue.bar.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    List<OrderItemEntity> findByOrderId(Long orderId);

    @Query("select function('date', s.paidAt), sum(i.quantity) from OrderItemEntity i " +
           "join i.order o " +
           "join o.sale s " +
           "where i.product.id = :productId " +
           "and s.saleStatus = 'PAID' " +
           "and s.paidAt between :from and :to " +
           "group by function('date', s.paidAt) " +
           "order by function('date', s.paidAt)")
    List<Object[]> aggregateDailyQuantity(@Param("productId") Long productId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(i.quantity),0) from OrderItemEntity i where i.order.id = :orderId")
    Integer sumQuantityByOrderId(@Param("orderId") Long orderId);

    @Query(value = "SELECT oi.product_id, SUM(oi.quantity) AS qty " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "JOIN sales s ON s.order_id = o.id " +
                   "WHERE s.paid_at BETWEEN :from AND :to " +
                   "AND s.sale_status = 'PAID' " +
                   "GROUP BY oi.product_id " +
                   "ORDER BY qty DESC",
           nativeQuery = true)
    List<Object[]> findTopProducts(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT p.id, p.name, SUM(oi.quantity) AS qty, " +
                   "COALESCE(SUM(oi.unit_price * oi.quantity), 0) AS revenue, " +
                   "COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) AS profit " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "JOIN sales s ON s.order_id = o.id " +
                   "JOIN products p ON p.id = oi.product_id " +
                   "WHERE s.paid_at BETWEEN :from AND :to " +
                   "AND s.sale_status = 'PAID' " +
                   "GROUP BY p.id, p.name " +
                   "ORDER BY revenue DESC, qty DESC",
           nativeQuery = true)
    List<Object[]> findTopProductsByRevenue(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to,
                                            org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT p.id, p.name, SUM(oi.quantity) AS qty, " +
                   "COALESCE(SUM(oi.unit_price * oi.quantity), 0) AS revenue, " +
                   "COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) AS profit " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "JOIN sales s ON s.order_id = o.id " +
                   "JOIN products p ON p.id = oi.product_id " +
                   "WHERE s.paid_at BETWEEN :from AND :to " +
                   "AND s.sale_status = 'PAID' " +
                   "GROUP BY p.id, p.name " +
                   "HAVING COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) <= 0 " +
                   "ORDER BY profit ASC, qty DESC",
           nativeQuery = true)
    List<Object[]> findLossMakingProducts(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "JOIN sales s ON s.order_id = o.id " +
                   "JOIN products p ON p.id = oi.product_id " +
                   "WHERE s.paid_at BETWEEN :from AND :to " +
                   "AND s.sale_status = 'PAID'",
           nativeQuery = true)
    BigDecimal totalGrossProfitBetween(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    @Query(value = "SELECT CAST(s.paid_at AS DATE) AS paid_day, " +
                   "COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) AS profit " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "JOIN sales s ON s.order_id = o.id " +
                   "JOIN products p ON p.id = oi.product_id " +
                   "WHERE s.paid_at BETWEEN :from AND :to " +
                   "AND s.sale_status = 'PAID' " +
                   "GROUP BY CAST(s.paid_at AS DATE) " +
                   "ORDER BY paid_day",
           nativeQuery = true)
    List<Object[]> aggregateDailyGrossProfitBetween(@Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);

    @Query(value = "SELECT oi.product_id, SUM(oi.quantity) AS qty " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "WHERE o.user_id = :userId " +
                   "AND EXISTS (SELECT 1 FROM sales s WHERE s.order_id = o.id AND s.paid_at IS NOT NULL AND s.sale_status = 'PAID') " +
                   "GROUP BY oi.product_id " +
                   "ORDER BY qty DESC",
           nativeQuery = true)
    List<Object[]> findTopProductsForUser(@Param("userId") Long userId,
                                          org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT oi2.product_id, SUM(oi2.quantity) AS qty " +
                   "FROM order_items oi " +
                   "JOIN order_items oi2 ON oi.order_id = oi2.order_id " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "WHERE oi.product_id = :productId " +
                   "AND oi2.product_id <> :productId " +
                   "AND EXISTS (SELECT 1 FROM sales s WHERE s.order_id = o.id AND s.paid_at IS NOT NULL AND s.sale_status = 'PAID') " +
                   "GROUP BY oi2.product_id " +
                   "ORDER BY qty DESC",
           nativeQuery = true)
    List<Object[]> findCoPurchasedProducts(@Param("productId") Long productId,
                                           org.springframework.data.domain.Pageable pageable);

    @Query("select i.product.id, coalesce(sum(i.quantity), 0) " +
           "from OrderItemEntity i " +
           "join i.order o " +
           "where i.product.id in :productIds " +
           "and o.inventoryStatus = 'RESERVED' " +
           "group by i.product.id")
    List<Object[]> sumReservedQuantitiesByProductIds(@Param("productIds") List<Long> productIds);

    @Query("select i.product.id, coalesce(sum(i.quantity), 0) " +
           "from OrderItemEntity i " +
           "join i.order o " +
           "where i.tenantId = :tenantId " +
           "and i.product.id in :productIds " +
           "and o.inventoryStatus = 'RESERVED' " +
           "group by i.product.id")
    List<Object[]> sumReservedQuantitiesByProductIdsAndTenantId(@Param("tenantId") Long tenantId,
                                                                @Param("productIds") List<Long> productIds);

    @Query("select coalesce(sum(i.quantity), 0) " +
           "from OrderItemEntity i " +
           "join i.order o " +
           "where i.product.id = :productId " +
           "and o.inventoryStatus = 'RESERVED'")
    Integer sumReservedQuantityByProductId(@Param("productId") Long productId);

    // ── Tenant-filtered queries ──

    @Query("select function('date', s.paidAt), sum(i.quantity) from OrderItemEntity i " +
           "join i.order o " +
           "join o.sale s " +
           "where i.tenantId = :tenantId " +
           "and i.product.id = :productId " +
           "and s.saleStatus = 'PAID' " +
           "and s.paidAt between :from and :to " +
           "group by function('date', s.paidAt) " +
           "order by function('date', s.paidAt)")
    List<Object[]> aggregateDailyQuantityByTenantId(@Param("tenantId") Long tenantId,
                                                     @Param("productId") Long productId,
                                                     @Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);

    @Query(value = "SELECT oi.product_id, SUM(oi.quantity) AS qty " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "JOIN sales s ON s.order_id = o.id " +
                   "WHERE oi.tenant_id = :tenantId " +
                   "AND s.paid_at BETWEEN :from AND :to " +
                   "AND s.sale_status = 'PAID' " +
                   "GROUP BY oi.product_id " +
                   "ORDER BY qty DESC",
           nativeQuery = true)
    List<Object[]> findTopProductsByTenantId(@Param("tenantId") Long tenantId,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to,
                                              org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT p.id, p.name, SUM(oi.quantity) AS qty, " +
                   "COALESCE(SUM(oi.unit_price * oi.quantity), 0) AS revenue, " +
                   "COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) AS profit " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "JOIN sales s ON s.order_id = o.id " +
                   "JOIN products p ON p.id = oi.product_id " +
                   "WHERE oi.tenant_id = :tenantId " +
                   "AND s.paid_at BETWEEN :from AND :to " +
                   "AND s.sale_status = 'PAID' " +
                   "GROUP BY p.id, p.name " +
                   "ORDER BY revenue DESC, qty DESC",
           nativeQuery = true)
    List<Object[]> findTopProductsByRevenueByTenantId(@Param("tenantId") Long tenantId,
                                                       @Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to,
                                                       org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT p.id, p.name, SUM(oi.quantity) AS qty, " +
                   "COALESCE(SUM(oi.unit_price * oi.quantity), 0) AS revenue, " +
                   "COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) AS profit " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "JOIN sales s ON s.order_id = o.id " +
                   "JOIN products p ON p.id = oi.product_id " +
                   "WHERE oi.tenant_id = :tenantId " +
                   "AND s.paid_at BETWEEN :from AND :to " +
                   "AND s.sale_status = 'PAID' " +
                   "GROUP BY p.id, p.name " +
                   "HAVING COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) <= 0 " +
                   "ORDER BY profit ASC, qty DESC",
           nativeQuery = true)
    List<Object[]> findLossMakingProductsByTenantId(@Param("tenantId") Long tenantId,
                                                     @Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to,
                                                     org.springframework.data.domain.Pageable pageable);

    @Query(value = "WITH product_metrics AS ( " +
                   "  SELECT p.id, p.name, SUM(oi.quantity) AS qty, " +
                   "         COALESCE(SUM(oi.unit_price * oi.quantity), 0) AS revenue, " +
                   "         COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) AS profit " +
                   "  FROM order_items oi " +
                   "  JOIN sales s ON s.order_id = oi.order_id " +
                   "  JOIN products p ON p.id = oi.product_id " +
                   "  WHERE oi.tenant_id = :tenantId " +
                   "    AND s.paid_at BETWEEN :from AND :to " +
                   "    AND s.sale_status = 'PAID' " +
                   "  GROUP BY p.id, p.name " +
                   ") " +
                   "SELECT metric_type, id, name, qty, revenue, profit FROM ( " +
                   "  SELECT 'TOP' AS metric_type, pm.id, pm.name, pm.qty, pm.revenue, pm.profit " +
                   "  FROM product_metrics pm " +
                   "  ORDER BY pm.revenue DESC, pm.qty DESC " +
                   "  LIMIT :topLimit " +
                   ") top_rows " +
                   "UNION ALL " +
                   "SELECT metric_type, id, name, qty, revenue, profit FROM ( " +
                   "  SELECT 'LOSS' AS metric_type, pm.id, pm.name, pm.qty, pm.revenue, pm.profit " +
                   "  FROM product_metrics pm " +
                   "  WHERE pm.profit <= 0 " +
                   "  ORDER BY pm.profit ASC, pm.qty DESC " +
                   "  LIMIT 1 " +
                   ") loss_rows",
           nativeQuery = true)
    List<Object[]> findDashboardProductMetricsByTenantId(@Param("tenantId") Long tenantId,
                                                         @Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to,
                                                         @Param("topLimit") int topLimit);

    @Query(value = "SELECT COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "JOIN sales s ON s.order_id = o.id " +
                   "JOIN products p ON p.id = oi.product_id " +
                   "WHERE oi.tenant_id = :tenantId " +
                   "AND s.paid_at BETWEEN :from AND :to " +
                   "AND s.sale_status = 'PAID'",
           nativeQuery = true)
    BigDecimal totalGrossProfitBetweenByTenantId(@Param("tenantId") Long tenantId,
                                                  @Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);

    @Query(value = "SELECT CAST(s.paid_at AS DATE) AS paid_day, " +
                   "COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) AS profit " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "JOIN sales s ON s.order_id = o.id " +
                   "JOIN products p ON p.id = oi.product_id " +
                   "WHERE oi.tenant_id = :tenantId " +
                   "AND s.paid_at BETWEEN :from AND :to " +
                   "AND s.sale_status = 'PAID' " +
                   "GROUP BY CAST(s.paid_at AS DATE) " +
                   "ORDER BY paid_day",
           nativeQuery = true)
    List<Object[]> aggregateDailyGrossProfitBetweenByTenantId(@Param("tenantId") Long tenantId,
                                                               @Param("from") LocalDateTime from,
                                                               @Param("to") LocalDateTime to);

    @Query(value = "SELECT oi.product_id, SUM(oi.quantity) AS qty " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "WHERE o.tenant_id = :tenantId " +
                   "AND o.user_id = :userId " +
                   "AND EXISTS (SELECT 1 FROM sales s WHERE s.order_id = o.id AND s.paid_at IS NOT NULL AND s.sale_status = 'PAID') " +
                   "GROUP BY oi.product_id " +
                   "ORDER BY qty DESC",
           nativeQuery = true)
    List<Object[]> findTopProductsForUserByTenantId(@Param("tenantId") Long tenantId,
                                                     @Param("userId") Long userId,
                                                     org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT oi2.product_id, SUM(oi2.quantity) AS qty " +
                   "FROM order_items oi " +
                   "JOIN order_items oi2 ON oi.order_id = oi2.order_id " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "WHERE o.tenant_id = :tenantId " +
                   "AND oi.product_id = :productId " +
                   "AND oi2.product_id <> :productId " +
                   "AND EXISTS (SELECT 1 FROM sales s WHERE s.order_id = o.id AND s.paid_at IS NOT NULL AND s.sale_status = 'PAID') " +
                   "GROUP BY oi2.product_id " +
                   "ORDER BY qty DESC",
           nativeQuery = true)
    List<Object[]> findCoPurchasedProductsByTenantId(@Param("tenantId") Long tenantId,
                                                      @Param("productId") Long productId,
                                                      org.springframework.data.domain.Pageable pageable);

    @Query("select i.product.id, coalesce(sum(i.quantity), 0) " +
           "from OrderItemEntity i " +
           "join i.order o " +
           "where i.tenantId = :tenantId " +
           "and i.product.id in :productIds " +
           "and o.inventoryStatus = 'RESERVED' " +
           "group by i.product.id")
    List<Object[]> sumReservedQuantitiesByProductIdsByTenantId(@Param("tenantId") Long tenantId,
                                                                @Param("productIds") List<Long> productIds);

    @Query("select coalesce(sum(i.quantity), 0) " +
           "from OrderItemEntity i " +
           "join i.order o " +
           "where i.tenantId = :tenantId " +
           "and i.product.id = :productId " +
           "and o.inventoryStatus = 'RESERVED'")
    Integer sumReservedQuantityByProductIdByTenantId(@Param("tenantId") Long tenantId,
                                                      @Param("productId") Long productId);
}
