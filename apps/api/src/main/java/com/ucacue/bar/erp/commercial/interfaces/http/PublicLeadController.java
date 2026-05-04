package com.ucacue.bar.erp.commercial.interfaces.http;

import com.ucacue.bar.erp.commercial.application.PublicLeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/leads")
@RequiredArgsConstructor
@Tag(name = "Public Leads", description = "Solicitudes comerciales publicas")
public class PublicLeadController {

    private final PublicLeadService publicLeadService;

    @PostMapping
    @Operation(summary = "Registrar solicitud publica de demo/contacto")
    public ResponseEntity<PublicLeadResponse> create(@Valid @RequestBody PublicLeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(publicLeadService.create(request));
    }
}
