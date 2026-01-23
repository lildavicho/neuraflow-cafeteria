package com.ucacue.bar.controller;

import com.ucacue.bar.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Reportes e históricos")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER','BUYER','CUSTOMER')")
    @Operation(summary = "Serie temporal de ventas")
    public ResponseEntity<?> sales(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.sales(from, to));
    }

    @GetMapping("/people")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER','BUYER','CUSTOMER')")
    @Operation(summary = "Serie temporal de afluencia")
    public ResponseEntity<?> people(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.people(from, to));
    }

    @GetMapping("/sales-vs-people")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER','BUYER','CUSTOMER')")
    @Operation(summary = "Relación ventas vs afluencia")
    public ResponseEntity<?> salesVsPeople(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.salesVsPeople(from, to));
    }

    @GetMapping(value = "/export")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER','BUYER','CUSTOMER')")
    @Operation(summary = "Exportar reporte en Excel o PDF")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "excel") String format,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        byte[] data = analyticsService.export(format, from, to);
        MediaType mediaType = "pdf".equalsIgnoreCase(format) ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;
        String extension = "pdf".equalsIgnoreCase(format) ? "pdf" : "xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics." + extension)
            .contentType(mediaType)
            .body(data);
    }
}


