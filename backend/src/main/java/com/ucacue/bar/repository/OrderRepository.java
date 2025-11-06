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
}
