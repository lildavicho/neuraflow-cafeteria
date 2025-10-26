package com.ucacue.bar.repository;

import com.ucacue.bar.entity.SaleEntity;
import com.ucacue.bar.entity.SaleEntity.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<SaleEntity, Long> {
    Optional<SaleEntity> findByInvoiceNumber(String invoiceNumber);
    
    Page<SaleEntity> findByUserId(Long userId, Pageable pageable);
    
    Page<SaleEntity> findByStatus(SaleStatus status, Pageable pageable);
    
    @Query("SELECT s FROM SaleEntity s WHERE s.created BETWEEN :startDate AND :endDate")
    Page<SaleEntity> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate, 
                                     Pageable pageable);
    
    @Query("SELECT s FROM SaleEntity s WHERE s.user.id = :userId AND s.created BETWEEN :startDate AND :endDate")
    List<SaleEntity> findByUserAndDateRange(@Param("userId") Long userId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(s.total) FROM SaleEntity s WHERE s.status = 'PAID' AND s.created BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalSales(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(s) FROM SaleEntity s WHERE s.created BETWEEN :startDate AND :endDate")
    Long countSalesInPeriod(@Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate);
    
    @Query(value = "SELECT p.name, p.id, SUM(si.quantity) as total_quantity, SUM(si.subtotal) as total_sales " +
                   "FROM sale_items si " +
                   "INNER JOIN products p ON si.product_id = p.id " +
                   "INNER JOIN sales s ON si.sale_id = s.id " +
                   "WHERE s.status = 'PAID' AND s.created BETWEEN :startDate AND :endDate " +
                   "GROUP BY p.id, p.name " +
                   "ORDER BY total_quantity DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopSellingProducts(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("limit") int limit);
}
