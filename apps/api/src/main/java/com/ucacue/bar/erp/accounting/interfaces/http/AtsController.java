package com.ucacue.bar.erp.accounting.interfaces.http;

import com.ucacue.bar.erp.accounting.application.AtsGenerationService;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AtsPeriodEntity;
import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/erp/sri/ats")
@RequiredArgsConstructor
@Tag(name = "ERP SRI ATS", description = "Anexo Transaccional Simplificado (ATS)")
@RequireTenantModule(ModuleCode.SRI)
public class AtsController {

    private final AtsGenerationService atsGenerationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Listar periodos ATS del tenant")
    public ResponseEntity<List<PeriodView>> listPeriods() {
        return ResponseEntity.ok(atsGenerationService.listPeriods().stream()
                .map(PeriodView::from)
                .toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Obtener periodo ATS por id")
    public ResponseEntity<PeriodView> getPeriod(@PathVariable Long id) {
        return ResponseEntity.ok(PeriodView.from(atsGenerationService.getPeriod(id)));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generar o regenerar ATS para un periodo (anio/mes)")
    public ResponseEntity<PeriodView> generate(@Valid @RequestBody GenerateBody body) {
        AtsPeriodEntity entity = atsGenerationService.generate(body.fiscalYear(), body.fiscalMonth());
        return ResponseEntity.ok(PeriodView.from(entity));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marcar periodo ATS como enviado al SRI")
    public ResponseEntity<PeriodView> markSubmitted(@PathVariable Long id) {
        return ResponseEntity.ok(PeriodView.from(atsGenerationService.markSubmitted(id)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cerrar un periodo ATS ya enviado")
    public ResponseEntity<PeriodView> close(@PathVariable Long id) {
        return ResponseEntity.ok(PeriodView.from(atsGenerationService.close(id)));
    }

    @GetMapping("/{id}/xml")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Descargar XML ATS del periodo")
    public ResponseEntity<byte[]> downloadXml(@PathVariable Long id) {
        AtsPeriodEntity period = atsGenerationService.getPeriod(id);
        String xml = period.getXmlPayload() != null ? period.getXmlPayload() : "";
        String fileName = String.format("ats_%d_%02d.xml", period.getFiscalYear(), period.getFiscalMonth());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.getBytes(StandardCharsets.UTF_8));
    }

    public record GenerateBody(
            @NotNull @Min(2000) @Max(2100) Integer fiscalYear,
            @NotNull @Min(1) @Max(12) Integer fiscalMonth) {
    }

    public record PeriodView(
            Long id,
            Integer fiscalYear,
            Integer fiscalMonth,
            String status,
            Integer salesCount,
            Integer purchasesCount,
            Integer withholdingsCount,
            BigDecimal totalSales,
            BigDecimal totalPurchases,
            BigDecimal totalWithheld,
            LocalDateTime generatedAt,
            LocalDateTime submittedAt,
            Boolean hasXml) {
        static PeriodView from(AtsPeriodEntity e) {
            return new PeriodView(
                    e.getId(),
                    e.getFiscalYear(),
                    e.getFiscalMonth(),
                    e.getStatus() != null ? e.getStatus().name() : null,
                    e.getSalesCount(),
                    e.getPurchasesCount(),
                    e.getWithholdingsCount(),
                    e.getTotalSales(),
                    e.getTotalPurchases(),
                    e.getTotalWithheld(),
                    e.getGeneratedAt(),
                    e.getSubmittedAt(),
                    e.getXmlPayload() != null && !e.getXmlPayload().isEmpty());
        }
    }
}
