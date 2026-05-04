package com.ucacue.bar.erp.reports.application;

import com.ucacue.bar.erp.shared.application.TabularExportService;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErpReportsService {

    private static final int TOP_PRODUCTS_LIMIT = 8;

    private final SaleRepository saleRepository;
    private final OrderItemRepository orderItemRepository;
    private final TabularExportService tabularExportService;
    private final TenantContextResolver tenantContextResolver;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "sales-reports", key = "#tenantCode + ':' + (#from == null ? 'default' : #from.toString()) + ':' + (#to == null ? 'default' : #to.toString())")
    public SalesReportView getSalesReport(String tenantCode, LocalDate from, LocalDate to) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(29);
        ReportAggregate current = aggregate(tenantId, resolvedFrom, resolvedTo);

        long rangeDays = ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1;
        LocalDate previousTo = resolvedFrom.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(Math.max(rangeDays - 1, 0));
        ReportAggregate previous = aggregate(tenantId, previousFrom, previousTo);

        return new SalesReportView(
                resolvedFrom,
                resolvedTo,
                current.totalSales(),
                current.totalProfit(),
                current.totalOrders(),
                averageTicket(current.totalSales(), current.totalOrders()),
                current.dailySales(),
                current.topProducts(),
                current.salesByPaymentMethod(),
                new ComparisonView(
                        percentDelta(current.totalSales(), previous.totalSales()),
                        percentDelta(current.totalProfit(), previous.totalProfit()),
                        percentDelta(BigDecimal.valueOf(current.totalOrders()), BigDecimal.valueOf(previous.totalOrders()))));
    }

    @Transactional(readOnly = true)
    public TabularExportService.ExportDocument exportSalesReport(String tenantCode, String format, LocalDate from, LocalDate to) {
        SalesReportView report = getSalesReport(tenantCode, from, to);
        List<List<?>> rows = new ArrayList<>();
        for (DailySalesView day : report.dailySales()) {
            rows.add(List.of(
                    day.date(),
                    day.sales(),
                    day.profit(),
                    day.orders(),
                    averageTicket(day.sales(), day.orders())));
        }
        return tabularExportService.export(
                "Reporte de ventas",
                List.of("Fecha", "Ventas", "Utilidad", "Ordenes", "Ticket"),
                rows,
                format);
    }

    private ReportAggregate aggregate(Long tenantId, LocalDate from, LocalDate to) {
        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay().minusNanos(1);
        Object[] summary = saleRepository.paidSummaryBetweenByTenantId(tenantId, rangeStart, rangeEnd);
        BigDecimal totalSales = toBigDecimal(summaryValue(summary, 0));
        BigDecimal totalProfit = safe(orderItemRepository.totalGrossProfitBetweenByTenantId(tenantId, rangeStart, rangeEnd));
        long totalOrders = toLong(summaryValue(summary, 1));

        Map<LocalDate, DailyAccumulator> daily = new TreeMap<>();
        for (Object[] row : saleRepository.aggregateDailyPaidBetweenByTenantId(tenantId, rangeStart, rangeEnd)) {
            LocalDate date = toLocalDate(row[0]);
            if (date == null) {
                continue;
            }
            daily.computeIfAbsent(date, DailyAccumulator::new)
                    .addSales(toBigDecimal(row[1]), toLong(row[2]));
        }
        for (Object[] row : orderItemRepository.aggregateDailyGrossProfitBetweenByTenantId(tenantId, rangeStart, rangeEnd)) {
            LocalDate date = toLocalDate(row[0]);
            if (date == null) {
                continue;
            }
            daily.computeIfAbsent(date, DailyAccumulator::new)
                    .addProfit(toBigDecimal(row[1]));
        }

        List<TopProductView> topProducts = orderItemRepository
                .findTopProductsByRevenueByTenantId(tenantId, rangeStart, rangeEnd, PageRequest.of(0, TOP_PRODUCTS_LIMIT))
                .stream()
                .map(row -> new TopProductView(
                        toLong(row[0]),
                        row[1] != null ? row[1].toString() : "Producto",
                        toLong(row[2]),
                        toBigDecimal(row[3]),
                        toBigDecimal(row[4])))
                .toList();
        List<PaymentMethodView> paymentViews = saleRepository.aggregateByPaymentMethodBetweenByTenantId(tenantId, rangeStart, rangeEnd)
                .stream()
                .map(row -> new PaymentMethodView(
                        row[0] != null ? row[0].toString() : "UNSPECIFIED",
                        toLong(row[1]),
                        toBigDecimal(row[2])))
                .sorted(Comparator.comparing(PaymentMethodView::total).reversed())
                .toList();

        List<DailySalesView> dailyViews = daily.values().stream()
                .map(DailyAccumulator::toView)
                .toList();

        return new ReportAggregate(totalSales, totalProfit, totalOrders, dailyViews, topProducts, paymentViews);
    }

    private BigDecimal averageTicket(BigDecimal total, long count) {
        if (count <= 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentDelta(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return current.signum() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Object summaryValue(Object[] summary, int index) {
        Object unwrapped = unwrap(summary);
        if (unwrapped instanceof Object[] array) {
            return array.length > index ? unwrap(array[index]) : null;
        }
        return index == 0 ? unwrapped : null;
    }

    private BigDecimal toBigDecimal(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped == null) {
            return BigDecimal.ZERO;
        }
        if (unwrapped instanceof Object[] array) {
            if (array.length == 0) {
                return BigDecimal.ZERO;
            }
            if (array.length == 1) {
                return toBigDecimal(array[0]);
            }
            log.warn("Ignoring invalid numeric report value with {} columns", array.length);
            return BigDecimal.ZERO;
        }
        if (unwrapped instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (unwrapped instanceof BigInteger bigInteger) {
            return new BigDecimal(bigInteger);
        }
        if (unwrapped instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String text = textValue(unwrapped);
        String normalized = normalizeDecimalText(text);
        if (normalized.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            log.warn("Ignoring invalid numeric report value: {}", text);
            return BigDecimal.ZERO;
        }
    }

    private long toLong(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped == null) {
            return 0L;
        }
        if (unwrapped instanceof Object[] array) {
            if (array.length == 0) {
                return 0L;
            }
            if (array.length == 1) {
                return toLong(array[0]);
            }
            log.warn("Ignoring invalid long report value with {} columns", array.length);
            return 0L;
        }
        if (unwrapped instanceof Number number) {
            return number.longValue();
        }
        String text = textValue(unwrapped);
        String normalized = normalizeDecimalText(text);
        if (normalized.isBlank()) {
            return 0L;
        }
        try {
            return new BigDecimal(normalized).longValue();
        } catch (NumberFormatException ex) {
            log.warn("Ignoring invalid long report value: {}", text);
            return 0L;
        }
    }

    private String normalizeDecimalText(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isBlank() || "null".equalsIgnoreCase(normalized)) {
            return "";
        }
        if (normalized.indexOf(',') >= 0 && normalized.indexOf('.') < 0) {
            return normalized.replace(',', '.');
        }
        return normalized.replace(",", "");
    }

    private String textValue(Object value) {
        if (value instanceof char[] chars) {
            return new String(chars);
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return value.toString();
    }

    private Object unwrap(Object value) {
        if (value instanceof Object[] array && array.length == 1) {
            return unwrap(array[0]);
        }
        return value;
    }

    private LocalDate toLocalDate(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped == null) {
            return null;
        }
        if (unwrapped instanceof LocalDate localDate) {
            return localDate;
        }
        if (unwrapped instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (unwrapped instanceof Date date) {
            return date.toLocalDate();
        }
        if (unwrapped instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        return LocalDate.parse(unwrapped.toString());
    }

    @Scheduled(fixedRateString = "${app.performance.reports-cache-evict-ms:60000}")
    @CacheEvict(cacheNames = "sales-reports", allEntries = true)
    public void evictSalesReportCache() {
        log.debug("Sales report cache evicted");
    }

    private record ReportAggregate(
            BigDecimal totalSales,
            BigDecimal totalProfit,
            long totalOrders,
            List<DailySalesView> dailySales,
            List<TopProductView> topProducts,
            List<PaymentMethodView> salesByPaymentMethod) {
    }

    private static final class DailyAccumulator {
        private final LocalDate date;
        private BigDecimal sales = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;
        private long orders = 0;

        private DailyAccumulator(LocalDate date) {
            this.date = date;
        }

        private void addSales(BigDecimal sales, long orders) {
            this.sales = this.sales.add(sales);
            this.orders = this.orders + orders;
        }

        private void addProfit(BigDecimal profit) {
            this.profit = this.profit.add(profit);
        }

        private DailySalesView toView() {
            return new DailySalesView(date, sales, profit, orders);
        }
    }

    public record SalesReportView(
            LocalDate from,
            LocalDate to,
            BigDecimal totalSales,
            BigDecimal totalProfit,
            long totalOrders,
            BigDecimal avgTicket,
            List<DailySalesView> dailySales,
            List<TopProductView> topProducts,
            List<PaymentMethodView> salesByPaymentMethod,
            ComparisonView comparisonPreviousPeriod) {
    }

    public record DailySalesView(LocalDate date, BigDecimal sales, BigDecimal profit, long orders) {
    }

    public record TopProductView(Long id, String name, long quantity, BigDecimal revenue, BigDecimal profit) {
    }

    public record PaymentMethodView(String method, long count, BigDecimal total) {
    }

    public record ComparisonView(BigDecimal salesDelta, BigDecimal profitDelta, BigDecimal ordersDelta) {
    }
}
