package com.ucacue.bar.controller;

import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import com.ucacue.bar.erp.shared.application.TabularExportService;
import com.ucacue.bar.exception.UnauthorizedException;
import com.ucacue.bar.repository.UserRepository;
import com.ucacue.bar.service.InventoryAuditService;
import com.ucacue.bar.service.InventoryAuditService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
    @RequiredArgsConstructor
    @Tag(name = "Inventory KARDEX", description = "Sistema KARDEX completo de movimientos de inventario")
    @RequireTenantModule(ModuleCode.INVENTORY)
public class InventoryAuditController {

    private final InventoryAuditService inventoryAuditService;
    private final UserRepository userRepository;

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Listar movimientos recientes de inventario")
    public ResponseEntity<List<StockMovementView>> movements(
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(inventoryAuditService.listRecent(size));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Exportar inventario")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "xlsx") String format) {
        TabularExportService.ExportDocument document = inventoryAuditService.exportProducts(format, search, category);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory." + document.extension())
                .contentType(MediaType.parseMediaType(document.contentType()))
                .body(document.content());
    }

    @GetMapping("/kardex")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "KARDEX filtrado y paginado")
    public ResponseEntity<Page<StockMovementView>> kardex(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryAuditService.listFiltered(productId, type, warehouse, from, to, page, size));
    }

    @GetMapping("/kardex/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "KARDEX por producto")
    public ResponseEntity<Page<StockMovementView>> kardexByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryAuditService.listByProduct(productId, page, size));
    }

    @PostMapping("/kardex/movement")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Registrar movimiento KARDEX")
    public ResponseEntity<StockMovementView> registerMovement(
            @RequestBody KardexMovementRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(inventoryAuditService.registerMovement(request, currentUserId(authentication)));
    }

    @GetMapping("/kardex/summary")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Resumen KARDEX")
    public ResponseEntity<KardexSummary> summary() {
        return ResponseEntity.ok(inventoryAuditService.getSummary());
    }

    @GetMapping("/kardex/warehouses")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Listar bodegas disponibles")
    public ResponseEntity<List<String>> warehouses() {
        return ResponseEntity.ok(inventoryAuditService.listWarehouses());
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Alertas de inventario activas")
    public ResponseEntity<Page<AlertView>> alerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryAuditService.listActiveAlerts(page, size));
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirmar alerta de inventario")
    public ResponseEntity<Map<String, String>> acknowledgeAlert(
            @PathVariable Long alertId,
            Authentication authentication) {
        inventoryAuditService.acknowledgeAlert(alertId, currentUserId(authentication));
        return ResponseEntity.ok(Map.of("message", "Alerta confirmada"));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"))
                .getId();
    }
}
