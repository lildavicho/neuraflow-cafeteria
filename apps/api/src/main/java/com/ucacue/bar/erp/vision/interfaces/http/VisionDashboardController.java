package com.ucacue.bar.erp.vision.interfaces.http;

import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import com.ucacue.bar.erp.vision.application.VisionDashboardService;
import com.ucacue.bar.erp.vision.domain.VisionTypes.ConversionResponse;
import com.ucacue.bar.erp.vision.domain.VisionTypes.FootfallResponse;
import com.ucacue.bar.erp.vision.domain.VisionTypes.InsightsResponse;
import com.ucacue.bar.erp.vision.domain.VisionTypes.PeakHoursResponse;
import com.ucacue.bar.erp.vision.domain.VisionTypes.PredictionsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/erp/vision-ai")
@RequiredArgsConstructor
@Tag(name = "ERP Vision AI", description = "Consultas desacopladas del Sistema 2 para dashboard comercial")
@RequireTenantModule(ModuleCode.VISION_AI)
public class VisionDashboardController {

    private final VisionDashboardService visionDashboardService;

    @GetMapping("/afluencia")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(summary = "Consultar afluencia por rango de fechas")
    public ResponseEntity<FootfallResponse> footfall(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<String> cameraCodes) {
        return ResponseEntity.ok(visionDashboardService.footfall(tenantCode, from, to, cameraCodes));
    }

    @GetMapping("/conversion")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(summary = "Consultar conversion personas vs ventas")
    public ResponseEntity<ConversionResponse> conversion(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<String> cameraCodes) {
        return ResponseEntity.ok(visionDashboardService.conversion(tenantCode, from, to, cameraCodes));
    }

    @GetMapping("/horas-pico")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(summary = "Consultar horas pico")
    public ResponseEntity<PeakHoursResponse> peakHours(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<String> cameraCodes) {
        return ResponseEntity.ok(visionDashboardService.peakHours(tenantCode, from, to, cameraCodes));
    }

    @GetMapping("/insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(summary = "Consultar insights de trafico")
    public ResponseEntity<InsightsResponse> insights(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<String> cameraCodes) {
        return ResponseEntity.ok(visionDashboardService.insights(tenantCode, from, to, cameraCodes));
    }

    @GetMapping("/predicciones")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(summary = "Consultar predicciones para dashboard")
    public ResponseEntity<PredictionsResponse> predictions(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<String> cameraCodes) {
        return ResponseEntity.ok(visionDashboardService.predictions(tenantCode, from, to, cameraCodes));
    }
}
