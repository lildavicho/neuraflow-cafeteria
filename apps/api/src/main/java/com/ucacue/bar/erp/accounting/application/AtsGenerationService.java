package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AtsPeriodStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AtsPeriodEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.SriConfigurationEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AtsPeriodRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.SriConfigurationRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.WithholdingDocumentRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtsGenerationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AtsPeriodRepository atsPeriodRepository;
    private final TaxDocumentRepository taxDocumentRepository;
    private final PurchaseRepository purchaseRepository;
    private final WithholdingDocumentRepository withholdingDocumentRepository;
    private final SriConfigurationRepository sriConfigurationRepository;
    private final TenantContextResolver tenantContextResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AtsPeriodEntity> listPeriods() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return atsPeriodRepository.findByTenantIdOrderByFiscalYearDescFiscalMonthDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public AtsPeriodEntity getPeriod(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return atsPeriodRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException("Periodo ATS no encontrado: " + id));
    }

    @Transactional
    public AtsPeriodEntity generate(int fiscalYear, int fiscalMonth) {
        if (fiscalMonth < 1 || fiscalMonth > 12) {
            throw new BadRequestException("Mes fiscal invalido: " + fiscalMonth);
        }
        if (fiscalYear < 2000 || fiscalYear > 2100) {
            throw new BadRequestException("Anio fiscal invalido: " + fiscalYear);
        }

        TenantEntity tenant = tenantContextResolver.resolveCurrent();
        Long tenantId = tenant.getId();

        YearMonth period = YearMonth.of(fiscalYear, fiscalMonth);
        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();
        LocalDateTime periodStartTs = periodStart.atStartOfDay();
        LocalDateTime periodEndTs = periodEnd.atTime(23, 59, 59);

        AtsPeriodEntity entity = atsPeriodRepository
                .findForUpdate(tenantId, fiscalYear, fiscalMonth)
                .orElseGet(() -> {
                    AtsPeriodEntity created = new AtsPeriodEntity();
                    created.setTenantId(tenantId);
                    created.setFiscalYear(fiscalYear);
                    created.setFiscalMonth(fiscalMonth);
                    created.setStatus(AtsPeriodStatus.DRAFT);
                    return created;
                });

        if (entity.getStatus() == AtsPeriodStatus.SUBMITTED || entity.getStatus() == AtsPeriodStatus.CLOSED) {
            throw new BadRequestException("No se puede regenerar un periodo en estado " + entity.getStatus());
        }

        List<TaxDocumentEntity> sales = taxDocumentRepository
                .findByTenantIdAndStatusAndIssueDateBetweenOrderByIssueDateAsc(
                        tenantId, TaxDocumentStatus.AUTHORIZED, periodStart, periodEnd);
        List<PurchaseEntity> purchases = purchaseRepository
                .findReceivedBetweenByTenantId(tenantId, periodStartTs, periodEndTs);
        List<WithholdingDocumentEntity> withholdings = withholdingDocumentRepository
                .findByTenantIdAndIssueDateBetweenOrderByIssueDateAsc(tenantId, periodStart, periodEnd);

        BigDecimal totalSales = sales.stream()
                .map(TaxDocumentEntity::getTotalAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPurchases = purchases.stream()
                .map(PurchaseEntity::getTotal)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWithheld = withholdings.stream()
                .map(WithholdingDocumentEntity::getTotalWithheld)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SriConfigurationEntity sriConfig = sriConfigurationRepository.findByTenantId(tenantId).orElse(null);
        String xml = buildXml(tenant, sriConfig, fiscalYear, fiscalMonth, sales, purchases, withholdings);

        entity.setSalesCount(sales.size());
        entity.setPurchasesCount(purchases.size());
        entity.setWithholdingsCount(withholdings.size());
        entity.setTotalSales(totalSales);
        entity.setTotalPurchases(totalPurchases);
        entity.setTotalWithheld(totalWithheld);
        entity.setXmlPayload(xml);
        entity.setGeneratedAt(LocalDateTime.now());
        entity.setStatus(AtsPeriodStatus.GENERATED);

        AtsPeriodEntity saved = atsPeriodRepository.save(entity);
        auditService.logAction(null, "GENERATE", "ATS_PERIOD", saved.getId(),
                "ATS generado " + fiscalYear + "-" + String.format("%02d", fiscalMonth)
                        + " ventas=" + sales.size() + " compras=" + purchases.size()
                        + " retenciones=" + withholdings.size());
        return saved;
    }

    @Transactional
    public AtsPeriodEntity markSubmitted(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        AtsPeriodEntity entity = atsPeriodRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException("Periodo ATS no encontrado: " + id));
        if (entity.getStatus() != AtsPeriodStatus.GENERATED) {
            throw new BadRequestException("Solo se puede marcar como enviado un periodo GENERATED. Actual: " + entity.getStatus());
        }
        entity.setStatus(AtsPeriodStatus.SUBMITTED);
        entity.setSubmittedAt(LocalDateTime.now());
        AtsPeriodEntity saved = atsPeriodRepository.save(entity);
        auditService.logAction(null, "SUBMIT", "ATS_PERIOD", saved.getId(),
                "ATS marcado como enviado al SRI");
        return saved;
    }

    @Transactional
    public AtsPeriodEntity close(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        AtsPeriodEntity entity = atsPeriodRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException("Periodo ATS no encontrado: " + id));
        if (entity.getStatus() != AtsPeriodStatus.SUBMITTED) {
            throw new BadRequestException("Solo se puede cerrar un periodo SUBMITTED. Actual: " + entity.getStatus());
        }
        entity.setStatus(AtsPeriodStatus.CLOSED);
        AtsPeriodEntity saved = atsPeriodRepository.save(entity);
        auditService.logAction(null, "CLOSE", "ATS_PERIOD", saved.getId(), "ATS cerrado");
        return saved;
    }

    private String buildXml(TenantEntity tenant,
                            SriConfigurationEntity sriConfig,
                            int fiscalYear,
                            int fiscalMonth,
                            List<TaxDocumentEntity> sales,
                            List<PurchaseEntity> purchases,
                            List<WithholdingDocumentEntity> withholdings) {
        String issuerRuc = sriConfig != null ? sriConfig.getIssuerRuc() : "";
        String issuerName = sriConfig != null && sriConfig.getIssuerLegalName() != null
                ? sriConfig.getIssuerLegalName()
                : tenant.getLegalName();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<iva>\n");
        xml.append("  <TipoIDInformante>R</TipoIDInformante>\n");
        xml.append("  <IdInformante>").append(escape(nullSafe(issuerRuc))).append("</IdInformante>\n");
        xml.append("  <razonSocial>").append(escape(nullSafe(issuerName))).append("</razonSocial>\n");
        xml.append("  <Anio>").append(fiscalYear).append("</Anio>\n");
        xml.append("  <Mes>").append(String.format("%02d", fiscalMonth)).append("</Mes>\n");

        if (!purchases.isEmpty()) {
            xml.append("  <compras>\n");
            for (PurchaseEntity purchase : purchases) {
                xml.append("    <detalleCompras>\n");
                xml.append("      <codSustento>01</codSustento>\n");
                xml.append("      <tpIdProv>").append(resolveIdType(purchase.getSupplierIdentification())).append("</tpIdProv>\n");
                xml.append("      <idProv>").append(escape(nullSafe(purchase.getSupplierIdentification()))).append("</idProv>\n");
                xml.append("      <tipoComprobante>01</tipoComprobante>\n");
                xml.append("      <fechaRegistro>").append(formatDate(purchase.getReceivedAt())).append("</fechaRegistro>\n");
                xml.append("      <secuencial>").append(escape(nullSafe(purchase.getExternalDocumentNumber()))).append("</secuencial>\n");
                xml.append("      <baseImponible>").append(money(purchase.getSubtotal())).append("</baseImponible>\n");
                xml.append("      <montoIva>").append(money(purchase.getTax())).append("</montoIva>\n");
                xml.append("      <total>").append(money(purchase.getTotal())).append("</total>\n");
                xml.append("    </detalleCompras>\n");
            }
            xml.append("  </compras>\n");
        }

        if (!sales.isEmpty()) {
            xml.append("  <ventas>\n");
            for (TaxDocumentEntity doc : sales) {
                xml.append("    <detalleVentas>\n");
                xml.append("      <tpIdCliente>").append(resolveIdType(doc.getBuyerIdentification())).append("</tpIdCliente>\n");
                xml.append("      <idCliente>").append(escape(nullSafe(doc.getBuyerIdentification()))).append("</idCliente>\n");
                xml.append("      <tipoComprobante>01</tipoComprobante>\n");
                xml.append("      <numeroComprobantes>1</numeroComprobantes>\n");
                xml.append("      <baseImponible>").append(money(doc.getSubtotalAmount())).append("</baseImponible>\n");
                xml.append("      <montoIva>").append(money(doc.getTaxAmount())).append("</montoIva>\n");
                xml.append("      <valorTotal>").append(money(doc.getTotalAmount())).append("</valorTotal>\n");
                xml.append("    </detalleVentas>\n");
            }
            xml.append("  </ventas>\n");
        }

        if (!withholdings.isEmpty()) {
            xml.append("  <retenciones>\n");
            for (WithholdingDocumentEntity wd : withholdings) {
                xml.append("    <detalleRetenciones>\n");
                xml.append("      <tpIdProv>").append(resolveIdType(wd.getSupplierIdentification())).append("</tpIdProv>\n");
                xml.append("      <idProv>").append(escape(nullSafe(wd.getSupplierIdentification()))).append("</idProv>\n");
                xml.append("      <fechaEmisionRet>").append(formatDate(wd.getIssueDate())).append("</fechaEmisionRet>\n");
                xml.append("      <secuencialRet>").append(escape(nullSafe(wd.getSequentialNumber()))).append("</secuencialRet>\n");
                xml.append("      <totalRetenido>").append(money(wd.getTotalWithheld())).append("</totalRetenido>\n");
                xml.append("    </detalleRetenciones>\n");
            }
            xml.append("  </retenciones>\n");
        }

        xml.append("</iva>\n");
        return xml.toString();
    }

    private static String resolveIdType(String id) {
        if (id == null) {
            return "07";
        }
        int len = id.replaceAll("\\s+", "").length();
        return switch (len) {
            case 13 -> "01";
            case 10 -> "02";
            case 0 -> "07";
            default -> "03";
        };
    }

    private static String money(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMAT.format(date);
    }

    private static String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_FORMAT.format(dateTime.toLocalDate());
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
