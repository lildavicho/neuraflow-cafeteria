package com.ucacue.bar.repository;

import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.OrderEntity.OrderStatus;
import com.ucacue.bar.entity.OrderEntity.PaymentMethod;
import com.ucacue.bar.entity.OrderEntity.PaymentStatus;
import com.ucacue.bar.entity.OrderEntity.TransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.id = :id")
    java.util.Optional<OrderEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.tenantId = :tenantId and o.id = :id")
    java.util.Optional<OrderEntity> findByTenantIdAndIdForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);

    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);

    Page<OrderEntity> findByTransactionStatus(TransactionStatus transactionStatus, Pageable pageable);

    Page<OrderEntity> findByStatusAndTransactionStatus(OrderStatus status, TransactionStatus transactionStatus, Pageable pageable);

    long countByPaymentMethodAndPaymentStatusAndStatusNot(PaymentMethod paymentMethod,
                                                          PaymentStatus paymentStatus,
                                                          OrderStatus status);

    @Query("select o from OrderEntity o where o.user.id = :userId order by o.createdAt desc")
    Page<OrderEntity> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("select coalesce(sum(s.total),0) from SaleEntity s where s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    BigDecimal totalDeliveredBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(s) from SaleEntity s where s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    long deliveredCountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select o from OrderEntity o join o.sale s where s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    List<OrderEntity> findAllBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select distinct o from OrderEntity o " +
           "join fetch o.sale s " +
           "left join fetch o.items items " +
           "left join fetch items.product " +
           "where s.saleStatus = 'PAID' and s.paidAt between :from and :to " +
           "order by s.paidAt asc")
    List<OrderEntity> findDetailedPaidBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (actual_ready_at - preparation_start_at)) / 60.0) " +
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

    @Query("select o from OrderEntity o join fetch o.user " +
           "where o.paymentMethod = :paymentMethod " +
           "and o.paymentStatus = :paymentStatus " +
           "and o.status <> :excludedStatus " +
           "order by o.createdAt desc")
    List<OrderEntity> findPendingTransfers(@Param("paymentMethod") PaymentMethod paymentMethod,
                                           @Param("paymentStatus") PaymentStatus paymentStatus,
                                           @Param("excludedStatus") OrderStatus excludedStatus,
                                           Pageable pageable);

    List<OrderEntity> findByTransactionStatusAndPaidAtBetween(TransactionStatus transactionStatus,
                                                              LocalDateTime from, LocalDateTime to);

    @Query(value = "SELECT date_trunc('hour', paid_at) AS hour_bucket, COUNT(*) AS cnt " +
                   "FROM sales " +
                   "WHERE paid_at BETWEEN :from AND :to " +
                   "AND sale_status = 'PAID' " +
                   "GROUP BY date_trunc('hour', paid_at) " +
                   "ORDER BY hour_bucket",
           nativeQuery = true)
    List<Object[]> countOrdersByHourBetween(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    // ── Tenant-filtered queries ──

    java.util.Optional<OrderEntity> findByTenantIdAndId(Long tenantId, Long id);

    @Query("select distinct o from OrderEntity o " +
           "left join fetch o.user " +
           "left join fetch o.items items " +
           "left join fetch items.product " +
           "where o.tenantId = :tenantId and o.id = :id")
    java.util.Optional<OrderEntity> findDetailedByTenantIdAndId(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @Query("select distinct o from OrderEntity o " +
           "left join fetch o.user " +
           "left join fetch o.items items " +
           "left join fetch items.product " +
           "where o.tenantId = :tenantId and o.id in :ids")
    List<OrderEntity> findDetailedByTenantIdAndIdIn(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);

    Page<OrderEntity> findByTenantId(Long tenantId, Pageable pageable);

    Page<OrderEntity> findByTenantIdAndStatus(Long tenantId, OrderStatus status, Pageable pageable);

    Page<OrderEntity> findByTenantIdAndTransactionStatus(Long tenantId, TransactionStatus transactionStatus, Pageable pageable);

    Page<OrderEntity> findByTenantIdAndStatusAndTransactionStatus(Long tenantId, OrderStatus status, TransactionStatus transactionStatus, Pageable pageable);

    long countByTenantIdAndPaymentMethodAndPaymentStatusAndStatusNot(Long tenantId,
                                                                      PaymentMethod paymentMethod,
                                                                      PaymentStatus paymentStatus,
                                                                      OrderStatus status);

    @Query(value = "SELECT " +
                   "(SELECT COUNT(*) FROM orders o " +
                   " WHERE o.tenant_id = :tenantId AND o.payment_method = 'TRANSFER' " +
                   " AND o.payment_status = 'PENDING' AND o.status <> 'CANCELLED') AS pending_transfers, " +
                   "(SELECT COUNT(*) FROM acc_receivables r " +
                   " WHERE r.tenant_id = :tenantId AND r.status IN ('OPEN','PARTIALLY_PAID','OVERDUE')) AS receivables_open, " +
                   "(SELECT COUNT(*) FROM acc_payables p " +
                   " WHERE p.tenant_id = :tenantId AND p.status IN ('OPEN','PARTIALLY_PAID','OVERDUE')) AS payables_open, " +
                   "(SELECT COUNT(*) FROM tax_documents td " +
                   " WHERE td.tenant_id = :tenantId AND td.status IN ('PENDING_VALIDATION','READY_TO_SEND','SENT','REJECTED')) AS sri_pending",
           nativeQuery = true)
    Object[] dashboardOperationalCountsByTenantId(@Param("tenantId") Long tenantId);

    @Query("select o from OrderEntity o where o.tenantId = :tenantId and o.user.id = :userId order by o.createdAt desc")
    Page<OrderEntity> findByTenantIdAndUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId, Pageable pageable);

    @Query("select coalesce(sum(s.total),0) from SaleEntity s where s.tenantId = :tenantId and s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    BigDecimal totalDeliveredBetweenByTenantId(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(s) from SaleEntity s where s.tenantId = :tenantId and s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    long deliveredCountBetweenByTenantId(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select o from OrderEntity o join o.sale s where o.tenantId = :tenantId and s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    List<OrderEntity> findAllBetweenByTenantId(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select distinct o from OrderEntity o " +
           "join fetch o.sale s " +
           "left join fetch o.items items " +
           "left join fetch items.product " +
           "where o.tenantId = :tenantId and s.saleStatus = 'PAID' and s.paidAt between :from and :to " +
           "order by s.paidAt asc")
    List<OrderEntity> findDetailedPaidBetweenByTenantId(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (actual_ready_at - preparation_start_at)) / 60.0) " +
                   "FROM orders " +
                   "WHERE tenant_id = :tenantId " +
                   "AND preparation_start_at IS NOT NULL " +
                   "AND actual_ready_at IS NOT NULL",
           nativeQuery = true)
    Double averagePreparationMinutesByTenantId(@Param("tenantId") Long tenantId);

    @Query(value = "SELECT COUNT(*) FROM orders " +
                   "WHERE tenant_id = :tenantId " +
                   "AND preparation_start_at IS NOT NULL " +
                   "AND actual_ready_at IS NOT NULL",
           nativeQuery = true)
    Long countPreparationSamplesByTenantId(@Param("tenantId") Long tenantId);

    @Query(value = "SELECT COUNT(*) FROM orders " +
                   "WHERE tenant_id = :tenantId " +
                   "AND status IN ('PREPARING','CONFIRMED') " +
                   "AND created_at >= :since",
           nativeQuery = true)
    Long countActiveSinceByTenantId(@Param("tenantId") Long tenantId, @Param("since") LocalDateTime since);

    @Query("select o from OrderEntity o join fetch o.user " +
           "where o.tenantId = :tenantId " +
           "and o.paymentMethod = :paymentMethod " +
           "and o.paymentStatus = :paymentStatus " +
           "and o.status <> :excludedStatus " +
           "order by o.createdAt desc")
    List<OrderEntity> findPendingTransfersByTenantId(@Param("tenantId") Long tenantId,
                                                     @Param("paymentMethod") PaymentMethod paymentMethod,
                                                     @Param("paymentStatus") PaymentStatus paymentStatus,
                                                     @Param("excludedStatus") OrderStatus excludedStatus,
                                                     Pageable pageable);

    List<OrderEntity> findByTenantIdAndTransactionStatusAndPaidAtBetween(Long tenantId,
                                                                          TransactionStatus transactionStatus,
                                                                          LocalDateTime from, LocalDateTime to);

    @Query(value = "SELECT date_trunc('hour', s.paid_at) AS hour_bucket, COUNT(*) AS cnt " +
                   "FROM sales s " +
                   "WHERE s.tenant_id = :tenantId " +
                   "AND s.paid_at BETWEEN :from AND :to " +
                   "AND s.sale_status = 'PAID' " +
                   "GROUP BY date_trunc('hour', s.paid_at) " +
                   "ORDER BY hour_bucket",
           nativeQuery = true)
    List<Object[]> countOrdersByHourBetweenByTenantId(@Param("tenantId") Long tenantId,
                                                      @Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);
}
