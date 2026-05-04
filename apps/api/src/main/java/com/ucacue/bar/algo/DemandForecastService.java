package com.ucacue.bar.algo;

import com.ucacue.bar.dto.ml.DemandForecastDTO;
import com.ucacue.bar.dto.ml.MlResponse;
import com.ucacue.bar.dto.ml.StaffingRecommendationDTO;
import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.SaleRepository;
import com.ucacue.bar.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class DemandForecastService {

    private static final double DEFAULT_ALPHA = 0.3;
    private static final int MIN_DAILY_POINTS = 7;
    private static final int MIN_WEEKLY_POINTS = 4;
    private static final int MIN_NON_ZERO_POINTS = 2;

    private final OrderItemRepository orderItemRepository;
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final TenantContextResolver tenantContextResolver;

    public MlResponse<DemandForecastDTO> forecastDemand(Long productId, String period, int daysAhead) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        String normalizedPeriod = "weekly".equalsIgnoreCase(period) ? "weekly" : "daily";
        int horizon = Math.max(1, daysAhead);

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays("weekly".equals(normalizedPeriod) ? 180 : 90);

        List<Double> historicalData = getHistoricalDemand(tenantId, productId, startDate, endDate, normalizedPeriod);
        long nonZeroPoints = historicalData.stream().filter(value -> value > 0.0).count();
        int minRequiredPoints = "weekly".equals(normalizedPeriod) ? MIN_WEEKLY_POINTS : MIN_DAILY_POINTS;

        if (historicalData.size() < minRequiredPoints || nonZeroPoints < MIN_NON_ZERO_POINTS) {
            return MlResponse.insufficient();
        }

        List<Double> forecast = exponentialSmoothing(historicalData, DEFAULT_ALPHA, horizon);
        double stdDev = calculateStandardDeviation(historicalData);
        List<Double> lowerBound = forecast.stream()
                .map(value -> round(Math.max(0, value - (1.96 * stdDev))))
                .toList();
        List<Double> upperBound = forecast.stream()
                .map(value -> round(value + (1.96 * stdDev)))
                .toList();

        Double accuracy = calculateAccuracy(historicalData);
        double confidence = confidenceFromSeries(historicalData.size(), nonZeroPoints, accuracy);

        DemandForecastDTO result = new DemandForecastDTO(
                productId,
                normalizedPeriod,
                horizon,
                roundList(historicalData),
                roundList(forecast),
                lowerBound,
                upperBound,
                accuracy != null ? round(accuracy) : null);

        return MlResponse.ok(result, confidence);
    }

    public MlResponse<List<StaffingRecommendationDTO>> getStaffingRecommendation(int daysAhead) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        int days = Math.max(1, daysAhead);
        LocalDateTime end = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime start = end.minusDays(14);

        List<Object[]> rows = saleRepository.countSalesByHourBetweenByTenantId(tenantId, start, end);
        if (rows == null || rows.isEmpty()) {
            return MlResponse.insufficient();
        }

        Map<Integer, Long> sumByHour = new HashMap<>();
        Map<LocalDate, Map<Integer, Long>> byDay = new HashMap<>();

        for (Object[] row : rows) {
            LocalDateTime bucket = toLocalDateTime(row[0]);
            if (bucket == null) {
                continue;
            }
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            sumByHour.merge(bucket.getHour(), count, Long::sum);
            byDay.computeIfAbsent(bucket.toLocalDate(), key -> new HashMap<>())
                    .merge(bucket.getHour(), count, Long::sum);
        }

        if (byDay.size() < 2) {
            return MlResponse.insufficient();
        }

        double daysObserved = byDay.size();
        Map<Integer, Double> avgByHour = new HashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            double avg = sumByHour.getOrDefault(hour, 0L) / daysObserved;
            avgByHour.put(hour, avg);
        }

        List<StaffingRecommendationDTO> recommendations = new ArrayList<>();
        for (int i = 0; i < days * 24; i++) {
            LocalDateTime slot = end.plusHours(i);
            double predictedOrders = avgByHour.getOrDefault(slot.getHour(), 0.0);
            int recommendedStaff = predictedOrders <= 0.0
                    ? 0
                    : Math.max(1, (int) Math.ceil(predictedOrders / 4.0));

            recommendations.add(new StaffingRecommendationDTO(
                    slot,
                    Math.round(predictedOrders),
                    recommendedStaff));
        }

        long nonZeroHours = avgByHour.values().stream().filter(value -> value > 0.0).count();
        double confidence = confidenceFromSeries((int) rows.size(), nonZeroHours, Math.min(100.0, daysObserved * 7.0));
        return MlResponse.ok(recommendations, confidence);
    }

    private List<Double> exponentialSmoothing(List<Double> data, double alpha, int forecastPeriods) {
        List<Double> smoothed = new ArrayList<>();
        double current = data.get(0);
        smoothed.add(current);

        for (int i = 1; i < data.size(); i++) {
            current = alpha * data.get(i) + ((1 - alpha) * current);
            smoothed.add(current);
        }

        double trend = calculateTrend(smoothed);
        double lastSmoothed = smoothed.get(smoothed.size() - 1);
        List<Double> forecast = new ArrayList<>();
        for (int i = 1; i <= forecastPeriods; i++) {
            forecast.add(Math.max(0, lastSmoothed + (trend * i)));
        }
        return forecast;
    }

    private List<Double> getHistoricalDemand(Long tenantId,
                                             Long productId,
                                             LocalDateTime startDate,
                                             LocalDateTime endDate,
                                             String period) {
        List<Object[]> salesData = orderItemRepository.aggregateDailyQuantityByTenantId(tenantId, productId, startDate, endDate);
        Map<LocalDate, Double> dailyData = aggregateDaily(salesData);
        if (dailyData.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, Double> completedSeries = completeDailySeries(dailyData, endDate.toLocalDate());
        return "weekly".equalsIgnoreCase(period)
                ? aggregateByWeek(completedSeries)
                : completedSeries.values().stream().toList();
    }

    private Map<LocalDate, Double> aggregateDaily(List<Object[]> salesData) {
        Map<LocalDate, Double> aggregatedData = new TreeMap<>();
        for (Object[] row : salesData) {
            LocalDate date = row[0] instanceof java.sql.Date sqlDate
                    ? sqlDate.toLocalDate()
                    : row[0] instanceof LocalDate localDate ? localDate : null;
            if (date == null) {
                continue;
            }
            double quantity = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            aggregatedData.merge(date, quantity, Double::sum);
        }
        return aggregatedData;
    }

    private Map<LocalDate, Double> completeDailySeries(Map<LocalDate, Double> dailyData, LocalDate endDate) {
        Map<LocalDate, Double> completed = new TreeMap<>();
        LocalDate cursor = dailyData.keySet().iterator().next();
        while (!cursor.isAfter(endDate)) {
            completed.put(cursor, dailyData.getOrDefault(cursor, 0.0));
            cursor = cursor.plusDays(1);
        }
        return completed;
    }

    private List<Double> aggregateByWeek(Map<LocalDate, Double> dailyData) {
        Map<Integer, Double> weeklyData = new TreeMap<>();
        for (Map.Entry<LocalDate, Double> entry : dailyData.entrySet()) {
            int week = entry.getKey().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                    + (entry.getKey().getYear() * 100);
            weeklyData.merge(week, entry.getValue(), Double::sum);
        }
        return weeklyData.values().stream().toList();
    }

    private double calculateTrend(List<Double> data) {
        if (data.size() < 2) {
            return 0.0;
        }

        double n = data.size();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;

        for (int i = 0; i < data.size(); i++) {
            sumX += i;
            sumY += data.get(i);
            sumXY += i * data.get(i);
            sumX2 += i * i;
        }

        double denominator = (n * sumX2) - (sumX * sumX);
        if (denominator == 0.0) {
            return 0.0;
        }
        return (n * sumXY - (sumX * sumY)) / denominator;
    }

    private double calculateStandardDeviation(List<Double> data) {
        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = data.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private Double calculateAccuracy(List<Double> data) {
        long nonZeroPoints = data.stream().filter(value -> value > 0.0).count();
        if (data.size() < 12 || nonZeroPoints < 3) {
            return null;
        }

        int splitPoint = Math.max(1, (int) Math.floor(data.size() * 0.8));
        if (splitPoint >= data.size()) {
            return null;
        }

        List<Double> train = data.subList(0, splitPoint);
        List<Double> test = data.subList(splitPoint, data.size());
        List<Double> forecast = exponentialSmoothing(train, DEFAULT_ALPHA, test.size());

        double mape = 0.0;
        int count = 0;
        for (int i = 0; i < Math.min(forecast.size(), test.size()); i++) {
            if (test.get(i) > 0.0) {
                mape += Math.abs(forecast.get(i) - test.get(i)) / test.get(i);
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        return Math.max(0.0, Math.min(100.0, (1 - (mape / count)) * 100.0));
    }

    private double confidenceFromSeries(long sampleSize, long nonZeroPoints, Double accuracy) {
        double sampleFactor = Math.min(1.0, sampleSize / 30.0);
        double signalFactor = Math.min(1.0, nonZeroPoints / 10.0);
        double accuracyFactor = accuracy != null ? Math.min(1.0, accuracy / 100.0) : 0.4;
        return round((sampleFactor * 0.45) + (signalFactor * 0.30) + (accuracyFactor * 0.25));
    }

    private List<Double> roundList(List<Double> values) {
        return values.stream().map(this::round).toList();
    }

    private double round(double value) {
        return Math.round(Math.max(0.0, value) * 100.0) / 100.0;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().atStartOfDay();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof String text) {
            try {
                return LocalDateTime.parse(text.replace(" ", "T"));
            } catch (Exception ex) {
                log.debug("Unable to parse timestamp value: {}", text, ex);
                return null;
            }
        }
        return null;
    }
}
