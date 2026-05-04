package com.ucacue.bar.erp.vision.infrastructure.http;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class VisionSystem2Contracts {

    private VisionSystem2Contracts() {
    }

    public record RangeQueryRequest(
            String tenantCode,
            LocalDateTime from,
            LocalDateTime to,
            String granularity,
            List<String> cameraCodes) {
    }

    public record FootfallPointDto(
            LocalDateTime bucketStart,
            Long peopleCount) {
    }

    public record PeakHourDto(
            Integer rank,
            LocalDateTime bucketStart,
            Long peopleCount,
            Double conversionRate) {
    }

    public record PredictionPointDto(
            LocalDateTime bucketStart,
            Double expectedVisitors,
            Double expectedConversionRate,
            String confidenceBand) {
    }

    public record FootfallQueryResponse(
            Long totalPeople,
            List<FootfallPointDto> series,
            List<String> insights) {
    }

    public record ConversionQueryResponse(
            Long totalPeople,
            Long closedSales,
            Double conversionRate,
            BigDecimal averageTicket,
            List<String> insights) {
    }

    public record PeakHoursQueryResponse(
            List<PeakHourDto> peakHours,
            List<String> insights) {
    }

    public record InsightsQueryResponse(
            List<String> insights) {
    }

    public record PredictionsQueryResponse(
            List<PredictionPointDto> predictions,
            List<String> insights) {
    }
}
