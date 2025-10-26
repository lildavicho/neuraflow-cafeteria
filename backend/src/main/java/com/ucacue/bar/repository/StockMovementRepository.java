package com.ucacue.bar.repository;

import com.ucacue.bar.entity.StockMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovementEntity, Long> {
    Page<StockMovementEntity> findByProductId(Long productId, Pageable pageable);
    
    Page<StockMovementEntity> findByCreatedBetween(LocalDateTime startDate, 
                                                   LocalDateTime endDate, 
                                                   Pageable pageable);
}
