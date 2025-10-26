package com.ucacue.bar.controller;

import com.ucacue.bar.dto.SaleDTO;
import com.ucacue.bar.dto.CreateSaleRequest;
import com.ucacue.bar.service.SaleService;
import com.ucacue.bar.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
@Tag(name = "Sales", description = "Sales management endpoints")
public class SaleController {
    
    private final SaleService saleService;
    private final IdempotencyService idempotencyService;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPRADOR')")
    @Operation(summary = "Create a new sale")
    public ResponseEntity<SaleDTO> createSale(
            @Valid @RequestBody CreateSaleRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        
        // Check idempotency
        if (idempotencyKey != null) {
            ResponseEntity<SaleDTO> cached = idempotencyService.getCachedResponse(idempotencyKey);
            if (cached != null) {
                return cached;
            }
        }
        
        SaleDTO sale = saleService.createSale(request, authentication.getName());
        ResponseEntity<SaleDTO> response = ResponseEntity.status(HttpStatus.CREATED).body(sale);
        
        // Cache response
        if (idempotencyKey != null) {
            idempotencyService.cacheResponse(idempotencyKey, response);
        }
        
        return response;
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPRADOR')")
    @Operation(summary = "Get sale by ID")
    public ResponseEntity<SaleDTO> getSaleById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(saleService.getSaleById(id, authentication));
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all sales with filters")
    public ResponseEntity<Page<SaleDTO>> getSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            Pageable pageable) {
        
        LocalDateTime fromDate = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDate = to != null ? to.atTime(23, 59, 59) : null;
        
        return ResponseEntity.ok(saleService.getSales(fromDate, toDate, status, paymentMethod, pageable));
    }
    
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPRADOR')")
    @Operation(summary = "Get current user's sales")
    public ResponseEntity<Page<SaleDTO>> getMySales(
            Authentication authentication,
            Pageable pageable) {
        return ResponseEntity.ok(saleService.getUserSales(authentication.getName(), pageable));
    }
    
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get sales summary")
    public ResponseEntity<Map<String, Object>> getSalesSummary(
            @RequestParam(defaultValue = "daily") String period) {
        return ResponseEntity.ok(saleService.getSalesSummary(period));
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update sale status")
    public ResponseEntity<SaleDTO> updateSaleStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(saleService.updateSaleStatus(id, status));
    }
    
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel a sale")
    public ResponseEntity<Void> cancelSale(@PathVariable Long id) {
        saleService.cancelSale(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/invoice/{invoiceNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPRADOR')")
    @Operation(summary = "Get sale by invoice number")
    public ResponseEntity<SaleDTO> getSaleByInvoice(
            @PathVariable String invoiceNumber,
            Authentication authentication) {
        return ResponseEntity.ok(saleService.getSaleByInvoiceNumber(invoiceNumber, authentication));
    }
}
