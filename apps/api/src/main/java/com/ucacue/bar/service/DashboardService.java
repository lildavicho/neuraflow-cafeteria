package com.ucacue.bar.service;

import com.ucacue.bar.dto.dashboard.BusinessInsightDTO;
import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.OrderEntity.OrderStatus;
import com.ucacue.bar.entity.OrderEntity.PaymentMethod;
import com.ucacue.bar.entity.OrderEntity.PaymentStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.OrderRepository;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private static final int TOP_PRODUCTS_LIMIT = 5;
    private static final int LOW_STOCK_LIMIT = 4;
    private static final int PENDING_TRANSFER_LIMIT = 6;

    private final SaleRepository saleRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final RealtimeGateway realtimeGateway;
    private final InsightEngineService insightEngineService;
    private final TenantContextResolver tenantContextResolver;
    private final TaxDocumentRepository taxDocumentRepository;
    private final DashboardSnapshotCacheStore snapshotCacheStore;

    @Value("${app.dashboard.snapshot.persistent-cache-enabled:true}")
    private boolean persistentSnapshotCacheEnabled;

    @Value("${app.dashboard.snapshot.ttl-seconds:60}")
    private long persistentSnapshotTtlSeconds;

    @Transactional(readOnly = true)
    public Map<String, Object> snapshot() {
        return snapshot(tenantContextResolver.resolveCurrent().getTenantCode());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dashboard-snapshots", key = "#tenantCode == null ? 'default' : #tenantCode")
    public Map<String, Object> snapshot(String tenantCode) {
        TenantEntity tenant = tenantContextResolver.resolve(tenantCode);
        LocalDateTime now = LocalDateTime.now();
        if (persistentSnapshotCacheEnabled) {
            var cached = snapshotCacheStore.findFresh(tenant.getId(), now);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        return refreshSnapshotCache(tenant.getId(), tenant.getTenantCode(), now);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> refreshSnapshotCache(Long tenantId, String tenantCode) {
        return refreshSnapshotCache(tenantId, tenantCode, LocalDateTime.now());
    }

    private Map<String, Object> refreshSnapshotCache(Long tenantId, String tenantCode, LocalDateTime now) {
        Map<String, Object> data = buildLiveSnapshot(tenantId, now);
        if (persistentSnapshotCacheEnabled) {
            snapshotCacheStore.save(
                    tenantId,
                    tenantCode,
                    data,
                    now,
                    now.plusSeconds(Math.max(10, persistentSnapshotTtlSeconds)));
        }
        return data;
    }

    private Map<String, Object> buildLiveSnapshot(Long tenantId, LocalDateTime now) {
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfYesterday = startOfToday.minusDays(1);
        LocalDateTime endOfYesterday = startOfToday.minusNanos(1);
        LocalDateTime startOfTopWindow = startOfToday.minusDays(6);

        Object[] summary = saleRepository.dashboardTodayYesterdaySummaryByTenantId(
                tenantId, startOfToday, now, startOfYesterday, endOfYesterday);

        BigDecimal ventasHoy = defaultBigDecimal(toBigDecimal(summaryValue(summary, 0)));
        long ordenesHoy = toLong(summaryValue(summary, 1));
        BigDecimal ventasAyer = defaultBigDecimal(toBigDecimal(summaryValue(summary, 2)));
        BigDecimal gananciasHoy = defaultBigDecimal(toBigDecimal(summaryValue(summary, 3)));
        BigDecimal gananciasAyer = defaultBigDecimal(toBigDecimal(summaryValue(summary, 4)));
        BigDecimal ticketPromedio = ordenesHoy > 0
                ? ventasHoy.divide(BigDecimal.valueOf(ordenesHoy), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> comparacionVentas = buildComparison(ventasHoy, ventasAyer);
        Map<String, Object> comparacionGanancias = buildComparison(gananciasHoy, gananciasAyer);

        List<Object[]> productMetricRows = orderItemRepository.findDashboardProductMetricsByTenantId(
                tenantId, startOfTopWindow, now, TOP_PRODUCTS_LIMIT);
        List<Map<String, Object>> productosTop = buildTopProducts(productMetricRows);
        Map<String, Object> productoEstrella = productosTop.isEmpty() ? null : productosTop.get(0);
        Map<String, Object> productoConPerdida = buildLossMakingProduct(productMetricRows);

        List<Object[]> lowStockRows = productRepository.findDashboardLowStockRowsByTenantId(tenantId, LOW_STOCK_LIMIT);
        List<Map<String, Object>> productosBajoStock = buildLowStockProducts(lowStockRows);
        long stockCriticoCount = lowStockRows.isEmpty() ? 0L : toLong(lowStockRows.get(0)[4]);
        List<Map<String, Object>> pagosTransferenciaPendientes = buildPendingTransfers(tenantId);
        Object[] operationalCounts = orderRepository.dashboardOperationalCountsByTenantId(tenantId);
        long totalPagosPendientes = toLong(summaryValue(operationalCounts, 0));
        long cuentasPorCobrarAbiertas = toLong(summaryValue(operationalCounts, 1));
        long cuentasPorPagarAbiertas = toLong(summaryValue(operationalCounts, 2));
        long documentosSriPendientes = toLong(summaryValue(operationalCounts, 3));
        LocalDate today = now.toLocalDate();
        LocalDate historicalStart = today.minusDays(6);
        Map<LocalDate, BigDecimal> historicalSales = new HashMap<>();
        Map<LocalDate, BigDecimal> historicalProfit = new HashMap<>();
        for (Object[] row : saleRepository.aggregateDailySalesAndProfitBetweenByTenantId(
                tenantId, historicalStart.atStartOfDay(), today.plusDays(1).atStartOfDay().minusNanos(1))) {
            LocalDate date = toLocalDate(row[0]);
            if (date != null) {
                historicalSales.put(date, defaultBigDecimal(toBigDecimal(row[1])));
                historicalProfit.put(date, defaultBigDecimal(toBigDecimal(row[2])));
            }
        }
        List<BusinessInsightDTO> insights = insightEngineService.buildInsights(
                ventasHoy,
                gananciasHoy,
                comparacionVentas,
                comparacionGanancias,
                productoEstrella,
                productoConPerdida,
                productosBajoStock,
                totalPagosPendientes);

        Map<String, Object> data = new HashMap<>();
        data.put("ventasHoy", ventasHoy);
        data.put("ventasAyer", ventasAyer);
        data.put("gananciasHoy", gananciasHoy);
        data.put("gananciasAyer", gananciasAyer);
        data.put("ordenesHoy", ordenesHoy);
        data.put("ticketPromedio", ticketPromedio);
        data.put("comparacionVentas", comparacionVentas);
        data.put("comparacionGanancias", comparacionGanancias);
        data.put("resumenNegocio", buildBusinessSummary(ventasHoy, gananciasHoy, comparacionGanancias));
        data.put("historicoVentas", buildSeries(historicalStart, today, historicalSales));
        data.put("historicoGanancias", buildSeries(historicalStart, today, historicalProfit));
        data.put("productosTop", productosTop);
        data.put("productoEstrella", productoEstrella);
        data.put("productosBajoStock", productosBajoStock);
        data.put("stockCriticoCount", stockCriticoCount);
        data.put("pagosTransferenciaPendientes", pagosTransferenciaPendientes);
        data.put("cuentasPorCobrarAbiertas", cuentasPorCobrarAbiertas);
        data.put("cuentasPorPagarAbiertas", cuentasPorPagarAbiertas);
        data.put("documentosSriPendientes", documentosSriPendientes);
        data.put("documentosSriRecientes", buildRecentSriDocuments(tenantId));
        data.put("insights", insights);
        data.put("alertas", buildAlerts(
                now,
                ventasHoy,
                gananciasHoy,
                comparacionGanancias,
                productosBajoStock,
                totalPagosPendientes,
                cuentasPorCobrarAbiertas,
                cuentasPorPagarAbiertas,
                documentosSriPendientes));
        data.put("generatedAt", now);
        return data;
    }

    @Async
    public void publishSnapshotAsync() {
        try {
            realtimeGateway.dashboard(snapshot());
        } catch (Exception ex) {
            log.debug("Unable to publish dashboard snapshot: {}", ex.getMessage());
        }
    }

    private Map<String, Object> buildComparison(BigDecimal actual, BigDecimal previous) {
        BigDecimal delta = actual.subtract(previous).setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentage;

        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            percentage = actual.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(100);
        } else {
            percentage = delta
                    .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        String trend = delta.compareTo(BigDecimal.ZERO) > 0
                ? "UP"
                : delta.compareTo(BigDecimal.ZERO) < 0 ? "DOWN" : "FLAT";

        return Map.of(
                "actual", actual,
                "anterior", previous,
                "deltaMonto", delta,
                "deltaPorcentaje", percentage,
                "tendencia", trend);
    }

    private Map<String, Object> buildBusinessSummary(BigDecimal ventasHoy,
                                                     BigDecimal gananciasHoy,
                                                     Map<String, Object> comparacionGanancias) {
        BigDecimal deltaGanancias = (BigDecimal) comparacionGanancias.get("deltaMonto");
        String status;
        String title;
        String detail;

        if (ventasHoy.compareTo(BigDecimal.ZERO) <= 0) {
            status = "NEUTRAL";
            title = "Aun no hay ventas registradas hoy";
            detail = "Necesitas ventas pagadas para medir si el negocio gana o pierde dinero.";
        } else if (gananciasHoy.compareTo(BigDecimal.ZERO) <= 0) {
            status = "DANGER";
            title = "Hoy estas perdiendo dinero";
            detail = "La ganancia estimada del dia esta en rojo. Revisa costos y margen por producto.";
        } else if (deltaGanancias.compareTo(BigDecimal.ZERO) < 0) {
            status = "WARNING";
            title = "Sigues ganando, pero menos que ayer";
            detail = "La utilidad de hoy cayo frente al dia anterior. Conviene revisar mezcla de ventas y precios.";
        } else {
            status = "GOOD";
            title = "Hoy estas ganando dinero";
            detail = "Las ventas pagadas cubren costo estimado y la ganancia va en la direccion correcta.";
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("status", status);
        summary.put("title", title);
        summary.put("detail", detail);
        summary.put("ventasHoy", ventasHoy);
        summary.put("gananciasHoy", gananciasHoy);
        summary.put("deltaGanancias", deltaGanancias);
        return summary;
    }

    private List<Map<String, Object>> buildTopProducts(List<Object[]> rows) {
        List<Map<String, Object>> products = new ArrayList<>();
        for (Object[] row : rows) {
            if (!"TOP".equals(row[0] != null ? row[0].toString() : null)) {
                continue;
            }
            Map<String, Object> product = new HashMap<>();
            product.put("id", toLong(row[1]));
            product.put("name", row[2] != null ? row[2].toString() : "Producto");
            product.put("quantity", toInt(row[3]));
            product.put("revenue", toBigDecimal(row[4]));
            product.put("profit", toBigDecimal(row[5]));
            products.add(product);
        }
        return products;
    }

    private Map<String, Object> buildLossMakingProduct(List<Object[]> rows) {
        for (Object[] row : rows) {
            if ("LOSS".equals(row[0] != null ? row[0].toString() : null)) {
                Map<String, Object> product = new HashMap<>();
                product.put("id", toLong(row[1]));
                product.put("name", row[2] != null ? row[2].toString() : "Producto");
                product.put("quantity", toInt(row[3]));
                product.put("revenue", toBigDecimal(row[4]));
                product.put("profit", toBigDecimal(row[5]));
                return product;
            }
        }
        return null;
    }

    private List<Map<String, Object>> buildLowStockProducts(List<Object[]> rows) {
        return rows.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", toLong(row[0]));
                    item.put("name", row[1] != null ? row[1].toString() : "Producto");
                    item.put("availableStock", toInt(row[2]));
                    item.put("minStock", toInt(row[3]));
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> buildPendingTransfers(Long tenantId) {
        return orderRepository.findPendingTransfersByTenantId(
                        tenantId,
                        PaymentMethod.TRANSFER,
                        PaymentStatus.PENDING,
                        OrderStatus.CANCELLED,
                        PageRequest.of(0, PENDING_TRANSFER_LIMIT))
                .stream()
                .map(this::toPendingTransferItem)
                .toList();
    }

    private Map<String, Object> toPendingTransferItem(OrderEntity order) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", order.getId());
        item.put("userName", order.getUser() != null ? order.getUser().getName() : null);
        item.put("userEmail", order.getUser() != null ? order.getUser().getEmail() : null);
        item.put("total", defaultBigDecimal(order.getTotal()));
        item.put("paymentReference", order.getPaymentReference());
        item.put("createdAt", order.getCreatedAt());
        return item;
    }

    private List<Map<String, Object>> buildAlerts(LocalDateTime now,
                                                  BigDecimal ventasHoy,
                                                  BigDecimal gananciasHoy,
                                                  Map<String, Object> comparacionGanancias,
                                                  List<Map<String, Object>> productosBajoStock,
                                                  long totalPagosPendientes,
                                                  long cuentasPorCobrarAbiertas,
                                                  long cuentasPorPagarAbiertas,
                                                  long documentosSriPendientes) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        BigDecimal deltaGanancias = (BigDecimal) comparacionGanancias.get("deltaMonto");

        if (gananciasHoy.compareTo(BigDecimal.ZERO) <= 0 && ventasHoy.compareTo(BigDecimal.ZERO) > 0) {
            alerts.add(alert("danger", "Ganancia en rojo", "Las ventas de hoy no estan dejando utilidad."));
        } else if (deltaGanancias.compareTo(BigDecimal.ZERO) < 0) {
            alerts.add(alert("warning", "Ganancia por debajo de ayer", "Hoy vas por debajo del ritmo de utilidad del dia anterior."));
        } else if (ventasHoy.compareTo(BigDecimal.ZERO) == 0 && now.getHour() >= 12) {
            alerts.add(alert("warning", "Sin ventas al mediodia", "Todavia no hay ventas pagadas registradas en el dia."));
        }

        if (!productosBajoStock.isEmpty()) {
            alerts.add(alert(
                    "warning",
                    "Productos con stock bajo",
                    productosBajoStock.size() + " productos necesitan reposicion o ajuste de inventario."));
        }

        if (totalPagosPendientes > 0) {
            alerts.add(alert(
                    "info",
                    "Transferencias pendientes",
                    totalPagosPendientes + " pagos por transferencia siguen sin confirmacion."));
        }

        if (documentosSriPendientes > 0) {
            alerts.add(alert(
                    "warning",
                    "Documentos SRI pendientes",
                    documentosSriPendientes + " comprobantes siguen pendientes de validacion, envio o correccion."));
        }

        if (cuentasPorCobrarAbiertas > 0) {
            alerts.add(alert(
                    "info",
                    "Cartera por cobrar abierta",
                    cuentasPorCobrarAbiertas + " cuentas por cobrar siguen abiertas y requieren seguimiento."));
        }

        if (cuentasPorPagarAbiertas > 0) {
            alerts.add(alert(
                    "info",
                    "Obligaciones por pagar abiertas",
                    cuentasPorPagarAbiertas + " cuentas por pagar siguen abiertas y afectan caja futura."));
        }

        return alerts;
    }

    private List<Map<String, Object>> buildHistoricalSalesSeries(Long tenantId, LocalDate today) {
        LocalDate start = today.minusDays(6);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay().minusNanos(1);
        Map<LocalDate, BigDecimal> values = new HashMap<>();
        for (Object[] row : saleRepository.aggregateDailyPaidBetweenByTenantId(tenantId, from, to)) {
            LocalDate date = toLocalDate(row[0]);
            if (date != null) {
                values.put(date, defaultBigDecimal(toBigDecimal(row[1])));
            }
        }
        return buildSeries(start, today, values);
    }

    private List<Map<String, Object>> buildHistoricalProfitSeries(Long tenantId, LocalDate today) {
        LocalDate start = today.minusDays(6);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay().minusNanos(1);
        Map<LocalDate, BigDecimal> values = new HashMap<>();
        for (Object[] row : orderItemRepository.aggregateDailyGrossProfitBetweenByTenantId(tenantId, from, to)) {
            LocalDate date = toLocalDate(row[0]);
            if (date != null) {
                values.put(date, defaultBigDecimal(toBigDecimal(row[1])));
            }
        }
        return buildSeries(start, today, values);
    }

    private List<Map<String, Object>> buildSeries(LocalDate start, LocalDate end, Map<LocalDate, BigDecimal> values) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            series.add(seriesPoint(date, values.getOrDefault(date, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))));
        }
        return series;
    }

    private Map<String, Object> seriesPoint(LocalDate date, BigDecimal value) {
        Map<String, Object> point = new HashMap<>();
        point.put("date", date);
        point.put("value", value);
        return point;
    }

    private List<Map<String, Object>> buildRecentSriDocuments(Long tenantId) {
        return taxDocumentRepository.findTop5ByTenantIdAndStatusInOrderByCreatedAtDesc(
                        tenantId, EnumSet.of(TaxDocumentStatus.READY_TO_SEND, TaxDocumentStatus.SENT, TaxDocumentStatus.REJECTED, TaxDocumentStatus.AUTHORIZED))
                .stream()
                .map(document -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", document.getId());
                    item.put("status", document.getStatus().name());
                    item.put("documentCode", document.getDocumentType().getDocumentCode());
                    item.put("sequentialNumber", document.getSequentialNumber());
                    item.put("authorizationCode", document.getAuthorizationCode());
                    item.put("total", defaultBigDecimal(document.getTotalAmount()));
                    item.put("issueDate", document.getIssueDate());
                    item.put("lastStatusAt", document.getLastStatusAt());
                    return item;
                })
                .toList();
    }

    private Map<String, Object> alert(String level, String title, String detail) {
        Map<String, Object> item = new HashMap<>();
        item.put("level", level);
        item.put("title", title);
        item.put("detail", detail);
        return item;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private Long toLong(Object value) {
        Object unwrapped = unwrap(value);
        return unwrapped == null ? 0L : ((Number) unwrapped).longValue();
    }

    private Integer toInt(Object value) {
        Object unwrapped = unwrap(value);
        return unwrapped == null ? 0 : ((Number) unwrapped).intValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (unwrapped instanceof BigDecimal bigDecimal) {
            return bigDecimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (unwrapped instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(unwrapped.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private Object summaryValue(Object[] summary, int index) {
        Object unwrapped = unwrap(summary);
        if (unwrapped instanceof Object[] array) {
            return array.length > index ? unwrap(array[index]) : null;
        }
        return index == 0 ? unwrapped : null;
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
}
