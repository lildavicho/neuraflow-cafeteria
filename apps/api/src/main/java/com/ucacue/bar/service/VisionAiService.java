package com.ucacue.bar.service;

import com.ucacue.bar.dto.vision.CameraSummaryResponse;
import com.ucacue.bar.dto.vision.CameraUpsertRequest;
import com.ucacue.bar.dto.vision.VisionCameraMetricsResponse;
import com.ucacue.bar.dto.vision.VisionConversionResponse;
import com.ucacue.bar.dto.vision.VisionDetectionRequest;
import com.ucacue.bar.dto.vision.VisionEventIngestResponse;
import com.ucacue.bar.dto.vision.VisionHourlyPointResponse;
import com.ucacue.bar.dto.vision.VisionMetricsResponse;
import com.ucacue.bar.dto.vision.VisionPeakHourResponse;
import com.ucacue.bar.dto.vision.VisionRangeResponse;
import com.ucacue.bar.dto.vision.VisionSummaryResponse;
import com.ucacue.bar.entity.CameraEntity;
import com.ucacue.bar.entity.SaleEntity;
import com.ucacue.bar.entity.VisionDetectionEventEntity;
import com.ucacue.bar.entity.VisionHourlyMetricEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.CameraRepository;
import com.ucacue.bar.repository.SaleRepository;
import com.ucacue.bar.repository.VisionDetectionEventRepository;
import com.ucacue.bar.repository.VisionHourlyMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisionAiService {

    private static final String DEFAULT_EVENT_TYPE = "ENTRY";
    private static final String DEFAULT_SOURCE = "YOLO_EXTERNAL";

    private final CameraRepository cameraRepository;
    private final VisionDetectionEventRepository visionDetectionEventRepository;
    private final VisionHourlyMetricRepository visionHourlyMetricRepository;
    private final SaleRepository saleRepository;
    private final RealtimeGateway realtimeGateway;
    private final TenantContextResolver tenantContextResolver;

    @Transactional(readOnly = true)
    public List<CameraSummaryResponse> listCameras() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        LocalDateTime currentHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        return cameraRepository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(camera -> new CameraSummaryResponse(
                        camera.getId(),
                        camera.getName(),
                        camera.getRtspUrl(),
                        camera.getDescription(),
                        camera.getLocation(),
                        camera.getEnabled(),
                        camera.getStreamPath(),
                        camera.getCreatedAt(),
                        camera.getUpdatedAt(),
                        visionDetectionEventRepository.findLastTimestampByTenantIdAndCameraId(tenantId, camera.getId()),
                        visionHourlyMetricRepository.findByTenantIdAndCameraIdAndHourStart(tenantId, camera.getId(), currentHour)
                                .map(metric -> metric.getUniquePeople() != null ? metric.getUniquePeople().longValue() : 0L)
                                .orElse(0L)))
                .toList();
    }

    @Transactional
    public CameraSummaryResponse registerCamera(CameraUpsertRequest request) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        String normalizedName = normalizeRequired(request.name(), "El nombre de la cámara es obligatorio");
        if (cameraRepository.existsByTenantIdAndNameIgnoreCase(tenantId, normalizedName)) {
            throw new BadRequestException("Ya existe una cámara registrada con ese nombre");
        }

        CameraEntity entity = new CameraEntity();
        entity.setTenantId(tenantId);
        applyCamera(entity, request, normalizedName);
        CameraEntity saved = cameraRepository.save(entity);
        return toCameraSummary(saved);
    }

    @Transactional
    public CameraSummaryResponse updateCamera(Long id, CameraUpsertRequest request) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        CameraEntity entity = cameraRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException("No existe la cámara solicitada"));
        if (entity.getTenantId() == null || !entity.getTenantId().equals(tenantId)) {
            throw new NotFoundException("No existe la cámara solicitada");
        }
        String normalizedName = normalizeRequired(request.name(), "El nombre de la cámara es obligatorio");
        if (!entity.getName().equalsIgnoreCase(normalizedName) && cameraRepository.existsByTenantIdAndNameIgnoreCase(tenantId, normalizedName)) {
            throw new BadRequestException("Ya existe una cámara registrada con ese nombre");
        }

        applyCamera(entity, request, normalizedName);
        CameraEntity saved = cameraRepository.save(entity);
        return toCameraSummary(saved);
    }

    @Transactional
    public VisionEventIngestResponse ingest(VisionDetectionRequest request) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        CameraEntity camera = cameraRepository.findByTenantIdAndId(tenantId, request.cameraId())
                .orElseThrow(() -> new NotFoundException("La cámara indicada no está registrada"));
        if (camera.getTenantId() == null || !camera.getTenantId().equals(tenantId)) {
            throw new NotFoundException("La cámara indicada no está registrada");
        }
        if (!Boolean.TRUE.equals(camera.getEnabled())) {
            throw new BadRequestException("La cámara está deshabilitada");
        }

        LocalDateTime timestamp = request.timestamp() != null ? request.timestamp() : LocalDateTime.now();
        if (timestamp.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BadRequestException("La marca de tiempo no puede estar en el futuro");
        }
        String eventType = normalizeOptional(request.eventType(), DEFAULT_EVENT_TYPE, 30).toUpperCase();
        int peopleCount = sanitizeCount(request.peopleCount());
        int uniquePeople = resolveUniquePeople(request, peopleCount);
        String eventKey = resolveEventKey(request, timestamp, peopleCount, uniquePeople, eventType);

        VisionDetectionEventEntity existing = visionDetectionEventRepository
                .findByTenantIdAndCameraIdAndEventKey(tenantId, camera.getId(), eventKey)
                .orElse(null);
        if (existing != null) {
            VisionHourlyMetricEntity currentMetric = getOrCreateHourlyMetric(tenantId, camera, existing.getTimestamp().truncatedTo(ChronoUnit.HOURS));
            return buildIngestResponse(false, existing, currentMetric);
        }

        VisionDetectionEventEntity entity = new VisionDetectionEventEntity();
        entity.setTenantId(tenantId);
        entity.setCamera(camera);
        entity.setEventKey(eventKey);
        entity.setTimestamp(timestamp);
        entity.setPeopleCount(peopleCount);
        entity.setUniquePeople(uniquePeople);
        entity.setEventType(eventType);
        entity.setSource(normalizeOptional(request.source(), DEFAULT_SOURCE, 40).toUpperCase());
        entity.setTrackIds(joinTrackIds(request.trackIds()));
        VisionDetectionEventEntity saved = visionDetectionEventRepository.save(entity);

        LocalDateTime hourStart = timestamp.truncatedTo(ChronoUnit.HOURS);
        VisionHourlyMetricEntity hourlyMetric = getOrCreateHourlyMetric(tenantId, camera, hourStart);
        hourlyMetric.setUniquePeople(defaultInt(hourlyMetric.getUniquePeople()) + uniquePeople);
        hourlyMetric.setEventCount(defaultInt(hourlyMetric.getEventCount()) + 1);
        hourlyMetric.setLastEventAt(timestamp);
        VisionHourlyMetricEntity savedMetric = visionHourlyMetricRepository.save(hourlyMetric);

        try {
            realtimeGateway.people(Map.of(
                    "event", "vision.count",
                    "cameraId", camera.getId(),
                    "cameraName", camera.getName(),
                    "timestamp", timestamp,
                    "count", peopleCount,
                    "uniquePeople", uniquePeople,
                    "hourStart", hourStart,
                    "hourUniquePeople", savedMetric.getUniquePeople()
            ));
        } catch (Exception ex) {
            log.warn("Unable to publish vision detection update for camera {}", camera.getId(), ex);
        }

        return buildIngestResponse(true, saved, savedMetric);
    }

    @Transactional(readOnly = true)
    public VisionMetricsResponse metrics(LocalDateTime from, LocalDateTime to) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        Range range = resolveRange(from, to);
        List<CameraEntity> cameras = cameraRepository.findByTenantIdOrderByNameAsc(tenantId);
        List<VisionHourlyMetricEntity> metrics = visionHourlyMetricRepository
                .findByTenantIdAndHourStartBetweenOrderByHourStartAsc(tenantId, range.start().truncatedTo(ChronoUnit.HOURS), range.end());
        List<SaleEntity> sales = saleRepository.findByPaidAtBetweenByTenantId(tenantId, range.start(), range.end());

        Map<LocalDateTime, HourAccumulator> peopleByHour = new TreeMap<>();
        Map<Long, CameraAccumulator> cameraAccumulators = new LinkedHashMap<>();
        LocalDateTime currentHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        for (CameraEntity camera : cameras) {
            cameraAccumulators.put(camera.getId(), new CameraAccumulator());
        }

        for (VisionHourlyMetricEntity metric : metrics) {
            if (metric.getHourStart() == null || metric.getCamera() == null) {
                continue;
            }
            HourAccumulator hourAccumulator = peopleByHour.computeIfAbsent(metric.getHourStart(), ignored -> new HourAccumulator());
            hourAccumulator.people += defaultInt(metric.getUniquePeople());

            CameraAccumulator cameraAccumulator = cameraAccumulators.get(metric.getCamera().getId());
            if (cameraAccumulator != null) {
                cameraAccumulator.uniquePeople += defaultInt(metric.getUniquePeople());
                cameraAccumulator.eventCount += defaultInt(metric.getEventCount());
                cameraAccumulator.lastEventAt = latest(cameraAccumulator.lastEventAt, metric.getLastEventAt());
                if (metric.getHourStart().equals(currentHour)) {
                    cameraAccumulator.lastHourPeople += defaultInt(metric.getUniquePeople());
                }
            }
        }

        BigDecimal revenue = BigDecimal.ZERO;
        long totalSales = 0;
        for (SaleEntity sale : sales) {
            if (sale.getPaidAt() == null) {
                continue;
            }
            LocalDateTime hourStart = sale.getPaidAt().truncatedTo(ChronoUnit.HOURS);
            HourAccumulator hourAccumulator = peopleByHour.computeIfAbsent(hourStart, ignored -> new HourAccumulator());
            hourAccumulator.sales += 1;
            hourAccumulator.revenue = hourAccumulator.revenue.add(sale.getTotal() != null ? sale.getTotal() : BigDecimal.ZERO);
            totalSales++;
            revenue = revenue.add(sale.getTotal() != null ? sale.getTotal() : BigDecimal.ZERO);
        }

        long totalPeople = peopleByHour.values().stream().mapToLong(hour -> hour.people).sum();
        List<VisionHourlyPointResponse> hourlySeries = new ArrayList<>();
        peopleByHour.forEach((hourStart, accumulator) -> hourlySeries.add(new VisionHourlyPointResponse(
                hourStart,
                accumulator.people,
                accumulator.sales,
                accumulator.revenue.setScale(2, RoundingMode.HALF_UP),
                conversionRate(accumulator.sales, accumulator.people))));

        List<VisionPeakHourResponse> peakHours = hourlySeries.stream()
                .filter(point -> point.people() > 0)
                .sorted(Comparator.comparingLong(VisionHourlyPointResponse::people).reversed()
                        .thenComparing(VisionHourlyPointResponse::hourStart))
                .limit(5)
                .map(point -> new VisionPeakHourResponse(
                        point.hourStart(),
                        point.people(),
                        point.sales(),
                        point.salesConversionRate()))
                .toList();

        List<VisionCameraMetricsResponse> cameraMetrics = cameras.stream()
                .map(camera -> {
                    CameraAccumulator accumulator = cameraAccumulators.get(camera.getId());
                    return new VisionCameraMetricsResponse(
                            camera.getId(),
                            camera.getName(),
                            camera.getLocation(),
                            camera.getEnabled(),
                            accumulator != null ? accumulator.uniquePeople : 0L,
                            accumulator != null ? accumulator.eventCount : 0L,
                            accumulator != null ? accumulator.lastEventAt : null,
                            accumulator != null ? accumulator.lastHourPeople : 0L
                    );
                })
                .sorted(Comparator.comparingLong(VisionCameraMetricsResponse::uniquePeople).reversed()
                        .thenComparing(VisionCameraMetricsResponse::cameraName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new VisionMetricsResponse(
                new VisionRangeResponse(range.start(), range.end()),
                new VisionSummaryResponse(
                        cameras.size(),
                        (int) cameras.stream().filter(camera -> Boolean.TRUE.equals(camera.getEnabled())).count(),
                        totalPeople,
                        totalSales,
                        revenue.setScale(2, RoundingMode.HALF_UP)),
                new VisionConversionResponse(
                        totalPeople,
                        totalSales,
                        revenue.setScale(2, RoundingMode.HALF_UP),
                        safeDivide(revenue, totalPeople),
                        conversionRate(totalSales, totalPeople)),
                hourlySeries,
                peakHours,
                cameraMetrics
        );
    }

    private CameraSummaryResponse toCameraSummary(CameraEntity camera) {
        return new CameraSummaryResponse(
                camera.getId(),
                camera.getName(),
                camera.getRtspUrl(),
                camera.getDescription(),
                camera.getLocation(),
                camera.getEnabled(),
                camera.getStreamPath(),
                camera.getCreatedAt(),
                camera.getUpdatedAt(),
                null,
                0L
        );
    }

    private void applyCamera(CameraEntity entity, CameraUpsertRequest request, String normalizedName) {
        entity.setName(normalizedName);
        entity.setRtspUrl(normalizeRequired(request.rtspUrl(), "La URL RTSP es obligatoria"));
        entity.setDescription(normalizeOptional(request.description(), null, 200));
        entity.setLocation(normalizeOptional(request.location(), null, 50));
        entity.setEnabled(request.enabled() != null ? request.enabled() : Boolean.TRUE);
        entity.setStreamPath(normalizeOptional(request.streamPath(), null, 50));
    }

    private VisionEventIngestResponse buildIngestResponse(boolean created,
                                                          VisionDetectionEventEntity event,
                                                          VisionHourlyMetricEntity metric) {
        return new VisionEventIngestResponse(
                created,
                event.getId(),
                event.getCamera().getId(),
                event.getCamera().getName(),
                event.getEventKey(),
                event.getTimestamp(),
                metric.getHourStart(),
                metric.getUniquePeople() != null ? metric.getUniquePeople().longValue() : 0L
        );
    }

    private String resolveEventKey(VisionDetectionRequest request,
                                   LocalDateTime timestamp,
                                   int peopleCount,
                                   int uniquePeople,
                                   String eventType) {
        if (StringUtils.hasText(request.eventKey())) {
            return request.eventKey().trim();
        }

        String fingerprint = request.cameraId() + "|" + timestamp.truncatedTo(ChronoUnit.SECONDS) + "|" + peopleCount
                + "|" + uniquePeople + "|" + eventType + "|" + joinTrackIds(request.trackIds());
        return DigestUtils.md5DigestAsHex(fingerprint.getBytes(StandardCharsets.UTF_8));
    }

    private VisionHourlyMetricEntity getOrCreateHourlyMetric(Long tenantId, CameraEntity camera, LocalDateTime hourStart) {
        return visionHourlyMetricRepository.findByTenantIdAndCameraIdAndHourStart(tenantId, camera.getId(), hourStart)
                .orElseGet(() -> {
                    VisionHourlyMetricEntity metric = new VisionHourlyMetricEntity();
                    metric.setTenantId(tenantId);
                    metric.setCamera(camera);
                    metric.setHourStart(hourStart);
                    metric.setUniquePeople(0);
                    metric.setEventCount(0);
                    return metric;
                });
    }

    private int resolveUniquePeople(VisionDetectionRequest request, int peopleCount) {
        if (request.trackIds() != null && !request.trackIds().isEmpty()) {
            return (int) request.trackIds().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .count();
        }
        if (request.uniquePeople() != null) {
            return sanitizeCount(request.uniquePeople());
        }
        return peopleCount;
    }

    private String joinTrackIds(List<String> trackIds) {
        if (trackIds == null || trackIds.isEmpty()) {
            return null;
        }
        return trackIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    private LocalDateTime latest(LocalDateTime current, LocalDateTime candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.isAfter(current)) {
            return candidate;
        }
        return current;
    }

    private int sanitizeCount(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new BadRequestException("El conteo de personas no puede ser negativo");
        }
        return value;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value, String fallback, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private Double conversionRate(long sales, long people) {
        if (people <= 0) {
            return 0.0;
        }
        double rate = (sales * 100.0) / people;
        return Math.round(rate * 100.0) / 100.0;
    }

    private BigDecimal safeDivide(BigDecimal value, long divisor) {
        if (divisor <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    private int defaultInt(Integer value) {
        return value != null ? value : 0;
    }

    private Range resolveRange(LocalDateTime from, LocalDateTime to) {
        LocalDateTime end = to != null ? to : LocalDateTime.now();
        LocalDateTime start = from != null ? from : end.minusDays(7);
        return new Range(start, end);
    }

    private record Range(LocalDateTime start, LocalDateTime end) {
    }

    private static class HourAccumulator {
        private long people;
        private long sales;
        private BigDecimal revenue = BigDecimal.ZERO;
    }

    private static class CameraAccumulator {
        private long uniquePeople;
        private long eventCount;
        private LocalDateTime lastEventAt;
        private long lastHourPeople;
    }
}
