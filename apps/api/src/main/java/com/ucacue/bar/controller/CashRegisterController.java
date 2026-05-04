package com.ucacue.bar.controller;

import com.ucacue.bar.entity.CashRegisterEntity;
import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.service.CashRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/cash-register")
@RequiredArgsConstructor
@Tag(name = "Cash Register", description = "Apertura y cierre de caja")
@RequireTenantModule(ModuleCode.POS)
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    @PostMapping("/open")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER') or @permissionService.can('cash:open')")
    @Operation(summary = "Abrir caja con monto inicial")
    public ResponseEntity<Map<String, Object>> openRegister(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        BigDecimal openingAmount = parseAmount(body.get("openingAmount"), BigDecimal.ZERO, "openingAmount");
        CashRegisterEntity register = cashRegisterService.openRegister(authentication.getName(), openingAmount);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(register));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER') or @permissionService.can('cash:close')")
    @Operation(summary = "Cerrar caja y generar Z-report")
    public ResponseEntity<Map<String, Object>> closeRegister(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        BigDecimal actualCash = body.containsKey("actualCash")
                ? parseAmount(body.get("actualCash"), null, "actualCash")
                : null;
        String notes = body.containsKey("notes") ? body.get("notes").toString() : null;
        CashRegisterEntity register = cashRegisterService.closeRegister(id, actualCash, notes);
        return ResponseEntity.ok(toDto(register));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Obtener la caja activa")
    public ResponseEntity<Map<String, Object>> getActive() {
        CashRegisterEntity register = cashRegisterService.getActive();
        if (register == null) {
            return ResponseEntity.ok(Map.of("active", false));
        }
        Map<String, Object> dto = new java.util.LinkedHashMap<>(toDto(register));
        dto.put("active", true);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Reporte Z de una caja")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(cashRegisterService.getReport(id));
    }

    private Map<String, Object> toDto(CashRegisterEntity r) {
        Map<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("id", r.getId());
        dto.put("status", r.getStatus());
        dto.put("userName", r.getUser().getName());
        dto.put("openedAt", r.getOpenedAt());
        dto.put("closedAt", r.getClosedAt());
        dto.put("openingAmount", r.getOpeningAmount());
        dto.put("closingAmount", r.getClosingAmount());
        dto.put("cashSalesTotal", r.getCashSalesTotal());
        dto.put("cardSalesTotal", r.getCardSalesTotal());
        dto.put("transferSalesTotal", r.getTransferSalesTotal());
        dto.put("totalSalesAmount", r.getTotalSalesAmount());
        dto.put("totalOrders", r.getTotalOrders());
        dto.put("expectedCash", r.getExpectedCash());
        dto.put("actualCash", r.getActualCash());
        dto.put("difference", r.getDifference());
        dto.put("notes", r.getNotes());
        return dto;
    }

    private BigDecimal parseAmount(Object value, BigDecimal fallback, String fieldName) {
        if (value == null) {
            return fallback;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Monto invalido en " + fieldName);
        }
    }
}
