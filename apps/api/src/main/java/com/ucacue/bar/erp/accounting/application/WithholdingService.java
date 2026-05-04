package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingDocumentStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingSupplierRegime;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingTaxType;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingLineEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingMatrixEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.WithholdingDocumentRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.WithholdingMatrixRepository;
import com.ucacue.bar.erp.purchases.infrastructure.persistence.entity.PurchaseEntity;
import com.ucacue.bar.erp.purchases.infrastructure.persistence.repository.PurchaseRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithholdingService {

    private final WithholdingMatrixRepository matrixRepository;
    private final WithholdingDocumentRepository documentRepository;
    private final PurchaseRepository purchaseRepository;
    private final TenantContextResolver tenantContextResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WithholdingMatrixEntity> listMatrix() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return matrixRepository.findByTenantIdAndActiveTrueOrderByConceptCodeAsc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<WithholdingDocumentEntity> listForPurchase(Long purchaseId) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return documentRepository.findByTenantIdAndPurchaseId(tenantId, purchaseId);
    }

    @Transactional(readOnly = true)
    public List<WithholdingDocumentEntity> listByStatus(WithholdingDocumentStatus status) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return documentRepository.findByTenantIdAndStatusOrderByIssueDateDesc(tenantId, status);
    }

    @Transactional
    public WithholdingDocumentEntity proposeForPurchase(Long purchaseId, ProposeRequest request) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        PurchaseEntity purchase = purchaseRepository.findById(purchaseId)
                .filter(p -> tenantId.equals(p.getTenantId()))
                .orElseThrow(() -> new NotFoundException("Compra no encontrada: " + purchaseId));

        documentRepository.findByTenantIdAndPurchaseId(tenantId, purchaseId).stream()
                .filter(existing -> existing.getStatus() != WithholdingDocumentStatus.CANCELLED
                        && existing.getStatus() != WithholdingDocumentStatus.REJECTED)
                .findAny()
                .ifPresent(existing -> {
                    throw new BadRequestException("Ya existe una retencion en estado " + existing.getStatus()
                            + " para la compra " + purchaseId);
                });

        WithholdingSupplierRegime regime = request.supplierRegime();
        if (regime == null) {
            throw new BadRequestException("Debe especificar el regimen del proveedor");
        }
        if (request.concepts() == null || request.concepts().isEmpty()) {
            throw new BadRequestException("Debe especificar al menos un concepto de retencion");
        }

        WithholdingDocumentEntity doc = new WithholdingDocumentEntity();
        doc.setTenantId(tenantId);
        doc.setPurchaseId(purchaseId);
        doc.setBranchId(purchase.getBranchId());
        doc.setSupplierIdentification(purchase.getSupplierIdentification());
        doc.setSupplierName(purchase.getSupplierName());
        doc.setIssueDate(request.issueDate() != null ? request.issueDate() : LocalDate.now());
        doc.setStatus(WithholdingDocumentStatus.PROPOSED);

        BigDecimal ivaBase = BigDecimal.ZERO;
        BigDecimal ivaWithheld = BigDecimal.ZERO;
        BigDecimal incomeBase = BigDecimal.ZERO;
        BigDecimal incomeWithheld = BigDecimal.ZERO;

        for (ConceptRequest concept : request.concepts()) {
            WithholdingMatrixEntity matrix = matrixRepository
                    .findByTenantIdAndConceptCodeAndSupplierRegimeAndTaxType(
                            tenantId, concept.conceptCode(), regime, concept.taxType())
                    .orElseThrow(() -> new BadRequestException(
                            "No existe matriz para concepto " + concept.conceptCode()
                                    + " regimen " + regime + " tipo " + concept.taxType()));

            BigDecimal base = concept.baseOverride() != null ? concept.baseOverride() : resolveBase(purchase, concept.taxType());
            if (base.compareTo(matrix.getMinBaseAmount()) < 0) {
                log.debug("Base {} below minimum {} for concept {} - skipping", base, matrix.getMinBaseAmount(), matrix.getConceptCode());
                continue;
            }

            BigDecimal withheld = base.multiply(matrix.getRatePercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            WithholdingLineEntity line = new WithholdingLineEntity();
            line.setTenantId(tenantId);
            line.setDocument(doc);
            line.setMatrixId(matrix.getId());
            line.setConceptCode(matrix.getConceptCode());
            line.setConceptName(matrix.getConceptName());
            line.setTaxType(matrix.getTaxType());
            line.setBaseAmount(base.setScale(2, RoundingMode.HALF_UP));
            line.setRatePercentage(matrix.getRatePercentage());
            line.setWithheldAmount(withheld);
            line.setRelatedDocumentCode(concept.relatedDocumentCode());
            line.setRelatedDocumentSerial(concept.relatedDocumentSerial());
            doc.getLines().add(line);

            if (matrix.getTaxType() == WithholdingTaxType.VAT) {
                ivaBase = ivaBase.add(base);
                ivaWithheld = ivaWithheld.add(withheld);
            } else {
                incomeBase = incomeBase.add(base);
                incomeWithheld = incomeWithheld.add(withheld);
            }
        }

        if (doc.getLines().isEmpty()) {
            throw new BadRequestException("Ningun concepto cumple el umbral minimo - retencion no aplicable");
        }

        doc.setIvaBase(ivaBase.setScale(2, RoundingMode.HALF_UP));
        doc.setIvaWithheld(ivaWithheld.setScale(2, RoundingMode.HALF_UP));
        doc.setIncomeBase(incomeBase.setScale(2, RoundingMode.HALF_UP));
        doc.setIncomeWithheld(incomeWithheld.setScale(2, RoundingMode.HALF_UP));
        doc.setTotalWithheld(ivaWithheld.add(incomeWithheld).setScale(2, RoundingMode.HALF_UP));

        WithholdingDocumentEntity saved = documentRepository.save(doc);
        auditService.logAction(null, "PROPOSE", "WITHHOLDING", saved.getId(),
                "Retencion propuesta para compra " + purchaseId + " total " + saved.getTotalWithheld());
        return saved;
    }

    @Transactional
    public WithholdingDocumentEntity confirm(Long documentId) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        WithholdingDocumentEntity doc = documentRepository.findByTenantIdAndId(tenantId, documentId)
                .orElseThrow(() -> new NotFoundException("Retencion no encontrada: " + documentId));
        if (doc.getStatus() != WithholdingDocumentStatus.PROPOSED) {
            throw new BadRequestException("Solo se pueden confirmar retenciones en estado PROPOSED. Actual: " + doc.getStatus());
        }
        doc.setStatus(WithholdingDocumentStatus.CONFIRMED);
        WithholdingDocumentEntity saved = documentRepository.save(doc);
        auditService.logAction(null, "CONFIRM", "WITHHOLDING", saved.getId(), "Retencion confirmada");
        return saved;
    }

    @Transactional
    public WithholdingDocumentEntity cancel(Long documentId, String reason) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        WithholdingDocumentEntity doc = documentRepository.findByTenantIdAndId(tenantId, documentId)
                .orElseThrow(() -> new NotFoundException("Retencion no encontrada: " + documentId));
        if (doc.getStatus() == WithholdingDocumentStatus.AUTHORIZED) {
            throw new BadRequestException("No se puede cancelar una retencion ya autorizada por el SRI");
        }
        doc.setStatus(WithholdingDocumentStatus.CANCELLED);
        doc.setCancelledAt(LocalDateTime.now());
        doc.setCancellationReason(reason);
        WithholdingDocumentEntity saved = documentRepository.save(doc);
        auditService.logAction(null, "CANCEL", "WITHHOLDING", saved.getId(),
                "Retencion cancelada: " + Optional.ofNullable(reason).orElse("(sin motivo)"));
        return saved;
    }

    private BigDecimal resolveBase(PurchaseEntity purchase, WithholdingTaxType taxType) {
        return switch (taxType) {
            case INCOME -> purchase.getSubtotal() != null ? purchase.getSubtotal() : BigDecimal.ZERO;
            case VAT -> purchase.getTax() != null ? purchase.getTax() : BigDecimal.ZERO;
        };
    }

    public record ProposeRequest(
            WithholdingSupplierRegime supplierRegime,
            LocalDate issueDate,
            Collection<ConceptRequest> concepts) {
    }

    public record ConceptRequest(
            String conceptCode,
            WithholdingTaxType taxType,
            BigDecimal baseOverride,
            String relatedDocumentCode,
            String relatedDocumentSerial) {
    }
}
