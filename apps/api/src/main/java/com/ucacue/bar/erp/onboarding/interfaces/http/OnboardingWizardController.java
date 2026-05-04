package com.ucacue.bar.erp.onboarding.interfaces.http;

import com.ucacue.bar.erp.onboarding.application.OnboardingWizardService;
import com.ucacue.bar.erp.onboarding.application.OnboardingWizardService.OnboardingCatalogView;
import com.ucacue.bar.erp.onboarding.application.OnboardingWizardService.OnboardingSessionView;
import com.ucacue.bar.erp.onboarding.application.OnboardingWizardService.RucValidationView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;

import java.util.Map;

@RestController
@RequestMapping("/erp/onboarding")
@RequiredArgsConstructor
@Tag(name = "ERP Onboarding", description = "Wizard multi-tenant para parametrizar Ecuador, SRI, contabilidad y operaciones")
public class OnboardingWizardController {

    private final OnboardingWizardService onboardingWizardService;

    @GetMapping("/session")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener o crear la sesion de onboarding del tenant actual")
    public ResponseEntity<OnboardingSessionView> session(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode) {
        return ResponseEntity.ok(onboardingWizardService.getOrCreate(tenantCode));
    }

    @PatchMapping("/steps/{step}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Guardar un paso del wizard con autosave")
    public ResponseEntity<OnboardingSessionView> saveStep(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable String step,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(onboardingWizardService.updateStep(tenantCode, step, payload));
    }

    @PostMapping("/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publicar el tenant cuando el checklist critico esta completo")
    public ResponseEntity<OnboardingSessionView> publish(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode) {
        return ResponseEntity.ok(onboardingWizardService.publish(tenantCode));
    }

    @GetMapping("/catalog")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Catalogos base del wizard: documentos SRI, formas de pago, verticales y rasgos fiscales")
    public ResponseEntity<OnboardingCatalogView> catalog() {
        return ResponseEntity.ok(onboardingWizardService.catalog());
    }

    @GetMapping("/ruc/validate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Validacion local basica de formato RUC")
    public ResponseEntity<RucValidationView> validateRuc(@RequestParam String ruc) {
        return ResponseEntity.ok(onboardingWizardService.validateRuc(ruc));
    }

    @PostMapping(value = "/sri-certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Subir firma electronica .p12 y guardarla cifrada para el tenant")
    public ResponseEntity<OnboardingSessionView> uploadSriCertificate(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestPart("file") MultipartFile file,
            @RequestPart("password") String password,
            @RequestPart(value = "alias", required = false) String alias,
            @RequestPart(value = "environment", required = false) String environment) {
        return ResponseEntity.ok(onboardingWizardService.uploadSriCertificate(
                tenantCode, file, password, alias, environment));
    }

    @PostMapping(value = "/ride-reference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Subir modelo o referencia visual del comprobante del negocio")
    public ResponseEntity<OnboardingSessionView> uploadRideReference(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(onboardingWizardService.uploadRideReference(tenantCode, file));
    }
}
