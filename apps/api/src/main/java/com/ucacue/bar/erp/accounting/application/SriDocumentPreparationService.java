package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.OrderItemEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.SriConfigurationEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentTaxLineEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxRuleEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxRuleRepository;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriKeyService;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriTextSanitizer;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriXmlMarshaller;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriXmlValidationService;
import com.ucacue.bar.erp.accounting.infrastructure.sri.model.SriFacturaXml;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SriDocumentPreparationService {

    private static final String DEFAULT_CONSUMER_ID = "9999999999999";
    private static final String DEFAULT_CONSUMER_NAME = "Consumidor final";
    private static final DateTimeFormatter ACCESS_KEY_DATE = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter XML_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrderRepository orderRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final TaxDocumentRepository taxDocumentRepository;
    private final TaxComputationService taxComputationService;
    private final SriKeyService sriKeyService;
    private final SriTextSanitizer sriTextSanitizer;
    private final SriXmlMarshaller sriXmlMarshaller;
    private final SriXmlValidationService sriXmlValidationService;

    public PreparedDocument prepare(TaxDocumentEntity document,
                                    SriConfigurationEntity configuration,
                                    List<TaxDocumentTaxLineEntity> persistedTaxLines) {
        List<String> errors = new ArrayList<>();
        String sriDocumentCode = resolveSriDocumentCode(document);
        if (sriDocumentCode == null) {
            errors.add("El tipo documental no tiene un codigo SRI soportado");
        }
        if (Boolean.TRUE.equals(document.getDocumentType().getRequiresSequence())) {
            if (isBlank(document.getEstablishmentCode()) || isBlank(document.getEmissionPointCode()) || isBlank(document.getSequentialNumber())) {
                errors.add("Faltan secuenciales tributarios");
            }
        }
        if (document.getIssueDate() == null) {
            errors.add("La fecha de emision es obligatoria");
        }
        if (digitsOnly(configuration.getIssuerRuc()).length() != 13) {
            errors.add("La configuracion SRI requiere un RUC emisor valido");
        }
        if (amount(document.getTotalAmount()).compareTo(amount(document.getSubtotalAmount()).add(amount(document.getTaxAmount()))) != 0) {
            errors.add("Total inconsistente con subtotal e impuestos");
        }

        List<SourceLine> sourceLines = resolveSourceLines(document, errors);
        BigDecimal computedTaxes = persistedTaxLines.stream()
                .map(TaxDocumentTaxLineEntity::getTaxAmount)
                .map(this::amount)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        if (!persistedTaxLines.isEmpty() && computedTaxes.compareTo(amount(document.getTaxAmount())) != 0) {
            errors.add("La suma de lineas tributarias no coincide con el impuesto total");
        }

        if (!errors.isEmpty()) {
            return new PreparedDocument(null, null, errors, sourceLines);
        }

        String accessKey = buildAccessKey(document, configuration, sriDocumentCode);
        String xml = buildXmlPayload(document, configuration, sourceLines, persistedTaxLines, sriDocumentCode);
        return new PreparedDocument(accessKey, xml, List.of(), sourceLines);
    }

    private List<SourceLine> resolveSourceLines(TaxDocumentEntity document, List<String> errors) {
        if (!document.getSourceType().startsWith("ORDER_")) {
            errors.add("Solo se soportan documentos tributarios originados desde ventas POS");
            return List.of();
        }

        OrderEntity order = orderRepository.findByTenantIdAndId(document.getTenantId(), document.getSourceId())
                .orElseThrow(() -> new NotFoundException("Orden origen no encontrada para el documento tributario"));
        if (order.getItems() == null || order.getItems().isEmpty()) {
            errors.add("La orden origen no tiene items para construir el XML tributario");
            return List.of();
        }

        TaxComputationService.TaxSummary summary = taxComputationService.calculate(
                document.getTenantId(),
                order.getItems().stream()
                        .map(item -> new TaxComputationService.TaxableLine(item.getProduct().getId(), item.getQuantity(), item.getUnitPrice()))
                        .toList());

        Map<Long, TaxComputationService.TaxLineResult> linesByProduct = new LinkedHashMap<>();
        for (TaxComputationService.TaxLineResult line : summary.lines()) {
            linesByProduct.put(line.productId(), line);
        }

        List<SourceLine> result = new ArrayList<>();
        for (OrderItemEntity item : order.getItems()) {
            TaxComputationService.TaxLineResult line = linesByProduct.get(item.getProduct().getId());
            BigDecimal subtotal = line != null ? amount(line.subtotalAmount()) : amount(item.getLineTotal());
            BigDecimal tax = line != null ? amount(line.taxAmount()) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            result.add(new SourceLine(
                    item.getProduct().getCode() != null ? item.getProduct().getCode() : String.valueOf(item.getProduct().getId()),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    amount(item.getUnitPrice()),
                    subtotal,
                    tax,
                    line != null ? line.taxRuleId() : null,
                    resolveTaxPercentage(subtotal, tax)));
        }

        if ("ORDER_REFUND".equalsIgnoreCase(document.getSourceType())) {
            TaxDocumentEntity baseInvoice = taxDocumentRepository.findByTenantIdAndSourceTypeAndSourceId(
                            document.getTenantId(), "ORDER_SALE", document.getSourceId())
                    .orElse(null);
            if (baseInvoice == null || isBlank(baseInvoice.getSequentialNumber())) {
                errors.add("La nota de credito requiere una factura electronica base previamente generada");
            }
        }
        return result;
    }

    private String buildXmlPayload(TaxDocumentEntity document,
                                   SriConfigurationEntity configuration,
                                   List<SourceLine> sourceLines,
                                   List<TaxDocumentTaxLineEntity> persistedTaxLines,
                                   String sriDocumentCode) {
        return switch (sriDocumentCode) {
            case "01" -> buildInvoiceXml(document, configuration, sourceLines, persistedTaxLines);
            case "04" -> buildCreditNoteXml(document, configuration, sourceLines, persistedTaxLines);
            default -> throw new BadRequestException("No existe generador XML para el documento SRI solicitado");
        };
    }

    private String buildInvoiceXml(TaxDocumentEntity document,
                                   SriConfigurationEntity configuration,
                                   List<SourceLine> sourceLines,
                                   List<TaxDocumentTaxLineEntity> persistedTaxLines) {
        SriFacturaXml factura = new SriFacturaXml();
        factura.setInfoTributaria(new SriFacturaXml.InfoTributaria(
                normalizeEnvironmentCode(configuration.getEnvironmentCode()),
                normalizeEmissionCode(configuration.getEmissionCode()),
                cleanAndLimit(configuration.getIssuerLegalName(), 300),
                cleanOptionalAndLimit(configuration.getIssuerTradeName(), 300),
                digitsOnly(configuration.getIssuerRuc()),
                buildAccessKey(document, configuration, "01"),
                "01",
                leftPad(document.getEstablishmentCode(), 3),
                leftPad(document.getEmissionPointCode(), 3),
                leftPad(document.getSequentialNumber(), 9),
                cleanAndLimit(configuration.getMatrixAddress(), 300),
                null,
                resolveRimpeText(configuration)
        ));

        SriFacturaXml.TotalConImpuestos totalConImpuestos = new SriFacturaXml.TotalConImpuestos();
        totalConImpuestos.getTotalImpuesto().addAll(buildInvoiceTotalTaxes(
                document.getTenantId(),
                sourceLines,
                persistedTaxLines
        ));

        SriFacturaXml.Pagos pagos = new SriFacturaXml.Pagos();
        pagos.getPago().add(new SriFacturaXml.Pago(
                resolvePaymentCode(document),
                formatDecimal(document.getTotalAmount()),
                null,
                null
        ));

        factura.setInfoFactura(new SriFacturaXml.InfoFactura(
                document.getIssueDate().format(XML_DATE),
                cleanAndLimit(configuration.getEstablishmentAddress(), 300),
                cleanOptionalAndLimit(configuration.getSpecialTaxpayer(), 13),
                normalizeObligatedAccounting(configuration.getObligatedAccounting()),
                resolveBuyerIdentificationType(document.getBuyerIdentification()),
                cleanAndLimit(resolveBuyerName(document), 300),
                resolveBuyerIdentification(document),
                formatDecimal(document.getSubtotalAmount()),
                "0.00",
                totalConImpuestos,
                "0.00",
                formatDecimal(document.getTotalAmount()),
                "DOLAR",
                pagos
        ));

        SriFacturaXml.Detalles detalles = new SriFacturaXml.Detalles();
        detalles.getDetalle().addAll(buildInvoiceDetails(document.getTenantId(), sourceLines));
        factura.setDetalles(detalles);

        SriFacturaXml.InfoAdicional infoAdicional = new SriFacturaXml.InfoAdicional();
        infoAdicional.getCampoAdicional().add(new SriFacturaXml.CampoAdicional(
                "Origen",
                cleanAndLimit("Venta POS " + document.getSourceId(), 300)
        ));
        String buyerEmail = resolveBuyerEmail(document);
        if (buyerEmail != null && !buyerEmail.isBlank()) {
            infoAdicional.getCampoAdicional().add(new SriFacturaXml.CampoAdicional(
                    "Email",
                    cleanAndLimit(buyerEmail, 300)
            ));
        }
        String buyerAddress = resolveBuyerAddress(document);
        if (buyerAddress != null && !buyerAddress.isBlank()) {
            infoAdicional.getCampoAdicional().add(new SriFacturaXml.CampoAdicional(
                    "Direccion",
                    cleanAndLimit(buyerAddress, 300)
            ));
        }
        String buyerPhone = resolveBuyerPhone(document);
        if (buyerPhone != null && !buyerPhone.isBlank()) {
            infoAdicional.getCampoAdicional().add(new SriFacturaXml.CampoAdicional(
                    "Telefono",
                    cleanAndLimit(buyerPhone, 300)
            ));
        }
        factura.setInfoAdicional(infoAdicional);

        String xml = sriXmlMarshaller.marshalFactura(factura);
        sriXmlValidationService.validateFacturaV110(xml);
        return xml;
    }

    private String buildCreditNoteXml(TaxDocumentEntity document,
                                      SriConfigurationEntity configuration,
                                      List<SourceLine> sourceLines,
                                      List<TaxDocumentTaxLineEntity> persistedTaxLines) {
        TaxDocumentEntity baseInvoice = taxDocumentRepository.findByTenantIdAndSourceTypeAndSourceId(
                        document.getTenantId(), "ORDER_SALE", document.getSourceId())
                .orElseThrow(() -> new BadRequestException("No existe la factura base para emitir la nota de credito"));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<notaCredito id=\"comprobante\" version=\"1.0.0\">");
        appendInfoTributaria(xml, document, configuration, "04");
        xml.append("<infoNotaCredito>");
        xml.append(tag("fechaEmision", document.getIssueDate().format(XML_DATE)));
        xml.append(tag("dirEstablecimiento", configuration.getEstablishmentAddress()));
        if (!isBlank(configuration.getSpecialTaxpayer())) {
            xml.append(tag("contribuyenteEspecial", configuration.getSpecialTaxpayer()));
        }
        xml.append(tag("obligadoContabilidad", normalizeObligatedAccounting(configuration.getObligatedAccounting())));
        xml.append(tag("tipoIdentificacionComprador", resolveBuyerIdentificationType(document.getBuyerIdentification())));
        xml.append(tag("razonSocialComprador", resolveBuyerName(document)));
        xml.append(tag("identificacionComprador", resolveBuyerIdentification(document)));
        xml.append(tag("codDocModificado", "01"));
        xml.append(tag("numDocModificado", formatSequential(baseInvoice)));
        xml.append(tag("fechaEmisionDocSustento", baseInvoice.getIssueDate().format(XML_DATE)));
        xml.append(tag("totalSinImpuestos", formatDecimal(document.getSubtotalAmount())));
        xml.append(tag("valorModificacion", formatDecimal(document.getTotalAmount())));
        xml.append(tag("moneda", "DOLAR"));
        xml.append("<totalConImpuestos>");
        appendAggregatedTaxes(xml, document.getTenantId(), sourceLines, persistedTaxLines);
        xml.append("</totalConImpuestos>");
        xml.append(tag("motivo", "Reembolso o anulacion de venta"));
        xml.append("</infoNotaCredito>");
        appendDetails(xml, document.getTenantId(), sourceLines);
        xml.append("</notaCredito>");
        return xml.toString();
    }

    private void appendInfoTributaria(StringBuilder xml, TaxDocumentEntity document, SriConfigurationEntity configuration, String documentCode) {
        xml.append("<infoTributaria>");
        xml.append(tag("ambiente", normalizeEnvironmentCode(configuration.getEnvironmentCode())));
        xml.append(tag("tipoEmision", normalizeEmissionCode(configuration.getEmissionCode())));
        xml.append(tag("razonSocial", configuration.getIssuerLegalName()));
        if (!isBlank(configuration.getIssuerTradeName())) {
            xml.append(tag("nombreComercial", configuration.getIssuerTradeName()));
        }
        xml.append(tag("ruc", digitsOnly(configuration.getIssuerRuc())));
        xml.append(tag("claveAcceso", buildAccessKey(document, configuration, documentCode)));
        xml.append(tag("codDoc", documentCode));
        xml.append(tag("estab", leftPad(document.getEstablishmentCode(), 3)));
        xml.append(tag("ptoEmi", leftPad(document.getEmissionPointCode(), 3)));
        xml.append(tag("secuencial", leftPad(document.getSequentialNumber(), 9)));
        xml.append(tag("dirMatriz", configuration.getMatrixAddress()));
        if (Boolean.TRUE.equals(configuration.getRimpeContributor())) {
            xml.append(tagRaw("contribuyenteRimpe", resolveRimpeText(configuration)));
        }
        xml.append("</infoTributaria>");
    }

    private void appendAggregatedTaxes(StringBuilder xml, Long tenantId, List<SourceLine> sourceLines, List<TaxDocumentTaxLineEntity> persistedTaxLines) {
        Map<Long, TaxRuleEntity> rulesById = loadTaxRulesById(tenantId, sourceLines, persistedTaxLines);
        Map<String, TaxAggregate> aggregates = new LinkedHashMap<>();
        for (SourceLine line : sourceLines) {
            TaxRuleEntity rule = line.taxRuleId() != null ? rulesById.get(line.taxRuleId()) : null;
            TaxMapping mapping = resolveTaxMapping(rule, line.percentage(), line.taxAmount());
            String key = mapping.code() + "|" + mapping.percentageCode();
            TaxAggregate aggregate = aggregates.computeIfAbsent(key,
                    ignored -> new TaxAggregate(mapping.code(), mapping.percentageCode(), BigDecimal.ZERO, BigDecimal.ZERO));
            aggregate.baseImponible = aggregate.baseImponible.add(amount(line.subtotalAmount()));
            aggregate.valor = aggregate.valor.add(amount(line.taxAmount()));
        }
        if (aggregates.isEmpty()) {
            xml.append("<totalImpuesto>")
                    .append(tag("codigo", "2"))
                    .append(tag("codigoPorcentaje", "0"))
                    .append(tag("baseImponible", "0.00"))
                    .append(tag("valor", "0.00"))
                    .append("</totalImpuesto>");
            return;
        }
        for (TaxAggregate aggregate : aggregates.values()) {
            xml.append("<totalImpuesto>")
                    .append(tag("codigo", aggregate.code))
                    .append(tag("codigoPorcentaje", aggregate.percentageCode))
                    .append(tag("baseImponible", formatDecimal(aggregate.baseImponible)))
                    .append(tag("valor", formatDecimal(aggregate.valor)))
                    .append("</totalImpuesto>");
        }
    }

    private void appendDetails(StringBuilder xml, Long tenantId, List<SourceLine> sourceLines) {
        Map<Long, TaxRuleEntity> rulesById = loadTaxRulesById(tenantId, sourceLines, List.of());
        xml.append("<detalles>");
        for (SourceLine line : sourceLines) {
            xml.append("<detalle>");
            xml.append(tag("codigoPrincipal", line.productCode()));
            xml.append(tag("descripcion", line.description()));
            xml.append(tag("cantidad", String.valueOf(line.quantity())));
            xml.append(tag("precioUnitario", formatDecimal(line.unitPrice())));
            xml.append(tag("descuento", "0.00"));
            xml.append(tag("precioTotalSinImpuesto", formatDecimal(line.subtotalAmount())));
            if (amount(line.taxAmount()).signum() > 0) {
                TaxRuleEntity rule = line.taxRuleId() != null ? rulesById.get(line.taxRuleId()) : null;
                TaxMapping mapping = resolveTaxMapping(rule, line.percentage(), line.taxAmount());
                xml.append("<impuestos><impuesto>")
                        .append(tag("codigo", mapping.code()))
                        .append(tag("codigoPorcentaje", mapping.percentageCode()))
                        .append(tag("tarifa", formatPercentage(line.percentage())))
                        .append(tag("baseImponible", formatDecimal(line.subtotalAmount())))
                        .append(tag("valor", formatDecimal(line.taxAmount())))
                        .append("</impuesto></impuestos>");
            }
            xml.append("</detalle>");
        }
        xml.append("</detalles>");
    }

    private List<SriFacturaXml.TotalImpuesto> buildInvoiceTotalTaxes(Long tenantId,
                                                                     List<SourceLine> sourceLines,
                                                                     List<TaxDocumentTaxLineEntity> persistedTaxLines) {
        Map<Long, TaxRuleEntity> rulesById = loadTaxRulesById(tenantId, sourceLines, persistedTaxLines);
        Map<String, TaxAggregate> aggregates = new LinkedHashMap<>();
        for (SourceLine line : sourceLines) {
            TaxRuleEntity rule = line.taxRuleId() != null ? rulesById.get(line.taxRuleId()) : null;
            TaxMapping mapping = resolveTaxMapping(rule, line.percentage(), line.taxAmount());
            String key = mapping.code() + "|" + mapping.percentageCode() + "|" + formatPercentage(line.percentage());
            TaxAggregate aggregate = aggregates.computeIfAbsent(key,
                    ignored -> new TaxAggregate(mapping.code(), mapping.percentageCode(), BigDecimal.ZERO, BigDecimal.ZERO));
            aggregate.baseImponible = aggregate.baseImponible.add(amount(line.subtotalAmount()));
            aggregate.valor = aggregate.valor.add(amount(line.taxAmount()));
            aggregate.tarifa = line.percentage();
        }
        if (aggregates.isEmpty()) {
            return List.of(new SriFacturaXml.TotalImpuesto("2", "0", null, "0.00", "0.00", "0.00"));
        }
        return aggregates.values().stream()
                .map(aggregate -> new SriFacturaXml.TotalImpuesto(
                        aggregate.code,
                        aggregate.percentageCode,
                        null,
                        formatDecimal(aggregate.baseImponible),
                        formatPercentage(aggregate.tarifa),
                        formatDecimal(aggregate.valor)))
                .toList();
    }

    private List<SriFacturaXml.Detalle> buildInvoiceDetails(Long tenantId, List<SourceLine> sourceLines) {
        Map<Long, TaxRuleEntity> rulesById = loadTaxRulesById(tenantId, sourceLines, List.of());
        List<SriFacturaXml.Detalle> details = new ArrayList<>();
        for (SourceLine line : sourceLines) {
            TaxRuleEntity rule = line.taxRuleId() != null ? rulesById.get(line.taxRuleId()) : null;
            TaxMapping mapping = resolveTaxMapping(rule, line.percentage(), line.taxAmount());
            SriFacturaXml.Impuestos impuestos = new SriFacturaXml.Impuestos();
            impuestos.getImpuesto().add(new SriFacturaXml.Impuesto(
                    mapping.code(),
                    mapping.percentageCode(),
                    formatPercentage(line.percentage()),
                    formatDecimal(line.subtotalAmount()),
                    formatDecimal(line.taxAmount())
            ));

            details.add(new SriFacturaXml.Detalle(
                    cleanCode(line.productCode()),
                    null,
                    cleanAndLimit(line.description(), 300),
                    "u",
                    formatQuantity(line.quantity()),
                    formatDecimal(line.unitPrice()),
                    null,
                    "0.00",
                    formatDecimal(line.subtotalAmount()),
                    null,
                    impuestos
            ));
        }
        return details;
    }

    public String buildAccessKey(TaxDocumentEntity document, SriConfigurationEntity configuration, String documentCode) {
        return sriKeyService.generarClave(
                document.getIssueDate().format(ACCESS_KEY_DATE),
                documentCode,
                digitsOnly(configuration.getIssuerRuc()),
                normalizeEnvironmentCode(configuration.getEnvironmentCode()),
                leftPad(document.getEstablishmentCode(), 3) + leftPad(document.getEmissionPointCode(), 3),
                leftPad(document.getSequentialNumber(), 9),
                leftPad(String.valueOf(Math.abs((document.getId() != null ? document.getId() : document.getSourceId()) % 100_000_000L)), 8),
                normalizeEmissionCode(configuration.getEmissionCode())
        );
    }

    public String resolveSriDocumentCode(TaxDocumentEntity document) {
        return switch (document.getDocumentType().getDocumentCode().toUpperCase(Locale.ROOT)) {
            case "SRI_INVOICE", "SALES_INVOICE" -> "01";
            case "SRI_CREDIT_NOTE" -> "04";
            default -> null;
        };
    }

    private Map<Long, TaxRuleEntity> loadTaxRulesById(Long tenantId, List<SourceLine> sourceLines, List<TaxDocumentTaxLineEntity> persistedTaxLines) {
        List<Long> requestedIds = new ArrayList<>();
        sourceLines.stream().map(SourceLine::taxRuleId).filter(Objects::nonNull).forEach(requestedIds::add);
        persistedTaxLines.stream().map(TaxDocumentTaxLineEntity::getTaxRuleId).filter(Objects::nonNull).forEach(requestedIds::add);
        Map<Long, TaxRuleEntity> rulesById = new LinkedHashMap<>();
        if (!requestedIds.isEmpty()) {
            for (TaxRuleEntity rule : taxRuleRepository.findAllById(requestedIds.stream().distinct().toList())) {
                if (tenantId.equals(rule.getTenantId())) {
                    rulesById.put(rule.getId(), rule);
                }
            }
        }
        return rulesById;
    }

    private TaxMapping resolveTaxMapping(TaxRuleEntity rule, BigDecimal percentage, BigDecimal taxAmount) {
        if (rule != null) {
            BigDecimal effectivePercentage = rule.getRate() != null ? rule.getRate() : percentage;
            String fallbackPercentageCode = fallbackPercentageCode(effectivePercentage, taxAmount);
            String configuredPercentageCode = rule.getTaxableObjectCode();
            return new TaxMapping(
                    normalizeTaxAuthorityCode(rule.getTaxAuthorityCode()),
                    shouldUseFallbackPercentageCode(effectivePercentage, configuredPercentageCode)
                            ? fallbackPercentageCode
                            : configuredPercentageCode);
        }
        return new TaxMapping("2", fallbackPercentageCode(percentage, taxAmount));
    }

    private String normalizeTaxAuthorityCode(String value) {
        if (value == null || !value.matches("[235]")) {
            return "2";
        }
        return value;
    }

    private String fallbackPercentageCode(BigDecimal percentage, BigDecimal taxAmount) {
        if (amount(taxAmount).signum() == 0 || percentage == null || percentage.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        BigDecimal normalizedPercentage = percentage.setScale(2, RoundingMode.HALF_UP);
        if (normalizedPercentage.compareTo(new BigDecimal("12.00")) == 0) {
            return "2";
        }
        if (normalizedPercentage.compareTo(new BigDecimal("14.00")) == 0) {
            return "3";
        }
        if (normalizedPercentage.compareTo(new BigDecimal("15.00")) == 0) {
            return "4";
        }
        if (normalizedPercentage.compareTo(new BigDecimal("5.00")) == 0) {
            return "5";
        }
        if (normalizedPercentage.compareTo(new BigDecimal("13.00")) == 0) {
            return "10";
        }
        return "4";
    }

    private boolean shouldUseFallbackPercentageCode(BigDecimal percentage, String configuredCode) {
        if (isBlank(configuredCode)) {
            return true;
        }
        String expectedCode = fallbackPercentageCode(percentage, BigDecimal.ONE);
        if ("4".equals(expectedCode) && percentage != null && percentage.setScale(2, RoundingMode.HALF_UP).compareTo(new BigDecimal("15.00")) == 0) {
            return !"4".equals(configuredCode);
        }
        return false;
    }

    private String resolveBuyerIdentification(TaxDocumentEntity document) {
        String identification = digitsOnly(document.getBuyerIdentification());
        if (identification.isBlank()) {
            return DEFAULT_CONSUMER_ID;
        }
        return identification.length() == 10 || identification.length() == 13
                ? identification
                : document.getBuyerIdentification().trim();
    }

    private String resolveBuyerName(TaxDocumentEntity document) {
        return isBlank(document.getBuyerName()) ? DEFAULT_CONSUMER_NAME : document.getBuyerName().trim();
    }

    private String resolveBuyerEmail(TaxDocumentEntity document) {
        if (document.getSourceType() == null || !document.getSourceType().startsWith("ORDER_")) {
            return null;
        }
        return orderRepository.findByTenantIdAndId(document.getTenantId(), document.getSourceId())
                .map(order -> {
                    if (order.getCustomerEmail() != null && !order.getCustomerEmail().isBlank()) {
                        return order.getCustomerEmail().trim();
                    }
                    if (order.getUser() != null && order.getUser().getEmail() != null
                            && !order.getUser().getEmail().isBlank()) {
                        return order.getUser().getEmail().trim();
                    }
                    return null;
                })
                .orElse(null);
    }

    private String resolveBuyerAddress(TaxDocumentEntity document) {
        if (document.getSourceType() == null || !document.getSourceType().startsWith("ORDER_")) {
            return null;
        }
        return orderRepository.findByTenantIdAndId(document.getTenantId(), document.getSourceId())
                .map(order -> {
                    if (order.getCustomerAddress() != null && !order.getCustomerAddress().isBlank()) {
                        return order.getCustomerAddress().trim();
                    }
                    return null;
                })
                .orElse(null);
    }

    private String resolveBuyerPhone(TaxDocumentEntity document) {
        if (document.getSourceType() == null || !document.getSourceType().startsWith("ORDER_")) {
            return null;
        }
        return orderRepository.findByTenantIdAndId(document.getTenantId(), document.getSourceId())
                .map(order -> {
                    if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
                        return order.getCustomerPhone().trim();
                    }
                    return null;
                })
                .orElse(null);
    }

    private String resolveBuyerIdentificationType(String identification) {
        String digits = digitsOnly(identification);
        if (digits.isBlank() || DEFAULT_CONSUMER_ID.equals(digits)) {
            return "07";
        }
        if (digits.length() == 13) {
            return "04";
        }
        if (digits.length() == 10) {
            return "05";
        }
        return "06";
    }

    private String resolvePaymentCode(TaxDocumentEntity document) {
        if (!"ORDER_SALE".equalsIgnoreCase(document.getSourceType())) {
            return "20";
        }
        OrderEntity order = orderRepository.findByTenantIdAndId(document.getTenantId(), document.getSourceId())
                .orElseThrow(() -> new NotFoundException("No existe la orden origen para resolver el codigo de pago"));
        if (order.getPaymentMethod() == null) {
            throw new BadRequestException("La orden origen no tiene metodo de pago para resolver el codigo SRI");
        }
        return switch (order.getPaymentMethod()) {
            case CASH -> "01";
            case CARD -> "19";
            case TRANSFER -> "20";
        };
    }

    private String normalizeEnvironmentCode(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("El ambiente SRI es obligatorio");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "1", "TEST", "TESTING" -> "1";
            case "2", "PROD", "PRODUCTION" -> "2";
            default -> throw new BadRequestException("Ambiente SRI invalido: " + value);
        };
    }

    private String normalizeEmissionCode(String value) {
        String digits = digitsOnly(value);
        return digits.isBlank() ? "1" : digits.substring(0, 1);
    }

    private String normalizeObligatedAccounting(String value) {
        return "SI".equalsIgnoreCase(value != null ? value.trim() : "") ? "SI" : "NO";
    }

    private String resolveRimpeText(SriConfigurationEntity configuration) {
        return Boolean.TRUE.equals(configuration.getRimpeContributor())
                ? "CONTRIBUYENTE RÉGIMEN RIMPE"
                : null;
    }

    private String formatSequential(TaxDocumentEntity document) {
        return leftPad(document.getEstablishmentCode(), 3) + "-" + leftPad(document.getEmissionPointCode(), 3) + "-" + leftPad(document.getSequentialNumber(), 9);
    }

    private BigDecimal resolveTaxPercentage(BigDecimal subtotal, BigDecimal taxAmount) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) == 0 || taxAmount == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return amount(taxAmount).multiply(BigDecimal.valueOf(100)).divide(amount(subtotal), 4, RoundingMode.HALF_UP);
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String leftPad(String value, int size) {
        String source = value == null ? "" : value.trim();
        if (source.length() >= size) {
            return source.substring(source.length() - size);
        }
        return "0".repeat(size - source.length()) + source;
    }

    private String tag(String name, String value) {
        return "<" + name + ">" + escapeXml(sriTextSanitizer.clean(value)) + "</" + name + ">";
    }

    private String tagRaw(String name, String value) {
        return "<" + name + ">" + escapeXml(value) + "</" + name + ">";
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String formatDecimal(BigDecimal value) {
        return amount(value).toPlainString();
    }

    private String formatQuantity(Integer value) {
        return BigDecimal.valueOf(value != null ? value : 0)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String formatPercentage(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal amount(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String cleanAndLimit(String value, int maxLength) {
        String cleaned = sriTextSanitizer.cleanAndLimit(value, maxLength);
        return cleaned.isBlank() ? "SIN INFORMACION" : cleaned;
    }

    private String cleanOptionalAndLimit(String value, int maxLength) {
        String cleaned = sriTextSanitizer.cleanAndLimit(value, maxLength);
        return cleaned.isBlank() ? null : cleaned;
    }

    private String cleanCode(String value) {
        String cleaned = sriTextSanitizer.cleanAndLimit(value, 25);
        return cleaned.isBlank() ? "SIN-CODIGO" : cleaned;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record PreparedDocument(
            String accessKey,
            String xmlPayload,
            List<String> errors,
            List<SourceLine> sourceLines) {
    }

    private record TaxMapping(String code, String percentageCode) {
    }

    private static final class TaxAggregate {
        private final String code;
        private final String percentageCode;
        private BigDecimal baseImponible;
        private BigDecimal valor;
        private BigDecimal tarifa = BigDecimal.ZERO;

        private TaxAggregate(String code, String percentageCode, BigDecimal baseImponible, BigDecimal valor) {
            this.code = code;
            this.percentageCode = percentageCode;
            this.baseImponible = baseImponible;
            this.valor = valor;
        }
    }

    public record SourceLine(
            String productCode,
            String description,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotalAmount,
            BigDecimal taxAmount,
            Long taxRuleId,
            BigDecimal percentage) {
    }
}
