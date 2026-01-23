package com.ucacue.bar.repository;

import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.OrderEntity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);

    @Query("select o from OrderEntity o where o.user.id = :userId order by o.createdAt desc")
    Page<OrderEntity> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("select coalesce(sum(o.total),0) from OrderEntity o where o.status = 'DELIVERED' and o.createdAt between :from and :to")
    BigDecimal totalDeliveredBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(o) from OrderEntity o where o.status = 'DELIVERED' and o.createdAt between :from and :to")
    long deliveredCountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select o from OrderEntity o where o.createdAt between :from and :to")
    List<OrderEntity> findAllBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT AVG(TIMESTAMPDIFF(MINUTE, preparation_start_at, actual_ready_at)) " +
                   "FROM orders " +
                   "WHERE preparation_start_at IS NOT NULL " +
                   "AND actual_ready_at IS NOT NULL",
           nativeQuery = true)
    Double averagePreparationMinutes();

    @Query(value = "SELECT COUNT(*) FROM orders " +
                   "WHERE preparation_start_at IS NOT NULL " +
                   "AND actual_ready_at IS NOT NULL",
           nativeQuery = true)
    Long countPreparationSamples();

    @Query(value = "SELECT COUNT(*) FROM orders " +
                   "WHERE status IN ('PREPARING','CONFIRMED') " +
                   "AND created_at >= :since",
           nativeQuery = true)
    Long countActiveSince(@Param("since") LocalDateTime since);

    @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00') AS hour_bucket, COUNT(*) AS cnt " +
                   "FROM orders " +
                   "WHERE created_at BETWEEN :from AND :to " +
                   "GROUP BY hour_bucket " +
                   "ORDER BY hour_bucket",
           nativeQuery = true)
    List<Object[]> countOrdersByHourBetween(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);
}
