package com.ucacue.bar.controller;

import com.ucacue.bar.dto.vision.CameraSummaryResponse;
import com.ucacue.bar.dto.vision.CameraUpsertRequest;
import com.ucacue.bar.dto.vision.VisionDetectionRequest;
import com.ucacue.bar.dto.vision.VisionEventIngestResponse;
import com.ucacue.bar.dto.vision.VisionMetricsResponse;
import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import com.ucacue.bar.service.VisionAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/vision-ai")
@RequiredArgsConstructor
@Tag(name = "Vision AI", description = "Registro de cámaras y métricas de afluencia")
@RequireTenantModule(ModuleCode.VISION_AI)
public class VisionAiController {

    private final VisionAiService visionAiService;

    @GetMapping("/cameras")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar cámaras registradas")
    public ResponseEntity<List<CameraSummaryResponse>> cameras() {
        return ResponseEntity.ok(visionAiService.listCameras());
    }

    @PostMapping("/cameras")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar cámara Vision AI")
    public ResponseEntity<CameraSummaryResponse> registerCamera(@RequestBody @Valid CameraUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visionAiService.registerCamera(request));
    }

    @PutMapping("/cameras/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar cámara Vision AI")
    public ResponseEntity<CameraSummaryResponse> updateCamera(@PathVariable Long id,
                                                              @RequestBody @Valid CameraUpsertRequest request) {
        return ResponseEntity.ok(visionAiService.updateCamera(id, request));
    }

    @PostMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ingestar evento Vision AI listo para YOLO externo")
    public ResponseEntity<VisionEventIngestResponse> ingest(@RequestBody @Valid VisionDetectionRequest request) {
        VisionEventIngestResponse response = visionAiService.ingest(request);
        return ResponseEntity.status(response.created() ? HttpStatus.ACCEPTED : HttpStatus.OK).body(response);
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Métricas Vision AI por hora, conversión y horas pico")
    public ResponseEntity<VisionMetricsResponse> metrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(visionAiService.metrics(from, to));
    }
}
