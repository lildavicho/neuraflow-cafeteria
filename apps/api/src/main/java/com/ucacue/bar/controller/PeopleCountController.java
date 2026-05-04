package com.ucacue.bar.controller;

import com.ucacue.bar.dto.vision.VisionDetectionRequest;
import com.ucacue.bar.dto.vision.VisionEventIngestResponse;
import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import com.ucacue.bar.service.VisionAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/people-counts")
@RequiredArgsConstructor
@Tag(name = "People Counts", description = "Alias legado para ingesta Vision AI")
@RequireTenantModule(ModuleCode.VISION_AI)
public class PeopleCountController {

    private final VisionAiService visionAiService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar evento Vision AI compatible con clientes legados")
    public ResponseEntity<VisionEventIngestResponse> register(@RequestBody @Valid VisionDetectionRequest request) {
        VisionEventIngestResponse response = visionAiService.ingest(request);
        return ResponseEntity.status(response.created() ? HttpStatus.ACCEPTED : HttpStatus.OK).body(response);
    }
}
