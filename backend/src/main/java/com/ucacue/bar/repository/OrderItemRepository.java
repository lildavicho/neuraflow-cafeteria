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
}
