package com.ucacue.bar.erp.vision.application;

import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import com.ucacue.bar.erp.shared.exception.ServiceUnavailableException;
import com.ucacue.bar.erp.vision.domain.VisionTypes;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.ConversionQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.FootfallQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.InsightsQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.PeakHoursQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.PredictionsQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.RangeQueryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class VisionDashboardService {

    private final VisionAiGateway visionAiGateway;
    private final TenantFeatureService tenantFeatureService;

    private final Map<String, VisionTypes.FootfallResponse> footfallCache = new ConcurrentHashMap<>();
    private final Map<String, VisionTypes.ConversionResponse> conversionCache = new ConcurrentHashMap<>();
    private final Map<String, VisionTypes.PeakHoursResponse> peakHoursCache = new ConcurrentHashMap<>();
    private final Map<String, VisionTypes.InsightsResponse> insightsCache = new ConcurrentHashMap<>();
    private final Map<String, VisionTypes.PredictionsResponse> predictionsCache = new ConcurrentHashMap<>();

    public VisionTypes.FootfallResponse footfall(String tenantCode,
                                                 LocalDateTime from,
                                                 LocalDateTime to,
                                                 List<String> cameraCodes) {
        assertVisionModule(tenantCode, ModuleCode.FOOTFALL);
        VisionTypes.Range range = range(from, to);
        String cacheKey = cacheKey("footfall", tenantCode, range, cameraCodes);

        try {
            FootfallQueryResponse response = visionAiGateway.footfall(new RangeQueryRequest(
                    tenantCode, range.from(), range.to(), "HOUR", cameraCodes));
            VisionTypes.FootfallResponse mapped = new VisionTypes.FootfallResponse(
                    tenantCode,
                    range,
                    VisionTypes.DataOrigin.LIVE,
                    false,
                    null,
                    response != null && response.totalPeople() != null ? response.totalPeople() : 0L,
                    response != null && response.series() != null
                            ? response.series().stream()
                            .map(item -> new VisionTypes.FootfallPoint(item.bucketStart(), item.peopleCount()))
                            .toList()
                            : List.of()
            );
            footfallCache.put(cacheKey, mapped);
            return mapped;
        } catch (ServiceUnavailableException ex) {
            VisionTypes.FootfallResponse cached = footfallCache.get(cacheKey);
            if (cached != null) {
                return new VisionTypes.FootfallResponse(
                        cached.tenantCode(), cached.range(), VisionTypes.DataOrigin.STALE_CACHE, true, ex.getMessage(),
                        cached.totalPeople(), cached.series());
            }
            return new VisionTypes.FootfallResponse(
                    tenantCode, range, VisionTypes.DataOrigin.UNAVAILABLE, true, ex.getMessage(), 0L, List.of());
        }
    }

    public VisionTypes.ConversionResponse conversion(String tenantCode,
                                                     LocalDateTime from,
                                                     LocalDateTime to,
                                                     List<String> cameraCodes) {
        assertVisionModule(tenantCode, ModuleCode.CONVERSION);
        VisionTypes.Range range = range(from, to);
        String cacheKey = cacheKey("conversion", tenantCode, range, cameraCodes);

        try {
            ConversionQueryResponse response = visionAiGateway.conversion(new RangeQueryRequest(
                    tenantCode, range.from(), range.to(), "DAY", cameraCodes));
            VisionTypes.ConversionResponse mapped = new VisionTypes.ConversionResponse(
                    tenantCode,
                    range,
                    VisionTypes.DataOrigin.LIVE,
                    false,
                    null,
                    response != null && response.totalPeople() != null ? response.totalPeople() : 0L,
                    response != null && response.closedSales() != null ? response.closedSales() : 0L,
                    response != null && response.conversionRate() != null ? response.conversionRate() : 0.0,
                    response != null && response.averageTicket() != null ? response.averageTicket() : BigDecimal.ZERO
            );
            conversionCache.put(cacheKey, mapped);
            return mapped;
        } catch (ServiceUnavailableException ex) {
            VisionTypes.ConversionResponse cached = conversionCache.get(cacheKey);
            if (cached != null) {
                return new VisionTypes.ConversionResponse(
                        cached.tenantCode(), cached.range(), VisionTypes.DataOrigin.STALE_CACHE, true, ex.getMessage(),
                        cached.totalPeople(), cached.closedSales(), cached.conversionRate(), cached.averageTicket());
            }
            return new VisionTypes.ConversionResponse(
                    tenantCode, range, VisionTypes.DataOrigin.UNAVAILABLE, true, ex.getMessage(), 0L, 0L, 0.0, BigDecimal.ZERO);
        }
    }

    public VisionTypes.PeakHoursResponse peakHours(String tenantCode,
                                                   LocalDateTime from,
                                                   LocalDateTime to,
                                                   List<String> cameraCodes) {
        assertVisionModule(tenantCode, ModuleCode.PEAK_HOURS);
        VisionTypes.Range range = range(from, to);
        String cacheKey = cacheKey("peak-hours", tenantCode, range, cameraCodes);

        try {
            PeakHoursQueryResponse response = visionAiGateway.peakHours(new RangeQueryRequest(
                    tenantCode, range.from(), range.to(), "HOUR", cameraCodes));
            VisionTypes.PeakHoursResponse mapped = new VisionTypes.PeakHoursResponse(
                    tenantCode,
                    range,
                    VisionTypes.DataOrigin.LIVE,
                    false,
                    null,
                    response != null && response.peakHours() != null
                            ? response.peakHours().stream()
                            .map(item -> new VisionTypes.PeakHour(
                                    item.rank() != null ? item.rank() : 0,
                                    item.bucketStart(),
                                    item.peopleCount() != null ? item.peopleCount() : 0L,
                                    item.conversionRate() != null ? item.conversionRate() : 0.0))
                            .toList()
                            : List.of()
            );
            peakHoursCache.put(cacheKey, mapped);
            return mapped;
        } catch (ServiceUnavailableException ex) {
            VisionTypes.PeakHoursResponse cached = peakHoursCache.get(cacheKey);
            if (cached != null) {
                return new VisionTypes.PeakHoursResponse(
                        cached.tenantCode(), cached.range(), VisionTypes.DataOrigin.STALE_CACHE, true, ex.getMessage(),
                        cached.peakHours());
            }
            return new VisionTypes.PeakHoursResponse(
                    tenantCode, range, VisionTypes.DataOrigin.UNAVAILABLE, true, ex.getMessage(), List.of());
        }
    }

    public VisionTypes.InsightsResponse insights(String tenantCode,
                                                 LocalDateTime from,
                                                 LocalDateTime to,
                                                 List<String> cameraCodes) {
        assertVisionModule(tenantCode, ModuleCode.TRAFFIC_INSIGHTS);
        VisionTypes.Range range = range(from, to);
        String cacheKey = cacheKey("insights", tenantCode, range, cameraCodes);

        try {
            InsightsQueryResponse response = visionAiGateway.insights(new RangeQueryRequest(
                    tenantCode, range.from(), range.to(), "DAY", cameraCodes));
            VisionTypes.InsightsResponse mapped = new VisionTypes.InsightsResponse(
                    tenantCode,
                    range,
                    VisionTypes.DataOrigin.LIVE,
                    false,
                    null,
                    response != null && response.insights() != null ? response.insights() : List.of()
            );
            insightsCache.put(cacheKey, mapped);
            return mapped;
        } catch (ServiceUnavailableException ex) {
            VisionTypes.InsightsResponse cached = insightsCache.get(cacheKey);
            if (cached != null) {
                return new VisionTypes.InsightsResponse(
                        cached.tenantCode(), cached.range(), VisionTypes.DataOrigin.STALE_CACHE, true, ex.getMessage(),
                        cached.insights());
            }
            return new VisionTypes.InsightsResponse(
                    tenantCode, range, VisionTypes.DataOrigin.UNAVAILABLE, true, ex.getMessage(), List.of());
        }
    }

    public VisionTypes.PredictionsResponse predictions(String tenantCode,
                                                       LocalDateTime from,
                                                       LocalDateTime to,
                                                       List<String> cameraCodes) {
        assertVisionModule(tenantCode, ModuleCode.PREDICTIONS);
        VisionTypes.Range range = range(from, to);
        String cacheKey = cacheKey("predictions", tenantCode, range, cameraCodes);

        try {
            PredictionsQueryResponse response = visionAiGateway.predictions(new RangeQueryRequest(
                    tenantCode, range.from(), range.to(), "DAY", cameraCodes));
            VisionTypes.PredictionsResponse mapped = new VisionTypes.PredictionsResponse(
                    tenantCode,
                    range,
                    VisionTypes.DataOrigin.LIVE,
                    false,
                    null,
                    response != null && response.predictions() != null
                            ? response.predictions().stream()
                            .map(item -> new VisionTypes.PredictionPoint(
                                    item.bucketStart(),
                                    item.expectedVisitors() != null ? item.expectedVisitors() : 0.0,
                                    item.expectedConversionRate() != null ? item.expectedConversionRate() : 0.0,
                                    item.confidenceBand()))
                            .toList()
                            : List.of(),
                    response != null && response.insights() != null ? response.insights() : List.of()
            );
            predictionsCache.put(cacheKey, mapped);
            return mapped;
        } catch (ServiceUnavailableException ex) {
            VisionTypes.PredictionsResponse cached = predictionsCache.get(cacheKey);
            if (cached != null) {
                return new VisionTypes.PredictionsResponse(
                        cached.tenantCode(), cached.range(), VisionTypes.DataOrigin.STALE_CACHE, true, ex.getMessage(),
                        cached.predictions(), cached.insights());
            }
            return new VisionTypes.PredictionsResponse(
                    tenantCode, range, VisionTypes.DataOrigin.UNAVAILABLE, true, ex.getMessage(), List.of(), List.of());
        }
    }

    private void assertVisionModule(String tenantCode, ModuleCode specificModule) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.VISION_AI);
        tenantFeatureService.assertModuleEnabled(tenantCode, specificModule);
    }

    private VisionTypes.Range range(LocalDateTime from, LocalDateTime to) {
        LocalDateTime end = to != null ? to : LocalDateTime.now();
        LocalDateTime start = from != null ? from : end.minusDays(7);
        return new VisionTypes.Range(start, end);
    }

    private String cacheKey(String prefix, String tenantCode, VisionTypes.Range range, List<String> cameraCodes) {
        String cameras = cameraCodes == null || cameraCodes.isEmpty() ? "all" : String.join(",", cameraCodes);
        return prefix + "|" + tenantCode + "|" + range.from() + "|" + range.to() + "|" + cameras;
    }
}
