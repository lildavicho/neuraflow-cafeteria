package com.ucacue.bar.repository;

import com.ucacue.bar.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    List<OrderItemEntity> findByOrderId(Long orderId);

    @Query("select function('date', o.createdAt), sum(i.quantity) from OrderItemEntity i " +
           "join i.order o " +
           "where i.product.id = :productId " +
           "and o.status = 'DELIVERED' " +
           "and o.createdAt between :from and :to " +
           "group by function('date', o.createdAt) " +
           "order by function('date', o.createdAt)")
    List<Object[]> aggregateDailyQuantity(@Param("productId") Long productId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(i.quantity),0) from OrderItemEntity i where i.order.id = :orderId")
    Integer sumQuantityByOrderId(@Param("orderId") Long orderId);

    @Query(value = "SELECT oi.product_id, SUM(oi.quantity) AS qty " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "WHERE o.created_at BETWEEN :from AND :to " +
                   "AND o.status = 'DELIVERED' " +
                   "GROUP BY oi.product_id " +
                   "ORDER BY qty DESC",
           nativeQuery = true)
    List<Object[]> findTopProducts(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT oi.product_id, SUM(oi.quantity) AS qty " +
                   "FROM order_items oi " +
                   "JOIN orders o ON o.id = oi.order_id " +
                   "WHERE o.user_id = :userId " +
                   "GROUP BY oi.product_id " +
                   "ORDER BY qty DESC",
           nativeQuery = true)
    List<Object[]> findTopProductsForUser(@Param("userId") Long userId,
                                          org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT oi2.product_id, SUM(oi2.quantity) AS qty " +
                   "FROM order_items oi " +
                   "JOIN order_items oi2 ON oi.order_id = oi2.order_id " +
                   "WHERE oi.product_id = :productId " +
                   "AND oi2.product_id <> :productId " +
                   "GROUP BY oi2.product_id " +
                   "ORDER BY qty DESC",
           nativeQuery = true)
    List<Object[]> findCoPurchasedProducts(@Param("productId") Long productId,
                                           org.springframework.data.domain.Pageable pageable);
}
