package com.ucacue.bar.algo;

import com.ucacue.bar.repository.OrderItemRepository;
import com.ucacue.bar.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class PreparationTimePredictor {

    private static final int MIN_MINUTES = 5;
    private static final int MAX_MINUTES = 60;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Estimate estimate(Long orderId) {
        int itemsCount = safeItemsCount(orderId);
        int concurrentOrders = safeConcurrentOrders();
        double baseMinutes = safeAverageMinutes();
        boolean peakHour = isPeakHour(LocalTime.now());

        double estimate = baseMinutes;
        estimate += Math.max(0, itemsCount - 1) * 1.5;
        estimate += Math.min(concurrentOrders, 8) * 2.0;
        if (peakHour) {
            estimate += 5.0;
        }

        int minutes = (int) Math.round(clamp(estimate, MIN_MINUTES, MAX_MINUTES));
        double confidence = confidenceFromSamples();

        return new Estimate(minutes, confidence, itemsCount, concurrentOrders, baseMinutes);
    }

    public Estimate estimateFor(int itemsCount, Integer concurrentOrdersOverride) {
        int concurrentOrders = concurrentOrdersOverride != null ? concurrentOrdersOverride : safeConcurrentOrders();
        double baseMinutes = safeAverageMinutes();
        boolean peakHour = isPeakHour(LocalTime.now());

        double estimate = baseMinutes;
        estimate += Math.max(0, itemsCount - 1) * 1.5;
        estimate += Math.min(concurrentOrders, 8) * 2.0;
        if (peakHour) {
            estimate += 5.0;
        }

        int minutes = (int) Math.round(clamp(estimate, MIN_MINUTES, MAX_MINUTES));
        double confidence = confidenceFromSamples();

        return new Estimate(minutes, confidence, itemsCount, concurrentOrders, baseMinutes);
    }

    private int safeItemsCount(Long orderId) {
        Integer count = orderItemRepository.sumQuantityByOrderId(orderId);
        return count != null ? count : 0;
    }

    private int safeConcurrentOrders() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        Long count = orderRepository.countActiveSince(since);
        return count != null ? count.intValue() : 0;
    }

    private double safeAverageMinutes() {
        Double avg = orderRepository.averagePreparationMinutes();
        if (avg == null || avg.isNaN() || avg.isInfinite()) {
            return 12.0;
        }
        return Math.max(8.0, Math.min(30.0, avg));
    }

    private double confidenceFromSamples() {
        Long samples = orderRepository.countPreparationSamples();
        long count = samples != null ? samples : 0;
        if (count >= 30) {
            return 0.85;
        }
        if (count >= 10) {
            return 0.7;
        }
        return 0.5;
    }

    private boolean isPeakHour(LocalTime time) {
        int hour = time.getHour();
        return (hour >= 11 && hour <= 14) || (hour >= 18 && hour <= 20);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Value
    public static class Estimate {
        int estimatedMinutes;
        double confidence;
        int itemsCount;
        int concurrentOrders;
        double baseMinutes;
    }
}
