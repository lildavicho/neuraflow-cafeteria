package com.ucacue.bar.repository;

import com.ucacue.bar.entity.SaleEntity;
import com.ucacue.bar.entity.SaleEntity.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<SaleEntity, Long> {
    Optional<SaleEntity> findByOrderId(Long orderId);

    @Query("select count(s) from SaleEntity s where s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    long countByPaidAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select s from SaleEntity s where s.saleStatus = 'PAID' and s.paidAt between :from and :to order by s.paidAt asc")
    List<SaleEntity> findByPaidAtBetweenOrderByPaidAtAsc(@Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(s.total), 0) from SaleEntity s where s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    BigDecimal totalPaidBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(s.total), 0), count(s) " +
           "from SaleEntity s " +
           "where s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    Object[] paidSummaryBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select function('date', s.paidAt), coalesce(sum(s.total), 0), count(s) " +
           "from SaleEntity s " +
           "where s.saleStatus = 'PAID' and s.paidAt between :from and :to " +
           "group by function('date', s.paidAt) " +
           "order by function('date', s.paidAt)")
    List<Object[]> aggregateDailyPaidBetween(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    @Query("select o.paymentMethod, count(s), coalesce(sum(s.total), 0) " +
           "from SaleEntity s join s.order o " +
           "where s.saleStatus = 'PAID' and s.paidAt between :from and :to " +
           "group by o.paymentMethod")
    List<Object[]> aggregateByPaymentMethodBetween(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);

    @Query("select s from SaleEntity s where s.saleStatus = :saleStatus and s.refundedAt between :from and :to order by s.refundedAt asc")
    List<SaleEntity> findBySaleStatusAndRefundedAtBetweenOrderByRefundedAtAsc(@Param("saleStatus") SaleStatus saleStatus,
                                                                               @Param("from") LocalDateTime from,
                                                                               @Param("to") LocalDateTime to);

    @Query(value = "SELECT date_trunc('hour', paid_at) AS hour_bucket, COUNT(*) AS cnt " +
                   "FROM sales " +
                   "WHERE paid_at BETWEEN :from AND :to " +
                   "AND sale_status = 'PAID' " +
                   "GROUP BY date_trunc('hour', paid_at) " +
                   "ORDER BY hour_bucket",
           nativeQuery = true)
    List<Object[]> countSalesByHourBetween(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    // ── Tenant-filtered queries ──

    @Query("select count(s) from SaleEntity s where s.tenantId = :tenantId and s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    long countByPaidAtBetweenByTenantId(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select s from SaleEntity s where s.tenantId = :tenantId and s.saleStatus = 'PAID' and s.paidAt between :from and :to order by s.paidAt asc")
    List<SaleEntity> findByPaidAtBetweenByTenantId(@Param("tenantId") Long tenantId,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(s.total), 0) from SaleEntity s where s.tenantId = :tenantId and s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    BigDecimal totalPaidBetweenByTenantId(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(s.total), 0), count(s) " +
           "from SaleEntity s " +
           "where s.tenantId = :tenantId and s.saleStatus = 'PAID' and s.paidAt between :from and :to")
    Object[] paidSummaryBetweenByTenantId(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT " +
                   "(SELECT COALESCE(SUM(s.total), 0) FROM sales s " +
                   " WHERE s.tenant_id = :tenantId AND s.sale_status = 'PAID' AND s.paid_at BETWEEN :todayFrom AND :now) AS ventas_hoy, " +
                   "(SELECT COUNT(*) FROM sales s " +
                   " WHERE s.tenant_id = :tenantId AND s.sale_status = 'PAID' AND s.paid_at BETWEEN :todayFrom AND :now) AS ordenes_hoy, " +
                   "(SELECT COALESCE(SUM(s.total), 0) FROM sales s " +
                   " WHERE s.tenant_id = :tenantId AND s.sale_status = 'PAID' AND s.paid_at BETWEEN :yesterdayFrom AND :yesterdayTo) AS ventas_ayer, " +
                   "(SELECT COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) " +
                   " FROM order_items oi " +
                   " JOIN sales s ON s.order_id = oi.order_id " +
                   " JOIN products p ON p.id = oi.product_id " +
                   " WHERE oi.tenant_id = :tenantId AND s.sale_status = 'PAID' AND s.paid_at BETWEEN :todayFrom AND :now) AS ganancias_hoy, " +
                   "(SELECT COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) " +
                   " FROM order_items oi " +
                   " JOIN sales s ON s.order_id = oi.order_id " +
                   " JOIN products p ON p.id = oi.product_id " +
                   " WHERE oi.tenant_id = :tenantId AND s.sale_status = 'PAID' AND s.paid_at BETWEEN :yesterdayFrom AND :yesterdayTo) AS ganancias_ayer",
           nativeQuery = true)
    Object[] dashboardTodayYesterdaySummaryByTenantId(@Param("tenantId") Long tenantId,
                                                       @Param("todayFrom") LocalDateTime todayFrom,
                                                       @Param("now") LocalDateTime now,
                                                       @Param("yesterdayFrom") LocalDateTime yesterdayFrom,
                                                       @Param("yesterdayTo") LocalDateTime yesterdayTo);

    @Query("select function('date', s.paidAt), coalesce(sum(s.total), 0), count(s) " +
           "from SaleEntity s " +
           "where s.tenantId = :tenantId and s.saleStatus = 'PAID' and s.paidAt between :from and :to " +
           "group by function('date', s.paidAt) " +
           "order by function('date', s.paidAt)")
    List<Object[]> aggregateDailyPaidBetweenByTenantId(@Param("tenantId") Long tenantId,
                                                        @Param("from") LocalDateTime from,
                                                        @Param("to") LocalDateTime to);

    @Query(value = "WITH sales_daily AS ( " +
                   "  SELECT CAST(s.paid_at AS DATE) AS paid_day, COALESCE(SUM(s.total), 0) AS sales_total " +
                   "  FROM sales s " +
                   "  WHERE s.tenant_id = :tenantId AND s.sale_status = 'PAID' AND s.paid_at BETWEEN :from AND :to " +
                   "  GROUP BY CAST(s.paid_at AS DATE) " +
                   "), profit_daily AS ( " +
                   "  SELECT CAST(s.paid_at AS DATE) AS paid_day, " +
                   "         COALESCE(SUM((oi.unit_price - COALESCE(p.purchase_price, 0)) * oi.quantity), 0) AS profit_total " +
                   "  FROM order_items oi " +
                   "  JOIN sales s ON s.order_id = oi.order_id " +
                   "  JOIN products p ON p.id = oi.product_id " +
                   "  WHERE oi.tenant_id = :tenantId AND s.sale_status = 'PAID' AND s.paid_at BETWEEN :from AND :to " +
                   "  GROUP BY CAST(s.paid_at AS DATE) " +
                   ") " +
                   "SELECT COALESCE(sd.paid_day, pd.paid_day) AS paid_day, " +
                   "       COALESCE(sd.sales_total, 0) AS sales_total, " +
                   "       COALESCE(pd.profit_total, 0) AS profit_total " +
                   "FROM sales_daily sd " +
                   "FULL OUTER JOIN profit_daily pd ON pd.paid_day = sd.paid_day " +
                   "ORDER BY paid_day",
           nativeQuery = true)
    List<Object[]> aggregateDailySalesAndProfitBetweenByTenantId(@Param("tenantId") Long tenantId,
                                                                  @Param("from") LocalDateTime from,
                                                                  @Param("to") LocalDateTime to);

    @Query("select o.paymentMethod, count(s), coalesce(sum(s.total), 0) " +
           "from SaleEntity s join s.order o " +
           "where s.tenantId = :tenantId and s.saleStatus = 'PAID' and s.paidAt between :from and :to " +
           "group by o.paymentMethod")
    List<Object[]> aggregateByPaymentMethodBetweenByTenantId(@Param("tenantId") Long tenantId,
                                                              @Param("from") LocalDateTime from,
                                                              @Param("to") LocalDateTime to);

    @Query("select s from SaleEntity s where s.tenantId = :tenantId and s.saleStatus = :saleStatus and s.refundedAt between :from and :to order by s.refundedAt asc")
    List<SaleEntity> findBySaleStatusAndRefundedAtBetweenByTenantId(@Param("tenantId") Long tenantId,
                                                                     @Param("saleStatus") SaleStatus saleStatus,
                                                                     @Param("from") LocalDateTime from,
                                                                     @Param("to") LocalDateTime to);

    @Query(value = "SELECT date_trunc('hour', paid_at) AS hour_bucket, COUNT(*) AS cnt " +
                   "FROM sales " +
                   "WHERE tenant_id = :tenantId " +
                   "AND paid_at BETWEEN :from AND :to " +
                   "AND sale_status = 'PAID' " +
                   "GROUP BY date_trunc('hour', paid_at) " +
                   "ORDER BY hour_bucket",
           nativeQuery = true)
    List<Object[]> countSalesByHourBetweenByTenantId(@Param("tenantId") Long tenantId,
                                                      @Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);
}
