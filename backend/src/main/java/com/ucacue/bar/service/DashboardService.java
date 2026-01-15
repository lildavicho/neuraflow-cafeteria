package com.ucacue.bar.service;

import com.ucacue.bar.entity.PeopleCountEntity;
import com.ucacue.bar.entity.SaleEntity;
import com.ucacue.bar.repository.OrderRepository;
import com.ucacue.bar.repository.PeopleCountRepository;
import com.ucacue.bar.repository.ProductRepository;
import com.ucacue.bar.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final OrderRepository orderRepository; // kept for compatibility if needed elsewhere
    private final ProductRepository productRepository;
    private final PeopleCountRepository peopleCountRepository;
    private final SaleRepository saleRepository;
    private final RealtimeGateway realtimeGateway;

    public Map<String, Object> snapshot() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime last24h = now.minusHours(24);

        BigDecimal ventasHoy = defaultBigDecimal(saleRepository.sumTotalByPaidAtBetween(startOfDay, now));
        long ordenesHoy = saleRepository.countByPaidAtBetween(startOfDay, now);
        BigDecimal ticketPromedio = ordenesHoy > 0
            ? ventasHoy.divide(BigDecimal.valueOf(ordenesHoy), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        long productosDisponibles = productRepository.count();

        Map<String, Object> data = new HashMap<>();
        data.put("ventasHoy", ventasHoy);
        data.put("ordenesHoy", ordenesHoy);
        data.put("ticketPromedio", ticketPromedio);
        data.put("productosDisponibles", productosDisponibles);
        data.put("salesSeries", buildSalesSeries(last24h, now));
        data.put("peopleSeries", buildPeopleSeries(last24h, now));
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

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private List<Map<String, Object>> buildSalesSeries(LocalDateTime from, LocalDateTime to) {
        Map<LocalDateTime, BigDecimal> buckets = new TreeMap<>();
        List<SaleEntity> sales = saleRepository.findByPaidAtBetween(from, to);
        for (SaleEntity sale : sales) {
            if (sale.getPaidAt() == null) continue;
            LocalDateTime bucket = sale.getPaidAt().truncatedTo(ChronoUnit.HOURS);
            buckets.merge(bucket, sale.getTotal(), BigDecimal::add);
        }

        return buckets.entrySet().stream()
            .map(entry -> Map.<String, Object>of(
                "timestamp", entry.getKey(),
                "total", entry.getValue()
            ))
            .toList();
    }

    private List<Map<String, Object>> buildPeopleSeries(LocalDateTime from, LocalDateTime to) {
        List<PeopleCountEntity> counts = peopleCountRepository.findBetween(from, to);
        List<Map<String, Object>> series = new ArrayList<>(counts.size());
        for (PeopleCountEntity entry : counts) {
            series.add(Map.of(
                "timestamp", entry.getTimestamp(),
                "count", entry.getCount()
            ));
        }
        return series;
    }
}
