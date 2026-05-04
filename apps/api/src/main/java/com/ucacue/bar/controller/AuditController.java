package com.ucacue.bar.controller;

import com.ucacue.bar.entity.AuditLogEntity;
import com.ucacue.bar.entity.LoginAuditEntity;
import com.ucacue.bar.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Auditoría de sistema y acceso")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    @Operation(summary = "Listar logs de auditoría del sistema")
    public ResponseEntity<Page<AuditLogEntity>> auditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.listAuditLogs(userId, action, entityType, from, to, page, size));
    }

    @GetMapping("/logins")
    @Operation(summary = "Listar auditoría de logins")
    public ResponseEntity<Page<LoginAuditEntity>> loginAudit(
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.listLoginAudit(email, page, size));
    }
}
