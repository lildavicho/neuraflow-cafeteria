package com.ucacue.bar.erp.platform.interfaces.http;

import com.ucacue.bar.erp.platform.application.SequenceService;
import com.ucacue.bar.erp.platform.application.SequenceService.*;
import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.SequenceEntity;
import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.SequenceEntity.ResetPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/erp/sequences")
@RequiredArgsConstructor
@Tag(name = "ERP Sequences", description = "Motor de secuencias configurable por sucursal")
public class SequenceController {

    private final SequenceService sequenceService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('branches:read')")
    @Operation(summary = "Listar todas las secuencias del tenant")
    public ResponseEntity<List<SequenceResponse>> list() {
        List<SequenceResponse> result = sequenceService.list().stream()
                .map(SequenceResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('branches:read')")
    @Operation(summary = "Obtener secuencia por ID")
    public ResponseEntity<SequenceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(SequenceResponse.from(sequenceService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('branches:create')")
    @Operation(summary = "Crear secuencia")
    public ResponseEntity<SequenceResponse> create(@Valid @RequestBody CreateSequenceRequest request) {
        CreateSequenceCommand cmd = new CreateSequenceCommand(
                request.code(), request.name(), request.branchId(),
                request.prefix(), request.suffix(),
                request.padLength(), request.incrementStep(),
                request.resetPolicy());
        SequenceEntity created = sequenceService.create(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(SequenceResponse.from(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('branches:update')")
    @Operation(summary = "Actualizar secuencia")
    public ResponseEntity<SequenceResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateSequenceRequest request) {
        UpdateSequenceCommand cmd = new UpdateSequenceCommand(
                request.name(), request.prefix(), request.suffix(),
                request.padLength(), request.incrementStep(),
                request.resetPolicy(), request.active());
        SequenceEntity updated = sequenceService.update(id, cmd);
        return ResponseEntity.ok(SequenceResponse.from(updated));
    }

    @PostMapping("/next")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generar siguiente numero de secuencia")
    public ResponseEntity<Map<String, String>> nextNumber(
            @RequestParam String code,
            @RequestParam(required = false) Long branchId) {
        String number = sequenceService.nextNumber(code, branchId);
        return ResponseEntity.ok(Map.of("number", number));
    }

    @GetMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Vista previa del siguiente numero (sin incrementar)")
    public ResponseEntity<Map<String, String>> preview(
            @RequestParam String code,
            @RequestParam(required = false) Long branchId) {
        String number = sequenceService.previewNext(code, branchId);
        return ResponseEntity.ok(Map.of("number", number));
    }

    // ── DTOs ─────────────────────────────────────────────────────────

    public record CreateSequenceRequest(
            @NotBlank @Size(max = 60) String code,
            @NotBlank @Size(max = 200) String name,
            Long branchId,
            @Size(max = 20) String prefix,
            @Size(max = 20) String suffix,
            Integer padLength,
            Integer incrementStep,
            ResetPolicy resetPolicy
    ) {}

    public record UpdateSequenceRequest(
            @Size(max = 200) String name,
            @Size(max = 20) String prefix,
            @Size(max = 20) String suffix,
            Integer padLength,
            Integer incrementStep,
            ResetPolicy resetPolicy,
            Boolean active
    ) {}

    public record SequenceResponse(
            Long id,
            Long branchId,
            String branchName,
            String code,
            String name,
            String prefix,
            String suffix,
            Integer padLength,
            Long currentNumber,
            Integer incrementStep,
            String resetPolicy,
            Boolean active,
            String createdAt,
            String updatedAt
    ) {
        public static SequenceResponse from(SequenceEntity e) {
            return new SequenceResponse(
                    e.getId(),
                    e.getBranch() != null ? e.getBranch().getId() : null,
                    e.getBranch() != null ? e.getBranch().getName() : null,
                    e.getCode(), e.getName(),
                    e.getPrefix(), e.getSuffix(), e.getPadLength(),
                    e.getCurrentNumber(), e.getIncrementStep(),
                    e.getResetPolicy() != null ? e.getResetPolicy().name() : null,
                    e.getActive(),
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                    e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        }
    }
}
