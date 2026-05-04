package com.ucacue.bar.erp.vision.interfaces.http;

import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import com.ucacue.bar.erp.vision.application.VisionApiException;
import com.ucacue.bar.erp.vision.application.VisionEventService;
import com.ucacue.bar.erp.vision.application.VisionStatisticsService;
import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionCameraEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.repository.VisionCameraRepository;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionDetectionReportRow;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionErrorResponse;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionEventRequest;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionEventResponse;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionRecentEventResponse;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionStatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/vision")
@RequiredArgsConstructor
@Tag(name = "Vision AI", description = "Ingestion YOLO y consultas multi-tenant de detecciones")
public class VisionEventController {

    private final VisionEventService visionEventService;
    private final VisionStatisticsService visionStatisticsService;
    private final TenantContextResolver tenantContextResolver;
    private final TenantFeatureService tenantFeatureService;
    private final VisionCameraRepository visionCameraRepository;

    @PostMapping("/events")
    @Operation(summary = "Recibir evento de deteccion desde servicio YOLO externo")
    public ResponseEntity<VisionEventResponse> receiveEvent(
            @RequestHeader(value = "X-Vision-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) VisionEventRequest request) {
        return ResponseEntity.ok(visionEventService.receiveEvent(apiKey, idempotencyKey, request));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Consultar estadisticas Vision AI del tenant autenticado")
    public ResponseEntity<VisionStatisticsResponse> statistics(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "daily") String period) {
        TenantEntity tenant = resolveTenant(tenantCode);
        return ResponseEntity.ok(visionStatisticsService.statistics(tenant.getId(), from, to, period, null));
    }

    @GetMapping("/cameras/{cameraCode}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Consultar estadisticas Vision AI por camara del tenant autenticado")
    public ResponseEntity<VisionStatisticsResponse> cameraStatistics(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable String cameraCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "hourly") String period) {
        TenantEntity tenant = resolveTenant(tenantCode);
        VisionCameraEntity camera = visionCameraRepository.findByTenantIdAndCodeIgnoreCase(tenant.getId(), cameraCode)
                .orElseThrow(() -> new VisionApiException(
                        HttpStatus.NOT_FOUND,
                        "CAMERA_NOT_FOUND_FOR_TENANT",
                        "Camera does not exist for this tenant"));
        return ResponseEntity.ok(visionStatisticsService.statistics(tenant.getId(), from, to, period, camera.getId()));
    }

    @GetMapping("/events/recent")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Listar eventos recientes Vision AI del tenant autenticado")
    public ResponseEntity<List<VisionRecentEventResponse>> recentEvents(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(defaultValue = "50") int limit) {
        TenantEntity tenant = resolveTenant(tenantCode);
        return ResponseEntity.ok(visionStatisticsService.recentEvents(tenant.getId(), limit));
    }

    @GetMapping("/reports/detections")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Reporte de detecciones Vision AI del tenant autenticado")
    public ResponseEntity<?> detectionReport(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(defaultValue = "100") int limit) {
        TenantEntity tenant = resolveTenant(tenantCode);
        List<VisionDetectionReportRow> rows = visionStatisticsService.detectionReport(tenant.getId(), from, to, limit);
        if ("csv".equalsIgnoreCase(format)) {
            byte[] content = toCsv(rows).getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename("vision-detections.csv").build().toString())
                    .body(content);
        }
        return ResponseEntity.ok(rows);
    }

    @ExceptionHandler(VisionApiException.class)
    public ResponseEntity<VisionErrorResponse> handleVisionApiException(VisionApiException ex) {
        return ResponseEntity.status(ex.status())
                .body(VisionErrorResponse.of(ex.code(), ex.getMessage()));
    }

    private TenantEntity resolveTenant(String tenantCode) {
        TenantEntity tenant = tenantContextResolver.resolve(tenantCode);
        tenantFeatureService.assertModuleEnabled(tenant.getTenantCode(), ModuleCode.VISION_AI);
        return tenant;
    }

    private String toCsv(List<VisionDetectionReportRow> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append("eventId,eventTimestamp,cameraCode,cameraName,locationCode,label,confidence,bboxX,bboxY,bboxWidth,bboxHeight,trackingId\n");
        for (VisionDetectionReportRow row : rows) {
            csv.append(escape(row.eventId())).append(',')
                    .append(escape(row.eventTimestamp() != null ? row.eventTimestamp().toString() : null)).append(',')
                    .append(escape(row.cameraCode())).append(',')
                    .append(escape(row.cameraName())).append(',')
                    .append(escape(row.locationCode())).append(',')
                    .append(escape(row.label())).append(',')
                    .append(row.confidence()).append(',')
                    .append(row.bboxX()).append(',')
                    .append(row.bboxY()).append(',')
                    .append(row.bboxWidth()).append(',')
                    .append(row.bboxHeight()).append(',')
                    .append(escape(row.trackingId())).append('\n');
        }
        return csv.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String safeValue = neutralizeCsvFormula(value);
        String escaped = safeValue.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String neutralizeCsvFormula(String value) {
        if (value.isBlank()) {
            return value;
        }
        String stripped = value.stripLeading();
        if (stripped.isEmpty()) {
            return value;
        }
        char first = stripped.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }
}
