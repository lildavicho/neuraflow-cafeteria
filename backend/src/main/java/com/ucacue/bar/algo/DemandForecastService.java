package com.ucacue.bar.algo;

import com.ucacue.bar.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DemandForecastService {
    
    private final SaleRepository saleRepository;
    private static final double DEFAULT_ALPHA = 0.3; // Smoothing parameter
    
    /**
     * Forecast demand using Simple Exponential Smoothing
     * @param productId Product to forecast
     * @param period Forecast period (daily, weekly)
     * @param daysAhead Number of days to forecast ahead
     * @return Forecast values
     */
    public Map<String, Object> forecastDemand(Long productId, String period, int daysAhead) {
        Map<String, Object> result = new HashMap<>();
        
        // Get historical data (last 90 days)
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(90);
        
        List<Double> historicalData = getHistoricalDemand(productId, startDate, endDate, period);
        
        if (historicalData.size() < 3) {
            result.put("error", "Insufficient historical data for forecasting");
            return result;
        }
        
        // Apply exponential smoothing
        List<Double> forecast = exponentialSmoothing(historicalData, DEFAULT_ALPHA, daysAhead);
        
        // Calculate confidence intervals
        double stdDev = calculateStandardDeviation(historicalData);
        List<Double> lowerBound = forecast.stream()
            .map(f -> Math.max(0, f - 1.96 * stdDev))
            .collect(Collectors.toList());
        List<Double> upperBound = forecast.stream()
            .map(f -> f + 1.96 * stdDev)
            .collect(Collectors.toList());
        
        result.put("forecast", forecast);
        result.put("lowerBound", lowerBound);
        result.put("upperBound", upperBound);
        result.put("historical", historicalData);
        result.put("period", period);
        result.put("productId", productId);
        result.put("alpha", DEFAULT_ALPHA);
        result.put("accuracy", calculateAccuracy(historicalData));
        
        return result;
    }
    
    /**
     * Simple Exponential Smoothing algorithm
     */
    private List<Double> exponentialSmoothing(List<Double> data, double alpha, int forecastPeriods) {
        List<Double> smoothed = new ArrayList<>();
        
        // Initialize with first observation
        double s = data.get(0);
        smoothed.add(s);
        
        // Apply smoothing to historical data
        for (int i = 1; i < data.size(); i++) {
            s = alpha * data.get(i) + (1 - alpha) * s;
            smoothed.add(s);
        }
        
        // Forecast future periods (using last smoothed value)
        double lastSmoothed = smoothed.get(smoothed.size() - 1);
        List<Double> forecast = new ArrayList<>();
        
        // Add trend component (simple linear trend)
        double trend = calculateTrend(smoothed);
        
        for (int i = 1; i <= forecastPeriods; i++) {
            forecast.add(Math.max(0, lastSmoothed + (trend * i)));
        }
        
        return forecast;
    }
    
    /**
     * Get historical demand data
     */
    private List<Double> getHistoricalDemand(Long productId, LocalDateTime startDate, 
                                            LocalDateTime endDate, String period) {
        List<Double> demands = new ArrayList<>();
        
        // Get sales data grouped by period
        List<Object[]> salesData = saleRepository.findTopSellingProducts(
            startDate, endDate, 1000
        );
        
        // Filter for specific product and aggregate by period
        Map<LocalDate, Double> aggregatedData = new HashMap<>();
        
        for (Object[] data : salesData) {
            Long prodId = ((Number) data[1]).longValue();
            if (prodId.equals(productId)) {
                Double quantity = ((Number) data[2]).doubleValue();
                // Simplified - in production, group by actual date
                LocalDate date = LocalDate.now().minusDays(demands.size());
                aggregatedData.merge(date, quantity, Double::sum);
            }
        }
        
        // Convert to list based on period
        if ("weekly".equalsIgnoreCase(period)) {
            // Aggregate by week
            demands = aggregateByWeek(aggregatedData);
        } else {
            // Daily data
            demands = new ArrayList<>(aggregatedData.values());
        }
        
        // Fill missing values with zeros
        if (demands.isEmpty()) {
            for (int i = 0; i < 30; i++) {
                demands.add(0.0);
            }
        }
        
        return demands;
    }
    
    /**
     * Aggregate daily data to weekly
     */
    private List<Double> aggregateByWeek(Map<LocalDate, Double> dailyData) {
        Map<Integer, Double> weeklyData = new TreeMap<>();
        
        for (Map.Entry<LocalDate, Double> entry : dailyData.entrySet()) {
            int week = entry.getKey().getDayOfYear() / 7;
            weeklyData.merge(week, entry.getValue(), Double::sum);
        }
        
        return new ArrayList<>(weeklyData.values());
    }
    
    /**
     * Calculate trend from smoothed data
     */
    private double calculateTrend(List<Double> data) {
        if (data.size() < 2) return 0.0;
        
        // Simple linear regression
        double n = data.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < data.size(); i++) {
            sumX += i;
            sumY += data.get(i);
            sumXY += i * data.get(i);
            sumX2 += i * i;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }
    
    /**
     * Calculate standard deviation
     */
    private double calculateStandardDeviation(List<Double> data) {
        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = data.stream()
            .mapToDouble(d -> Math.pow(d - mean, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }
    
    /**
     * Calculate forecast accuracy (MAPE)
     */
    private double calculateAccuracy(List<Double> data) {
        if (data.size() < 10) return 0.0;
        
        // Split data for validation
        int splitPoint = (int)(data.size() * 0.8);
        List<Double> train = data.subList(0, splitPoint);
        List<Double> test = data.subList(splitPoint, data.size());
        
        // Forecast on training data
        List<Double> forecast = exponentialSmoothing(train, DEFAULT_ALPHA, test.size());
        
        // Calculate MAPE (Mean Absolute Percentage Error)
        double mape = 0.0;
        int count = 0;
        
        for (int i = 0; i < Math.min(forecast.size(), test.size()); i++) {
            if (test.get(i) > 0) {
                mape += Math.abs(forecast.get(i) - test.get(i)) / test.get(i);
                count++;
            }
        }
        
        if (count > 0) {
            mape = (1 - mape / count) * 100; // Convert to accuracy percentage
        }
        
        return Math.max(0, Math.min(100, mape));
    }
    
    /**
     * Get seasonal factors for demand adjustment
     */
    public Map<String, Double> getSeasonalFactors() {
        Map<String, Double> factors = new HashMap<>();
        
        // Day of week factors (simplified)
        factors.put("Monday", 0.9);
        factors.put("Tuesday", 0.95);
        factors.put("Wednesday", 1.0);
        factors.put("Thursday", 1.1);
        factors.put("Friday", 1.2);
        factors.put("Saturday", 0.8);
        factors.put("Sunday", 0.7);
        
        return factors;
    }
}
