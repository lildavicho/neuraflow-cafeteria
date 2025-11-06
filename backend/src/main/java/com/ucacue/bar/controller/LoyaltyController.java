package com.ucacue.bar.controller;

import com.ucacue.bar.dto.loyalty.LoyaltyBalanceResponse;
import com.ucacue.bar.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loyalty")
@RequiredArgsConstructor
@Tag(name = "Loyalty", description = "Programa de puntos")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER')")
    @Operation(summary = "Saldo de puntos del usuario actual")
    public ResponseEntity<LoyaltyBalanceResponse> myBalance(Authentication authentication) {
        return ResponseEntity.ok(loyaltyService.getBalanceByEmail(authentication.getName()));
    }

    @GetMapping("/ledger")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Historial de puntos de un usuario")
    public ResponseEntity<?> ledger(@RequestParam Long userId) {
        return ResponseEntity.ok(loyaltyService.getLedger(userId));
    }
}
