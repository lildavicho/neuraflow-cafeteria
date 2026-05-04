package com.ucacue.bar.erp.platform.interfaces.http;

import com.ucacue.bar.erp.platform.application.BranchService;
import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.BranchEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/erp/branches")
@RequiredArgsConstructor
@Tag(name = "ERP Branches", description = "Gestion de sucursales")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('branches:read')")
    @Operation(summary = "Listar sucursales paginadas")
    public ResponseEntity<Page<BranchResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        Page<BranchResponse> result = branchService
                .list(PageRequest.of(page, size, Sort.by("name")), activeOnly)
                .map(BranchResponse::from);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('branches:read')")
    @Operation(summary = "Listar todas las sucursales activas (sin paginacion)")
    public ResponseEntity<List<BranchResponse>> listAll() {
        List<BranchResponse> result = branchService.listAll().stream()
                .map(BranchResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('branches:read')")
    @Operation(summary = "Obtener sucursal por ID")
    public ResponseEntity<BranchResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(BranchResponse.from(branchService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('branches:create')")
    @Operation(summary = "Crear sucursal")
    public ResponseEntity<BranchResponse> create(@Valid @RequestBody CreateBranchRequest request) {
        BranchEntity entity = new BranchEntity();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setAddress(request.address());
        entity.setCity(request.city());
        entity.setProvince(request.province());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setSriEstablishmentCode(request.sriEstablishmentCode());
        entity.setIsMain(request.isMain() != null ? request.isMain() : false);

        BranchEntity created = branchService.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(BranchResponse.from(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('branches:update')")
    @Operation(summary = "Actualizar sucursal")
    public ResponseEntity<BranchResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateBranchRequest request) {
        BranchEntity updates = new BranchEntity();
        updates.setCode(request.code());
        updates.setName(request.name());
        updates.setAddress(request.address());
        updates.setCity(request.city());
        updates.setProvince(request.province());
        updates.setPhone(request.phone());
        updates.setEmail(request.email());
        updates.setSriEstablishmentCode(request.sriEstablishmentCode());
        updates.setIsMain(request.isMain());
        updates.setActive(request.active());

        BranchEntity updated = branchService.update(id, updates);
        return ResponseEntity.ok(BranchResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('branches:delete')")
    @Operation(summary = "Desactivar sucursal (cascada bodegas)")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        branchService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or @permissionService.can('branches:update')")
    @Operation(summary = "Reactivar sucursal")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        branchService.activate(id);
        return ResponseEntity.ok().build();
    }

    // --- DTOs ---

    public record CreateBranchRequest(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 300) String address,
            @Size(max = 100) String city,
            @Size(max = 100) String province,
            @Size(max = 30) String phone,
            @Size(max = 150) String email,
            @Size(max = 10) String sriEstablishmentCode,
            Boolean isMain
    ) {}

    public record UpdateBranchRequest(
            @Size(max = 20) String code,
            @Size(max = 150) String name,
            @Size(max = 300) String address,
            @Size(max = 100) String city,
            @Size(max = 100) String province,
            @Size(max = 30) String phone,
            @Size(max = 150) String email,
            @Size(max = 10) String sriEstablishmentCode,
            Boolean isMain,
            Boolean active
    ) {}

    public record BranchResponse(
            Long id,
            String code,
            String name,
            String address,
            String city,
            String province,
            String phone,
            String email,
            String sriEstablishmentCode,
            Boolean isMain,
            Boolean active,
            String createdAt,
            String updatedAt
    ) {
        public static BranchResponse from(BranchEntity e) {
            return new BranchResponse(
                    e.getId(), e.getCode(), e.getName(), e.getAddress(),
                    e.getCity(), e.getProvince(), e.getPhone(), e.getEmail(),
                    e.getSriEstablishmentCode(), e.getIsMain(), e.getActive(),
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                    e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null
            );
        }
    }
}
