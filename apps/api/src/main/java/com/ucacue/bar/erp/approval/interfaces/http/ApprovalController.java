package com.ucacue.bar.erp.approval.interfaces.http;

import com.ucacue.bar.erp.approval.application.ApprovalService;
import com.ucacue.bar.erp.approval.application.ApprovalService.*;
import com.ucacue.bar.erp.approval.domain.ApprovalTypes.ApprovalAction;
import com.ucacue.bar.erp.approval.infrastructure.persistence.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/erp/approvals")
@RequiredArgsConstructor
@Tag(name = "ERP Approvals", description = "Motor de aprobaciones configurable")
public class ApprovalController {

    private final ApprovalService approvalService;

    // ── Definitions ──────────────────────────────────────────────────

    @GetMapping("/definitions")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('approvals:configure')")
    @Operation(summary = "Listar flujos de aprobación activos")
    public ResponseEntity<List<DefinitionResponse>> listDefinitions() {
        List<DefinitionResponse> result = approvalService.listDefinitions().stream()
                .map(DefinitionResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/definitions/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('approvals:configure')")
    @Operation(summary = "Obtener flujo de aprobación por ID")
    public ResponseEntity<DefinitionDetailResponse> getDefinition(@PathVariable Long id) {
        return ResponseEntity.ok(DefinitionDetailResponse.from(approvalService.getDefinition(id)));
    }

    @PostMapping("/definitions")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('approvals:configure')")
    @Operation(summary = "Crear flujo de aprobación")
    public ResponseEntity<DefinitionDetailResponse> createDefinition(
            @Valid @RequestBody CreateDefinitionRequest request) {
        List<StepCommand> steps = request.steps() != null
                ? request.steps().stream()
                    .map(s -> new StepCommand(s.name(), s.approverRole(), s.requiredCount()))
                    .toList()
                : List.of();

        CreateDefinitionCommand cmd = new CreateDefinitionCommand(
                request.code(), request.name(), request.entityType(),
                request.conditionExpr(), steps);

        ApprovalDefinitionEntity created = approvalService.createDefinition(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(DefinitionDetailResponse.from(created));
    }

    @PutMapping("/definitions/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('approvals:configure')")
    @Operation(summary = "Actualizar flujo de aprobación")
    public ResponseEntity<DefinitionDetailResponse> updateDefinition(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDefinitionRequest request) {
        UpdateDefinitionCommand cmd = new UpdateDefinitionCommand(
                request.name(), request.conditionExpr(), request.active());
        ApprovalDefinitionEntity updated = approvalService.updateDefinition(id, cmd);
        return ResponseEntity.ok(DefinitionDetailResponse.from(updated));
    }

    // ── Instances (bandeja de aprobaciones) ──────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('approvals:read')")
    @Operation(summary = "Listar solicitudes pendientes de aprobación")
    public ResponseEntity<Page<InstanceResponse>> listPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InstanceResponse> result = approvalService
                .listPending(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(InstanceResponse::from);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('approvals:read')")
    @Operation(summary = "Obtener detalle de solicitud de aprobación")
    public ResponseEntity<InstanceDetailResponse> getInstance(@PathVariable Long id) {
        return ResponseEntity.ok(InstanceDetailResponse.from(approvalService.getInstance(id)));
    }

    @PostMapping("/request")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('approvals:read')")
    @Operation(summary = "Solicitar aprobación para una entidad")
    public ResponseEntity<InstanceResponse> requestApproval(
            @Valid @RequestBody RequestApprovalRequest request) {
        ApprovalInstanceEntity created = approvalService.requestApproval(
                request.entityType(), request.entityId(), request.requestedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(InstanceResponse.from(created));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('approvals:approve')")
    @Operation(summary = "Aprobar solicitud")
    public ResponseEntity<InstanceResponse> approve(@PathVariable Long id,
                                                     @RequestBody ActionRequest request) {
        ApprovalInstanceEntity result = approvalService.processAction(
                id, ApprovalAction.APPROVE, request.userId(), request.userName(), request.comment());
        return ResponseEntity.ok(InstanceResponse.from(result));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('approvals:reject')")
    @Operation(summary = "Rechazar solicitud")
    public ResponseEntity<InstanceResponse> reject(@PathVariable Long id,
                                                    @RequestBody ActionRequest request) {
        ApprovalInstanceEntity result = approvalService.processAction(
                id, ApprovalAction.REJECT, request.userId(), request.userName(), request.comment());
        return ResponseEntity.ok(InstanceResponse.from(result));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('approvals:approve')")
    @Operation(summary = "Cancelar solicitud de aprobación")
    public ResponseEntity<InstanceResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(InstanceResponse.from(approvalService.cancel(id)));
    }

    @GetMapping("/counts")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('approvals:read')")
    @Operation(summary = "Conteo de aprobaciones pendientes")
    public ResponseEntity<Map<String, Long>> counts() {
        return ResponseEntity.ok(Map.of("pending", approvalService.countPending()));
    }

    // ── Request DTOs ─────────────────────────────────────────────────

    public record CreateDefinitionRequest(
            @NotBlank @Size(max = 60) String code,
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 60) String entityType,
            String conditionExpr,
            List<StepRequest> steps
    ) {}

    public record StepRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 32) String approverRole,
            Integer requiredCount
    ) {}

    public record UpdateDefinitionRequest(
            @Size(max = 200) String name,
            String conditionExpr,
            Boolean active
    ) {}

    public record RequestApprovalRequest(
            @NotBlank String entityType,
            @NotNull Long entityId,
            Long requestedBy
    ) {}

    public record ActionRequest(
            Long userId,
            String userName,
            String comment
    ) {}

    // ── Response DTOs ────────────────────────────────────────────────

    public record DefinitionResponse(Long id, String code, String name, String entityType, Boolean active) {
        public static DefinitionResponse from(ApprovalDefinitionEntity e) {
            return new DefinitionResponse(e.getId(), e.getCode(), e.getName(), e.getEntityType(), e.getActive());
        }
    }

    public record DefinitionDetailResponse(
            Long id, String code, String name, String entityType,
            String conditionExpr, Boolean active, List<StepResponse> steps,
            String createdAt, String updatedAt
    ) {
        public static DefinitionDetailResponse from(ApprovalDefinitionEntity e) {
            List<StepResponse> steps = e.getSteps() != null
                    ? e.getSteps().stream()
                        .map(s -> new StepResponse(s.getId(), s.getStepOrder(), s.getName(),
                                s.getApproverRole(), s.getRequiredCount()))
                        .toList()
                    : List.of();
            return new DefinitionDetailResponse(
                    e.getId(), e.getCode(), e.getName(), e.getEntityType(),
                    e.getConditionExpr(), e.getActive(), steps,
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                    e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        }
    }

    public record StepResponse(Long id, Integer order, String name, String approverRole, Integer requiredCount) {}

    public record InstanceResponse(
            Long id, String entityType, Long entityId, Integer currentStep,
            String status, String definitionName, Long requestedBy,
            String createdAt, String updatedAt
    ) {
        public static InstanceResponse from(ApprovalInstanceEntity e) {
            return new InstanceResponse(
                    e.getId(), e.getEntityType(), e.getEntityId(), e.getCurrentStep(),
                    e.getStatus().name(),
                    e.getDefinition() != null ? e.getDefinition().getName() : null,
                    e.getRequestedBy(),
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                    e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        }
    }

    public record InstanceDetailResponse(
            Long id, String entityType, Long entityId, Integer currentStep,
            String status, String definitionName, Long requestedBy,
            List<ActionResponse> actions,
            String createdAt, String updatedAt
    ) {
        public static InstanceDetailResponse from(ApprovalInstanceEntity e) {
            List<ActionResponse> actions = e.getActions() != null
                    ? e.getActions().stream()
                        .map(a -> new ActionResponse(a.getId(), a.getStepOrder(), a.getAction().name(),
                                a.getUserId(), a.getUserName(), a.getComment(),
                                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null))
                        .toList()
                    : List.of();
            return new InstanceDetailResponse(
                    e.getId(), e.getEntityType(), e.getEntityId(), e.getCurrentStep(),
                    e.getStatus().name(),
                    e.getDefinition() != null ? e.getDefinition().getName() : null,
                    e.getRequestedBy(), actions,
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                    e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        }
    }

    public record ActionResponse(Long id, Integer stepOrder, String action, Long userId,
                                  String userName, String comment, String createdAt) {}
}
