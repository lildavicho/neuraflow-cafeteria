package com.ucacue.bar.repository;

import com.ucacue.bar.entity.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<SaleEntity, Long> {
    Optional<SaleEntity> findByOrderId(Long orderId);

    long countByPaidAtBetween(LocalDateTime from, LocalDateTime to);

    List<SaleEntity> findByPaidAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(s.total),0) from SaleEntity s where s.paidAt between :from and :to")
    BigDecimal sumTotalByPaidAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
