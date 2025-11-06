package com.ucacue.bar.controller;

import com.ucacue.bar.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "KPIs en tiempo real")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/snapshot")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER')")
    @Operation(summary = "Obtiene el estado inicial del dashboard")
    public ResponseEntity<?> snapshot() {
        return ResponseEntity.ok(dashboardService.snapshot());
    }
}
