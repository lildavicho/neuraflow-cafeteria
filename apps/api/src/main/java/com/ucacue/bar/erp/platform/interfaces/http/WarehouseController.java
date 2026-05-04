package com.ucacue.bar.erp.platform.interfaces.http;

import com.ucacue.bar.erp.platform.application.WarehouseService;
import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.WarehouseEntity;
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

@RestController
@RequestMapping("/erp/warehouses")
@RequiredArgsConstructor
@Tag(name = "ERP Warehouses", description = "Gestion de bodegas")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('warehouses:read')")
    @Operation(summary = "Listar bodegas paginadas")
    public ResponseEntity<Page<WarehouseResponse>> list(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<WarehouseResponse> result = warehouseService
                .list(branchId, PageRequest.of(page, size, Sort.by("name")))
                .map(WarehouseResponse::from);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('warehouses:read')")
    @Operation(summary = "Listar todas las bodegas activas (sin paginacion)")
    public ResponseEntity<List<WarehouseResponse>> listAll(
            @RequestParam(required = false) Long branchId) {
        List<WarehouseResponse> result = warehouseService.listAll(branchId).stream()
                .map(WarehouseResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('warehouses:read')")
    @Operation(summary = "Obtener bodega por ID")
    public ResponseEntity<WarehouseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(WarehouseResponse.from(warehouseService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('warehouses:create')")
    @Operation(summary = "Crear bodega")
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseEntity entity = new WarehouseEntity();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setType(request.type() != null ? request.type() : WarehouseEntity.WarehouseType.STORE);
        entity.setAddress(request.address());

        WarehouseEntity created = warehouseService.create(request.branchId(), entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(WarehouseResponse.from(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('warehouses:update')")
    @Operation(summary = "Actualizar bodega")
    public ResponseEntity<WarehouseResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateWarehouseRequest request) {
        WarehouseEntity updates = new WarehouseEntity();
        updates.setCode(request.code());
        updates.setName(request.name());
        updates.setType(request.type());
        updates.setAddress(request.address());
        updates.setActive(request.active());

        WarehouseEntity updated = warehouseService.update(id, updates);
        return ResponseEntity.ok(WarehouseResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('warehouses:delete')")
    @Operation(summary = "Desactivar bodega")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        warehouseService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // --- DTOs ---

    public record CreateWarehouseRequest(
            @NotNull Long branchId,
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 150) String name,
            WarehouseEntity.WarehouseType type,
            @Size(max = 300) String address
    ) {}

    public record UpdateWarehouseRequest(
            @Size(max = 20) String code,
            @Size(max = 150) String name,
            WarehouseEntity.WarehouseType type,
            @Size(max = 300) String address,
            Boolean active
    ) {}

    public record WarehouseResponse(
            Long id,
            Long branchId,
            String branchName,
            String code,
            String name,
            String type,
            String address,
            Boolean active,
            String createdAt,
            String updatedAt
    ) {
        public static WarehouseResponse from(WarehouseEntity e) {
            return new WarehouseResponse(
                    e.getId(),
                    e.getBranch() != null ? e.getBranch().getId() : null,
                    e.getBranch() != null ? e.getBranch().getName() : null,
                    e.getCode(), e.getName(),
                    e.getType() != null ? e.getType().name() : null,
                    e.getAddress(), e.getActive(),
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                    e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null
            );
        }
    }
}
