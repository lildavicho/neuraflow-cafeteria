package com.ucacue.bar.repository;

import com.ucacue.bar.entity.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<SaleEntity, Long> {
    Optional<SaleEntity> findByOrderId(Long orderId);

    long countByPaidAtBetween(LocalDateTime from, LocalDateTime to);
}
