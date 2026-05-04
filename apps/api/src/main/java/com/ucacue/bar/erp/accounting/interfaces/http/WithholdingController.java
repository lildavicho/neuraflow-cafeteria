package com.ucacue.bar.erp.accounting.interfaces.http;

import com.ucacue.bar.erp.accounting.application.WithholdingService;
import com.ucacue.bar.erp.accounting.application.WithholdingService.ConceptRequest;
import com.ucacue.bar.erp.accounting.application.WithholdingService.ProposeRequest;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingDocumentStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingSupplierRegime;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingTaxType;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingLineEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingMatrixEntity;
import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/erp/sri/withholdings")
@RequiredArgsConstructor
@Tag(name = "ERP SRI Withholdings", description = "Comprobantes de retencion SRI (codDoc 07)")
@RequireTenantModule(ModuleCode.SRI)
public class WithholdingController {

    private final WithholdingService withholdingService;

    @GetMapping("/matrix")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Listar matriz de conceptos de retencion vigentes")
    public ResponseEntity<List<MatrixView>> listMatrix() {
        return ResponseEntity.ok(withholdingService.listMatrix().stream()
                .map(MatrixView::from)
                .toList());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Listar retenciones por estado")
    public ResponseEntity<List<DocumentView>> listByStatus(@RequestParam WithholdingDocumentStatus status) {
        return ResponseEntity.ok(withholdingService.listByStatus(status).stream()
                .map(DocumentView::from)
                .toList());
    }

    @GetMapping("/by-purchase/{purchaseId}")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Obtener retenciones asociadas a una compra")
    public ResponseEntity<List<DocumentView>> listForPurchase(@PathVariable Long purchaseId) {
        return ResponseEntity.ok(withholdingService.listForPurchase(purchaseId).stream()
                .map(DocumentView::from)
                .toList());
    }

    @PostMapping("/purchases/{purchaseId}/propose")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Proponer retencion para una compra")
    public ResponseEntity<DocumentView> propose(@PathVariable Long purchaseId,
                                                @Valid @RequestBody ProposeBody body) {
        WithholdingDocumentEntity doc = withholdingService.proposeForPurchase(purchaseId,
                new ProposeRequest(body.supplierRegime(), body.issueDate(),
                        body.concepts().stream()
                                .map(c -> new ConceptRequest(
                                        c.conceptCode(), c.taxType(), c.baseOverride(),
                                        c.relatedDocumentCode(), c.relatedDocumentSerial()))
                                .toList()));
        return ResponseEntity.ok(DocumentView.from(doc));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirmar retencion propuesta")
    public ResponseEntity<DocumentView> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(DocumentView.from(withholdingService.confirm(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancelar retencion no autorizada")
    public ResponseEntity<DocumentView> cancel(@PathVariable Long id, @RequestBody CancelBody body) {
        return ResponseEntity.ok(DocumentView.from(withholdingService.cancel(id, body.reason())));
    }

    // ── DTOs ──────────────────────────────────────────────────

    public record ProposeBody(
            @NotNull WithholdingSupplierRegime supplierRegime,
            LocalDate issueDate,
            @NotEmpty List<ConceptBody> concepts) {
    }

    public record ConceptBody(
            @NotNull String conceptCode,
            @NotNull WithholdingTaxType taxType,
            BigDecimal baseOverride,
            String relatedDocumentCode,
            String relatedDocumentSerial) {
    }

    public record CancelBody(String reason) {
    }

    public record MatrixView(
            Long id, String conceptCode, String conceptName, String taxType,
            String supplierRegime, BigDecimal ratePercentage, BigDecimal minBaseAmount,
            String sriFormCode, Boolean active) {
        static MatrixView from(WithholdingMatrixEntity m) {
            return new MatrixView(m.getId(), m.getConceptCode(), m.getConceptName(),
                    m.getTaxType().name(), m.getSupplierRegime().name(),
                    m.getRatePercentage(), m.getMinBaseAmount(), m.getSriFormCode(), m.getActive());
        }
    }

    public record DocumentView(
            Long id, Long purchaseId, String status, String supplierIdentification, String supplierName,
            LocalDate issueDate, String sequentialNumber, String accessKey,
            BigDecimal ivaBase, BigDecimal ivaWithheld,
            BigDecimal incomeBase, BigDecimal incomeWithheld, BigDecimal totalWithheld,
            List<LineView> lines) {
        static DocumentView from(WithholdingDocumentEntity d) {
            return new DocumentView(d.getId(), d.getPurchaseId(), d.getStatus().name(),
                    d.getSupplierIdentification(), d.getSupplierName(),
                    d.getIssueDate(), d.getSequentialNumber(), d.getAccessKey(),
                    d.getIvaBase(), d.getIvaWithheld(), d.getIncomeBase(), d.getIncomeWithheld(),
                    d.getTotalWithheld(), d.getLines().stream().map(LineView::from).toList());
        }
    }

    public record LineView(
            String conceptCode, String conceptName, String taxType,
            BigDecimal baseAmount, BigDecimal ratePercentage, BigDecimal withheldAmount,
            String relatedDocumentCode, String relatedDocumentSerial) {
        static LineView from(WithholdingLineEntity l) {
            return new LineView(l.getConceptCode(), l.getConceptName(), l.getTaxType().name(),
                    l.getBaseAmount(), l.getRatePercentage(), l.getWithheldAmount(),
                    l.getRelatedDocumentCode(), l.getRelatedDocumentSerial());
        }
    }
}
