package com.ucacue.bar.erp.vision.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class VisionTypes {

    private VisionTypes() {
    }

    public enum DataOrigin {
        LIVE,
        STALE_CACHE,
        UNAVAILABLE
    }

    public record Range(
            LocalDateTime from,
            LocalDateTime to) {
    }

    public record FootfallPoint(
            LocalDateTime bucketStart,
            Long peopleCount) {
    }

    public record PeakHour(
            int rank,
            LocalDateTime bucketStart,
            Long peopleCount,
            Double conversionRate) {
    }

    public record PredictionPoint(
            LocalDateTime bucketStart,
            Double expectedVisitors,
            Double expectedConversionRate,
            String confidenceBand) {
    }

    public record FootfallResponse(
            String tenantCode,
            Range range,
            DataOrigin source,
            boolean degraded,
            String message,
            Long totalPeople,
            List<FootfallPoint> series) {
    }

    public record ConversionResponse(
            String tenantCode,
            Range range,
            DataOrigin source,
            boolean degraded,
            String message,
            Long totalPeople,
            Long closedSales,
            Double conversionRate,
            BigDecimal averageTicket) {
    }

    public record PeakHoursResponse(
            String tenantCode,
            Range range,
            DataOrigin source,
            boolean degraded,
            String message,
            List<PeakHour> peakHours) {
    }

    public record InsightsResponse(
            String tenantCode,
            Range range,
            DataOrigin source,
            boolean degraded,
            String message,
            List<String> insights) {
    }

    public record PredictionsResponse(
            String tenantCode,
            Range range,
            DataOrigin source,
            boolean degraded,
            String message,
            List<PredictionPoint> predictions,
            List<String> insights) {
    }
}
