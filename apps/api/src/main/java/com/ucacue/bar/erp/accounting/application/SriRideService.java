package com.ucacue.bar.erp.accounting.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.RideTemplateEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.SriConfigurationEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.RideTemplateRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.SriConfigurationRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SriRideService {

    private static final String DEFAULT_TEMPLATE_CODE = "DEFAULT";
    private static final Pattern UNSAFE_TEMPLATE_PATTERN = Pattern.compile(
            "(?is)<\\s*(script|iframe|object|embed|form|meta|link)\\b|javascript\\s*:|srcdoc\\s*=|data\\s*:\\s*text/html|\\son[a-z]+\\s*=");
    private static final DateTimeFormatter AUTHORIZATION_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final TenantContextResolver tenantContextResolver;
    private final TaxDocumentRepository taxDocumentRepository;
    private final SriConfigurationRepository sriConfigurationRepository;
    private final RideTemplateRepository rideTemplateRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final Map<Long, RideTemplateEntity> templateCache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public RideRenderResult previewDocument(String tenantCode, Long taxDocumentId) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        TaxDocumentEntity document = taxDocumentRepository.findByTenantIdAndId(tenantId, taxDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento tributario no encontrado"));
        return render(document, loadConfiguration(tenantId), resolveTemplate(tenantId));
    }

    @Transactional(readOnly = true)
    public RideRenderResult previewLatest(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        RideTemplateEntity template = resolveTemplate(tenantId);
        SriConfigurationEntity configuration = loadConfiguration(tenantId);
        return taxDocumentRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
                .map(document -> render(document, configuration, template))
                .orElseGet(() -> renderSample(tenantId, configuration, template));
    }

    @Transactional(readOnly = true)
    public RidePdfResult generatePdf(String tenantCode, Long taxDocumentId) {
        RideRenderResult rendered = previewDocument(tenantCode, taxDocumentId);
        return renderPdf(rendered);
    }

    @Transactional(readOnly = true)
    public RidePdfResult generatePdfForTenant(Long tenantId, Long taxDocumentId) {
        TaxDocumentEntity document = taxDocumentRepository.findByTenantIdAndId(tenantId, taxDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento tributario no encontrado"));
        RideRenderResult rendered = render(document, loadConfiguration(tenantId), resolveTemplate(tenantId));
        return renderPdf(rendered);
    }

    private RidePdfResult renderPdf(RideRenderResult rendered) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerPdfFonts(builder);
            builder.withHtmlContent(rendered.html(), null);
            builder.toStream(output);
            builder.run();
            String fileName = "ride-" + (rendered.accessKey() != null && !rendered.accessKey().isBlank()
                    ? rendered.accessKey()
                    : rendered.documentId()) + ".pdf";
            return new RidePdfResult(rendered.documentId(), rendered.accessKey(), fileName, output.toByteArray());
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo generar el PDF RIDE: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public RideTemplateView getCurrentTemplate(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return toView(resolveTemplate(tenantId));
    }

    @Transactional
    public RideTemplateView saveTemplate(String tenantCode, UpsertRideTemplateCommand command) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        if (command == null) {
            throw new BadRequestException("La plantilla RIDE es obligatoria");
        }
        String templateCode = normalizeTemplateCode(command.templateCode());
        String htmlContent = command.htmlContent();
        if (htmlContent != null && !htmlContent.isBlank()) {
            validateTemplateHtml(htmlContent);
        }

        RideTemplateEntity entity = rideTemplateRepository
                .findByTenantIdAndTemplateCodeIgnoreCase(tenantId, templateCode)
                .orElseGet(RideTemplateEntity::new);
        entity.setTenantId(tenantId);
        entity.setTemplateCode(templateCode);
        entity.setTemplateName(command.templateName() == null || command.templateName().isBlank()
                ? "InsightVision RIDE"
                : command.templateName().trim());
        entity.setLayoutJson(serializeLayout(mergeLayout(command.layout())));
        entity.setHtmlContent(htmlContent == null || htmlContent.isBlank() ? null : htmlContent.trim());
        entity.setCustom(true);
        entity.setActive(command.active() == null || command.active());
        RideTemplateEntity saved = rideTemplateRepository.save(entity);
        templateCache.remove(tenantId);
        auditService.logAction(null, "SAVE", "RIDE_TEMPLATE", saved.getId(),
                "Plantilla " + saved.getTemplateCode() + " (active=" + saved.getActive() + ")");
        return toView(saved);
    }

    private RideRenderResult render(TaxDocumentEntity document, SriConfigurationEntity configuration, RideTemplateEntity template) {
        Map<String, Object> layout = resolveLayout(template);
        Map<String, Object> context = buildContext(document, configuration);
        context.put("layout", layout);
        String html = buildHtmlDocument(layout, context, templateBody(template));
        return new RideRenderResult(document.getId(), stringValue(context, "seguridad", "claveAcceso"), html);
    }

    private RideRenderResult renderSample(Long tenantId, SriConfigurationEntity configuration, RideTemplateEntity template) {
        Map<String, Object> layout = resolveLayout(template);
        Map<String, Object> context = buildSampleContext(tenantId, configuration);
        context.put("layout", layout);
        String html = buildHtmlDocument(layout, context, templateBody(template));
        return new RideRenderResult(null, stringValue(context, "seguridad", "claveAcceso"), html);
    }

    private Map<String, Object> buildContext(TaxDocumentEntity document, SriConfigurationEntity configuration) {
        Element root = parseFacturaRoot(resolveXmlPayload(document));
        Element infoTributaria = requireDescendant(root, "infoTributaria");
        Element infoFactura = requireDescendant(root, "infoFactura");

        String accessKey = firstNonBlank(childText(infoTributaria, "claveAcceso"), document.getAccessKey());
        List<Map<String, Object>> detalles = parseDetails(root);
        List<Map<String, Object>> impuestos = parseTotalTaxes(infoFactura, detalles);
        Totals totals = calculateTotals(infoFactura, detalles, impuestos);
        boolean rimpe = !isBlank(childText(infoTributaria, "contribuyenteRimpe"))
                || (configuration != null && Boolean.TRUE.equals(configuration.getRimpeContributor()));
        boolean retentionAgent = configuration != null && Boolean.TRUE.equals(configuration.getRetentionAgent());
        boolean iva15 = hasIva15(impuestos, detalles);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("legal", mapOf(
                "ruc", firstNonBlank(childText(infoTributaria, "ruc"), configuration != null ? configuration.getIssuerRuc() : null),
                "razonSocial", firstNonBlank(childText(infoTributaria, "razonSocial"), configuration != null ? configuration.getIssuerLegalName() : null),
                "nombreComercial", firstNonBlank(childText(infoTributaria, "nombreComercial"), configuration != null ? configuration.getIssuerTradeName() : null),
                "dirMatriz", firstNonBlank(childText(infoTributaria, "dirMatriz"), configuration != null ? configuration.getMatrixAddress() : null),
                "dirEstablecimiento", firstNonBlank(childText(infoFactura, "dirEstablecimiento"), configuration != null ? configuration.getEstablishmentAddress() : null),
                "obligadoContabilidad", firstNonBlank(childText(infoFactura, "obligadoContabilidad"), configuration != null ? configuration.getObligatedAccounting() : null, "NO"),
                "contribuyenteEspecial", firstNonBlank(childText(infoFactura, "contribuyenteEspecial"), configuration != null ? configuration.getSpecialTaxpayer() : null, "No aplica"),
                "contribuyenteRimpe", rimpe ? firstNonBlank(childText(infoTributaria, "contribuyenteRimpe"), rimpeText()) : null,
                "agenteRetencion", retentionAgent ? retentionAgentText(configuration.getRetentionAgentResolution()) : null,
                "ambiente", childText(infoTributaria, "ambiente"),
                "ambienteLabel", "2".equals(childText(infoTributaria, "ambiente")) ? "Produccion" : "Pruebas",
                "tipoEmision", firstNonBlank(childText(infoTributaria, "tipoEmision"), "1")
        ));
        context.put("documento", mapOf(
                "id", document.getId(),
                "codDoc", childText(infoTributaria, "codDoc"),
                "tipo", resolveDocumentLabel(childText(infoTributaria, "codDoc")),
                "estab", childText(infoTributaria, "estab"),
                "ptoEmi", childText(infoTributaria, "ptoEmi"),
                "secuencial", childText(infoTributaria, "secuencial"),
                "numero", formatDocumentNumber(infoTributaria, document),
                "fechaEmision", firstNonBlank(childText(infoFactura, "fechaEmision"), document.getIssueDate() != null ? document.getIssueDate().toString() : null),
                "autorizacion", firstNonBlank(document.getAuthorizationCode(), accessKey),
                "fechaAutorizacion", document.getAuthorizedAt() != null ? document.getAuthorizedAt().format(AUTHORIZATION_DATE) : null,
                "estado", document.getStatus() != null ? document.getStatus().name() : null
        ));
        context.put("cliente", mapOf(
                "nombre", firstNonBlank(childText(infoFactura, "razonSocialComprador"), document.getBuyerName()),
                "identificacion", firstNonBlank(childText(infoFactura, "identificacionComprador"), document.getBuyerIdentification()),
                "tipoIdentificacion", childText(infoFactura, "tipoIdentificacionComprador"),
                "direccion", firstNonBlank(additionalInfoValue(root, "Direccion"), additionalInfoValue(root, "Dirección"), "No registrada"),
                "email", firstNonBlank(additionalInfoValue(root, "Email"), additionalInfoValue(root, "Correo"), "No registrado")
        ));
        context.put("detalles", detalles);
        context.put("impuestos", impuestos);
        context.put("totales", mapOf(
                "subtotal", money(totals.subtotal()),
                "descuento", money(totals.discount()),
                "iva15", money(totals.iva15()),
                "impuestosTotal", money(totals.tax()),
                "propina", money(decimal(childText(infoFactura, "propina"))),
                "total", money(totals.total()),
                "moneda", firstNonBlank(childText(infoFactura, "moneda"), "DOLAR"),
                "diferenciaCentavos", money(totals.difference()),
                "pagos", parsePayments(infoFactura, totals.total())
        ));
        context.put("seguridad", mapOf(
                "claveAcceso", accessKey,
                "qrDataUri", generateQrDataUri(accessKey)
        ));
        context.put("leyendas", buildLegends(rimpe, retentionAgent, configuration != null ? configuration.getRetentionAgentResolution() : null, iva15));
        context.put("infoAdicional", parseAdditionalInfo(root));
        return context;
    }

    private Map<String, Object> buildSampleContext(Long tenantId, SriConfigurationEntity configuration) {
        String accessKey = "1604202601"
                + leftPad(configuration != null ? digitsOnly(configuration.getIssuerRuc()) : "9999999999999", 13)
                + "1" + "001001" + "000000001" + "12345678" + "1" + "0";
        List<Map<String, Object>> detalles = new ArrayList<>();
        detalles.add(mapOf(
                "codigo", "PROD-001",
                "codigoAuxiliar", "BAR-001",
                "descripcion", "Producto de ejemplo",
                "cantidad", "2.00",
                "precioUnitario", "5.00",
                "descuento", "0.00",
                "subtotal", "10.00",
                "ivaCodigo", "2",
                "ivaCodigoPorcentaje", "4",
                "ivaTarifa", "15.00",
                "ivaBase", "10.00",
                "ivaValor", "1.50",
                "impuestos", List.of(mapOf(
                        "codigo", "2",
                        "codigoPorcentaje", "4",
                        "tarifa", "15.00",
                        "baseImponible", "10.00",
                        "valor", "1.50",
                        "label", "Tarifa 15%"
                ))
        ));
        List<Map<String, Object>> impuestos = List.of(mapOf(
                "codigo", "2",
                "codigoPorcentaje", "4",
                "tarifa", "15.00",
                "baseImponible", "10.00",
                "valor", "1.50",
                "label", "Tarifa 15%"
        ));

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("legal", mapOf(
                "ruc", configuration != null ? configuration.getIssuerRuc() : "9999999999999",
                "razonSocial", configuration != null ? configuration.getIssuerLegalName() : "NEGOCIO DE EJEMPLO",
                "nombreComercial", configuration != null ? configuration.getIssuerTradeName() : "InsightVision",
                "dirMatriz", configuration != null ? configuration.getMatrixAddress() : "Direccion matriz",
                "dirEstablecimiento", configuration != null ? configuration.getEstablishmentAddress() : "Direccion establecimiento",
                "obligadoContabilidad", configuration != null ? configuration.getObligatedAccounting() : "NO",
                "contribuyenteEspecial", configuration != null ? firstNonBlank(configuration.getSpecialTaxpayer(), "No aplica") : "No aplica",
                "contribuyenteRimpe", configuration != null && Boolean.TRUE.equals(configuration.getRimpeContributor()) ? rimpeText() : null,
                "agenteRetencion", configuration != null && Boolean.TRUE.equals(configuration.getRetentionAgent())
                        ? retentionAgentText(configuration.getRetentionAgentResolution())
                        : null,
                "ambiente", configuration != null ? configuration.getEnvironmentCode() : "1",
                "ambienteLabel", configuration != null && "2".equals(configuration.getEnvironmentCode()) ? "Produccion" : "Pruebas",
                "tipoEmision", "1"
        ));
        context.put("documento", mapOf(
                "id", null,
                "codDoc", "01",
                "tipo", "Factura",
                "estab", "001",
                "ptoEmi", "001",
                "secuencial", "000000001",
                "numero", "001-001-000000001",
                "fechaEmision", "16/04/2026",
                "autorizacion", accessKey,
                "fechaAutorizacion", LocalDateTime.now().format(AUTHORIZATION_DATE),
                "estado", "PREVIEW"
        ));
        context.put("cliente", mapOf(
                "nombre", "Consumidor final",
                "identificacion", "9999999999999",
                "tipoIdentificacion", "07",
                "direccion", "No registrada",
                "email", "No registrado"
        ));
        context.put("detalles", detalles);
        context.put("impuestos", impuestos);
        context.put("totales", mapOf(
                "subtotal", "10.00",
                "descuento", "0.00",
                "iva15", "1.50",
                "impuestosTotal", "1.50",
                "propina", "0.00",
                "total", "11.50",
                "moneda", "DOLAR",
                "diferenciaCentavos", "0.00",
                "pagos", List.of(mapOf(
                        "formaPago", "20 - Transferencia, deposito u otro pago bancario",
                        "total", "11.50",
                        "plazo", "0",
                        "unidadTiempo", "dias"
                ))
        ));
        context.put("seguridad", mapOf(
                "claveAcceso", accessKey,
                "qrDataUri", generateQrDataUri(accessKey)
        ));
        context.put("leyendas", buildLegends(
                configuration != null && Boolean.TRUE.equals(configuration.getRimpeContributor()),
                configuration != null && Boolean.TRUE.equals(configuration.getRetentionAgent()),
                configuration != null ? configuration.getRetentionAgentResolution() : null,
                true));
        context.put("infoAdicional", List.of(mapOf("nombre", "Vista previa", "valor", "Tenant " + tenantId)));
        return context;
    }

    private String buildHtmlDocument(Map<String, Object> layout, Map<String, Object> context, String bodyTemplate) {
        validateTemplateHtml(bodyTemplate);
        String renderedBody;
        try {
            Handlebars handlebars = new Handlebars();
            Template compiled = handlebars.compileInline(bodyTemplate);
            renderedBody = compiled.apply(context);
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo procesar la plantilla RIDE: " + ex.getMessage());
        }

        Map<String, Object> legal = nestedMap(context, "legal");
        Map<String, Object> document = nestedMap(context, "documento");
        Map<String, Object> security = nestedMap(context, "seguridad");
        StringBuilder legends = new StringBuilder();
        for (Object legend : listValue(context.get("leyendas"))) {
            legends.append("<span>").append(escapeHtml(String.valueOf(legend))).append("</span>");
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                """.stripIndent()
                + buildCss(layout)
                + """
                  </style>
                </head>
                <body>
                  <main class="a4-sheet a4-paper ride-a4">
                    <header class="ride-header">
                      <div class="ride-brand">
                """.stripIndent()
                + renderLogo(layout)
                + "        <div class=\"ride-brand-copy\">\n"
                + "          <p class=\"ride-overline\">Representacion impresa del comprobante electronico</p>\n"
                + "          <h1>" + escapeHtml(value(legal, "razonSocial")) + "</h1>\n"
                + "          <strong>" + escapeHtml(value(legal, "nombreComercial")) + "</strong>\n"
                + "          <span>Direccion matriz: " + escapeHtml(value(legal, "dirMatriz")) + "</span>\n"
                + "          <span>Direccion establecimiento: " + escapeHtml(value(legal, "dirEstablecimiento")) + "</span>\n"
                + "        </div>\n"
                + "      </div>\n"
                + "      <aside class=\"sri-card ride-legal-card\">\n"
                + "        <div class=\"ride-legal-card__top\">\n"
                + "          <span class=\"ride-doc-type\">" + escapeHtml(value(document, "tipo")) + "</span>\n"
                + "          <table class=\"ride-legal-grid\"><tbody>\n"
                + "            <tr><td>RUC</td><th>" + escapeHtml(value(legal, "ruc")) + "</th></tr>\n"
                + "            <tr><td>Numero</td><th>" + escapeHtml(value(document, "numero")) + "</th></tr>\n"
                + "            <tr><td>Ambiente</td><th>" + escapeHtml(value(legal, "ambienteLabel")) + "</th></tr>\n"
                + "            <tr><td>Obligado contabilidad</td><th>" + escapeHtml(value(legal, "obligadoContabilidad")) + "</th></tr>\n"
                + "          </tbody></table>\n"
                + "        </div>\n"
                + "        <table class=\"ride-security\"><tbody><tr>\n"
                + "          <td><span class=\"ride-label\">Clave de acceso</span><strong class=\"ride-access-key\">" + escapeHtml(value(security, "claveAcceso")) + "</strong></td>\n"
                + "          <td class=\"ride-qr-cell\"><img class=\"ride-qr\" src=\"" + escapeHtml(value(security, "qrDataUri")) + "\" alt=\"QR SRI\" /></td>\n"
                + "        </tr></tbody></table>\n"
                + "      </aside>\n"
                + "    </header>\n"
                + "    <div class=\"ride-legend-row\">" + legends + "</div>\n"
                + "    <section class=\"ride-content\">\n"
                + renderedBody
                + "    </section>\n"
                + "    <footer class=\"ride-footer-note\">" + escapeHtml(string(layout.get("footerNote"), "Documento generado por InsightVision")) + "</footer>\n"
                + "  </main>\n"
                + "</body>\n"
                + "</html>\n";
    }

    private String buildCss(Map<String, Object> layout) {
        String headerColor = cssColor(layout.get("headerColor"), "#123a5a");
        String accentColor = cssColor(layout.get("accentColor"), "#0f766e");
        String borderColor = cssColor(layout.get("borderColor"), "#d9e2ec");
        String fontFamily = cssFont(layout.get("fontFamily"), "Inter, sans-serif");
        return """
                @font-face { font-family: 'Montserrat'; src: url('/fonts/Montserrat-Regular.ttf') format('truetype'); font-weight: 400; font-style: normal; }
                @font-face { font-family: 'Montserrat'; src: url('/fonts/Montserrat-Bold.ttf') format('truetype'); font-weight: 700; font-style: normal; }
                @font-face { font-family: 'Inter'; src: url('/fonts/Inter-Regular.ttf') format('truetype'); font-weight: 400; font-style: normal; }
                @font-face { font-family: 'Roboto'; src: url('/fonts/Roboto-Regular.ttf') format('truetype'); font-weight: 400; font-style: normal; }
                @page { size: A4; margin: 0; }
                * { box-sizing: border-box; }
                body { margin: 0; color: #1d2430; font-family: %s; font-size: 11.6px; line-height: 1.42; background: #ffffff; }
                .a4-sheet, .a4-paper { width: 210mm; min-height: 297mm; background: #ffffff; border-radius: 4px; }
                .ride-a4 { padding: 13mm 14mm 12mm; background: #ffffff; border-top: 4mm solid %s; }
                .ride-header { display: table; width: 100%%; table-layout: fixed; border-bottom: 1px solid #e2e8f0; padding-bottom: 7mm; }
                .ride-brand { display: table-cell; width: auto; padding-right: 9mm; vertical-align: top; }
                .ride-logo-box { float: left; width: 50mm; height: 21mm; background: #ffffff; border-radius: 4px; margin: 0 8mm 3mm 0; color: %s; font-family: Montserrat, sans-serif; font-weight: 700; text-align: left; }
                .ride-logo-box img { max-width: 50mm; max-height: 21mm; display: block; }
                .ride-brand-copy { overflow: hidden; }
                .ride-overline, .ride-label { margin: 0; color: #64748b; font-size: 7.5pt; font-weight: 700; text-transform: uppercase; }
                .ride-brand-copy h1 { margin: 0; color: #111827; font-family: Montserrat, sans-serif; font-size: 22px; line-height: 1.12; font-weight: 700; text-transform: uppercase; overflow-wrap: anywhere; }
                .ride-brand-copy strong { display: block; color: %s; font-size: 11px; margin: 2px 0 4px; }
                .ride-brand-copy span { display: block; color: #64748b; font-size: 7.8pt; margin-top: 2px; overflow-wrap: anywhere; }
                .sri-card, .ride-legal-card { display: table-cell; width: 74mm; background: #f8fafc; border: 1px solid #e2e8f0; border-left: 6px solid %s; border-radius: 8px; padding: 13px 14px; vertical-align: top; }
                .ride-doc-type { display: block; color: %s; font-family: Montserrat, sans-serif; font-size: 17px; font-weight: 700; text-transform: uppercase; margin-bottom: 8px; }
                .ride-legal-grid { width: 100%%; border-collapse: collapse; margin: 0; }
                .ride-legal-grid td { color: #64748b; font-size: 7.4pt; text-transform: uppercase; padding: 2px 6px 2px 0; border: 0; }
                .ride-legal-grid th { color: #111827; text-align: right; font-weight: 700; padding: 2px 0; border: 0; background: transparent; overflow-wrap: anywhere; }
                .ride-security { width: 100%%; border-collapse: collapse; margin-top: 10px; border-top: 1px solid #e2e8f0; }
                .ride-security td { border: 0; padding: 10px 0 0; vertical-align: bottom; }
                .ride-qr-cell { width: 24mm; padding-left: 9px !important; }
                .ride-access-key { display: block; margin-top: 4px; font-family: "Courier New", monospace; font-size: 8.5px; line-height: 1.35; color: #111827; overflow-wrap: anywhere; }
                .ride-legend-row { margin-top: 5mm; }
                .ride-legend-row span { display: inline-block; color: %s; border: 1px solid %s; border-radius: 8px; padding: 3px 8px; font-size: 8.4px; font-weight: 700; text-transform: uppercase; margin: 0 4px 4px 0; }
                .ride-qr { width: 24mm; height: 24mm; border: 1px solid #d7dbe1; background: #ffffff; padding: 3px; border-radius: 4px; }
                .ride-schema-grid, .ride-compliance-grid { display: table; width: 100%%; table-layout: fixed; margin-top: 5mm; border-spacing: 4mm 0; margin-left: -4mm; }
                .ride-schema-card { display: table-cell; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 4mm; vertical-align: top; }
                .ride-compliance-grid .ride-schema-card { width: 34%%; }
                .ride-schema-title { border-bottom: 1px solid #e2e8f0; padding-bottom: 2mm; margin-bottom: 3mm; }
                .ride-schema-title span { color: %s; font-family: Montserrat, sans-serif; font-size: 8px; font-weight: 700; }
                .ride-schema-title strong { float: right; color: #64748b; font-size: 7.6px; font-weight: 700; text-transform: uppercase; }
                .ride-schema-table { width: 100%%; border-collapse: collapse; }
                .ride-schema-table td { color: #64748b; font-size: 7.5px; font-weight: 700; padding: 1px 5px 1px 0; border: 0; background: transparent; }
                .ride-schema-table th { color: #111827; font-size: 8px; text-align: right; padding: 1px 0; border: 0; background: transparent; overflow-wrap: anywhere; }
                .ride-section { margin-top: 6.5mm; }
                .ride-section--client { display: table; width: 100%%; table-layout: fixed; background: #f8fafc; border: 1px solid #e2e8f0; border-left: 4px solid %s; border-radius: 8px; padding: 5mm 6mm; }
                .ride-section--client > div { display: table-cell; vertical-align: top; }
                .ride-section h2 { margin: 2px 0 0; color: #111827; font-family: Montserrat, sans-serif; font-size: 16px; line-height: 1.2; font-weight: 700; overflow-wrap: anywhere; }
                .ride-facts { width: 72mm; border-collapse: collapse; margin: 0 0 0 auto; }
                .ride-facts td { color: #737d8b; font-size: 9.5px; text-transform: uppercase; padding: 2px 8px 2px 0; border: 0; }
                .ride-facts th { color: #151a22; text-align: right; font-weight: 700; padding: 2px 0; border: 0; background: transparent; overflow-wrap: anywhere; }
                .ride-table { width: 100%%; border-collapse: collapse; margin: 0; border: 1px solid #e2e8f0; border-radius: 6px; }
                .ride-table th { background: %s; color: #ffffff; font-family: Montserrat, sans-serif; font-size: 8.6px; font-weight: 700; text-align: left; text-transform: uppercase; padding: 7px 8px; border: 0; }
                .ride-table td { color: #334155; padding: 7px 8px; border: 0; border-bottom: 1px solid #e2e8f0; vertical-align: top; overflow-wrap: anywhere; }
                .ride-table td strong { color: #111827; }
                .ride-table--legal th { font-size: 7.1px; padding: 5px; }
                .ride-table--legal td { font-size: 8px; padding: 5px; }
                .number { text-align: right; }
                .ride-bottom { display: table; width: 100%%; table-layout: fixed; margin-top: 8mm; }
                .ride-additional { display: table-cell; color: #505b69; border-top: 1px solid #eceff3; padding-top: 4mm; padding-right: 9mm; vertical-align: bottom; }
                .ride-additional p { margin: 4px 0; }
                .ride-totals { display: table-cell; width: 70mm; border: 1px solid #e6e9ee; border-radius: 8px; vertical-align: bottom; }
                .ride-totals table { width: 100%%; border-collapse: collapse; margin: 0; }
                .ride-totals td, .ride-totals th { padding: 7px 10px; border: 0; border-bottom: 1px solid #eef1f4; background: #ffffff; }
                .ride-totals td { color: #5b6573; }
                .ride-totals th { color: #141922; text-align: right; font-weight: 700; }
                .ride-totals__tax td, .ride-totals__tax th { color: %s; font-weight: 700; }
                .total-box td, .total-box th, .ride-totals__grand td, .ride-totals__grand th { background: %s; color: #ffffff; border-bottom: 0; }
                .total-box td, .ride-totals__grand td { font-size: 9.4px; font-weight: 700; text-transform: uppercase; }
                .total-box th, .ride-totals__grand th { font-family: Montserrat, sans-serif; font-size: 22pt; line-height: 1; font-weight: 900; }
                .ride-footer-note { margin-top: 8mm; padding-top: 4mm; border-top: 1px solid #eceff3; color: #7c8795; font-size: 9.5px; text-align: center; }
                """.formatted(
                fontFamily,
                headerColor,
                headerColor,
                headerColor,
                headerColor,
                headerColor,
                headerColor,
                borderColor,
                headerColor,
                headerColor,
                headerColor,
                accentColor,
                headerColor);
    }

    private void registerPdfFonts(PdfRendererBuilder builder) {
        builder.useFont(fontSupplier("Montserrat-Regular.ttf"), "Montserrat", 400,
                BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(fontSupplier("Montserrat-Bold.ttf"), "Montserrat", 700,
                BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(fontSupplier("Inter-Regular.ttf"), "Inter", 400,
                BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(fontSupplier("Roboto-Regular.ttf"), "Roboto", 400,
                BaseRendererBuilder.FontStyle.NORMAL, true);
    }

    private FSSupplier<InputStream> fontSupplier(String fileName) {
        return () -> {
            InputStream stream = SriRideService.class.getResourceAsStream("/fonts/" + fileName);
            if (stream == null) {
                throw new BadRequestException("No existe la fuente PDF requerida: " + fileName);
            }
            return stream;
        };
    }

    private String defaultBodyTemplate() {
        return """
                <section class="ride-schema-grid">
                  <article class="ride-schema-card">
                    <div class="ride-schema-title"><span>infoTributaria</span><strong>Datos del emisor</strong></div>
                    <table class="ride-schema-table"><tbody>
                      <tr><td>ambiente</td><th>{{legal.ambiente}}</th></tr>
                      <tr><td>tipoEmision</td><th>{{legal.tipoEmision}}</th></tr>
                      <tr><td>razonSocial</td><th>{{legal.razonSocial}}</th></tr>
                      <tr><td>nombreComercial</td><th>{{legal.nombreComercial}}</th></tr>
                      <tr><td>ruc</td><th>{{legal.ruc}}</th></tr>
                      <tr><td>codDoc</td><th>{{documento.codDoc}}</th></tr>
                      <tr><td>estab / ptoEmi</td><th>{{documento.estab}}-{{documento.ptoEmi}}</th></tr>
                      <tr><td>secuencial</td><th>{{documento.secuencial}}</th></tr>
                      <tr><td>dirMatriz</td><th>{{legal.dirMatriz}}</th></tr>
                    </tbody></table>
                  </article>
                  <article class="ride-schema-card">
                    <div class="ride-schema-title"><span>infoFactura</span><strong>Cliente y totales</strong></div>
                    <table class="ride-schema-table"><tbody>
                      <tr><td>fechaEmision</td><th>{{documento.fechaEmision}}</th></tr>
                      <tr><td>dirEstablecimiento</td><th>{{legal.dirEstablecimiento}}</th></tr>
                      <tr><td>contribuyenteEspecial</td><th>{{legal.contribuyenteEspecial}}</th></tr>
                      <tr><td>obligadoContabilidad</td><th>{{legal.obligadoContabilidad}}</th></tr>
                      <tr><td>tipoIdentificacionComprador</td><th>{{cliente.tipoIdentificacion}}</th></tr>
                      <tr><td>razonSocialComprador</td><th>{{cliente.nombre}}</th></tr>
                      <tr><td>identificacionComprador</td><th>{{cliente.identificacion}}</th></tr>
                      <tr><td>importeTotal</td><th>{{totales.total}}</th></tr>
                    </tbody></table>
                  </article>
                </section>

                <section class="ride-section">
                  <div class="ride-schema-title"><span>detalles</span><strong>Productos / servicios con impuesto por linea</strong></div>
                  <table class="ride-table ride-table--legal">
                    <thead><tr><th>Cod. principal</th><th>Cod. auxiliar</th><th>Descripcion</th><th class="number">Cant.</th><th class="number">Unitario</th><th class="number">Desc.</th><th class="number">Total sin impuesto</th><th>IVA cod.</th><th class="number">Base / valor</th></tr></thead>
                    <tbody>
                      {{#each detalles}}
                      <tr><td>{{codigo}}</td><td>{{codigoAuxiliar}}</td><td><strong>{{descripcion}}</strong></td><td class="number">{{cantidad}}</td><td class="number">{{precioUnitario}}</td><td class="number">{{descuento}}</td><td class="number">{{subtotal}}</td><td>{{ivaCodigo}}/{{ivaCodigoPorcentaje}} - {{ivaTarifa}}%</td><td class="number">{{ivaBase}} / {{ivaValor}}</td></tr>
                      {{/each}}
                    </tbody>
                  </table>
                </section>

                <section class="ride-compliance-grid">
                  <article class="ride-schema-card">
                    <div class="ride-schema-title"><span>totalConImpuestos</span><strong>Resumen IVA</strong></div>
                    <table class="ride-schema-table"><tbody><tr><td>codigo</td><th>2</th></tr><tr><td>codigoPorcentaje</td><th>4</th></tr><tr><td>baseImponible</td><th>{{totales.subtotal}}</th></tr><tr><td>tarifa</td><th>15.00</th></tr><tr><td>valor</td><th>{{totales.iva15}}</th></tr></tbody></table>
                  </article>
                  <article class="ride-schema-card">
                    <div class="ride-schema-title"><span>pagos</span><strong>Forma de pago SRI</strong></div>
                    {{#each totales.pagos}}
                    <table class="ride-schema-table"><tbody><tr><td>formaPago</td><th>{{formaPago}}</th></tr><tr><td>total</td><th>{{total}}</th></tr><tr><td>plazo</td><th>{{plazo}}</th></tr><tr><td>unidadTiempo</td><th>{{unidadTiempo}}</th></tr></tbody></table>
                    {{/each}}
                  </article>
                  <aside class="ride-totals">
                    <table><tbody><tr><td>totalSinImpuestos</td><th>{{totales.subtotal}}</th></tr><tr><td>totalDescuento</td><th>{{totales.descuento}}</th></tr><tr class="ride-totals__tax"><td>IVA 15% codigo 4</td><th>{{totales.iva15}}</th></tr><tr><td>propina</td><th>{{totales.propina}}</th></tr><tr class="total-box ride-totals__grand"><td>Total a pagar</td><th>{{totales.total}}</th></tr></tbody></table>
                  </aside>
                </section>

                <section class="ride-bottom">
                  <div class="ride-additional"><span class="ride-label">infoAdicional</span>{{#each infoAdicional}}<p><strong>{{nombre}}:</strong> {{valor}}</p>{{/each}}</div>
                </section>
                """;
    }

    private List<Map<String, Object>> parseDetails(Element root) {
        Element detallesElement = child(root, "detalles");
        if (detallesElement == null) {
            return List.of();
        }
        List<Map<String, Object>> detalles = new ArrayList<>();
        for (Element detalle : children(detallesElement, "detalle")) {
            BigDecimal subtotal = decimal(childText(detalle, "precioTotalSinImpuesto"));
            List<Map<String, Object>> taxes = parseLineTaxes(detalle);
            Map<String, Object> primaryTax = taxes.isEmpty()
                    ? mapOf("codigo", "2", "codigoPorcentaje", "4", "tarifa", "15.00", "baseImponible", money(subtotal), "valor", "0.00")
                    : taxes.get(0);
            detalles.add(mapOf(
                    "codigo", firstNonBlank(childText(detalle, "codigoPrincipal"), childText(detalle, "codigoAuxiliar")),
                    "codigoAuxiliar", firstNonBlank(childText(detalle, "codigoAuxiliar"), ""),
                    "descripcion", childText(detalle, "descripcion"),
                    "cantidad", decimalText(childText(detalle, "cantidad")),
                    "precioUnitario", money(decimal(childText(detalle, "precioUnitario"))),
                    "descuento", money(decimal(childText(detalle, "descuento"))),
                    "subtotal", money(subtotal),
                    "subtotalNumber", subtotal,
                    "ivaCodigo", string(primaryTax.get("codigo"), "2"),
                    "ivaCodigoPorcentaje", string(primaryTax.get("codigoPorcentaje"), "4"),
                    "ivaTarifa", string(primaryTax.get("tarifa"), "15.00"),
                    "ivaBase", string(primaryTax.get("baseImponible"), money(subtotal)),
                    "ivaValor", string(primaryTax.get("valor"), "0.00"),
                    "impuestos", taxes
            ));
        }
        return detalles;
    }

    private List<Map<String, Object>> parseLineTaxes(Element detalle) {
        Element impuestosElement = child(detalle, "impuestos");
        if (impuestosElement == null) {
            return List.of();
        }
        List<Map<String, Object>> taxes = new ArrayList<>();
        for (Element impuesto : children(impuestosElement, "impuesto")) {
            taxes.add(buildTaxMap(impuesto));
        }
        return taxes;
    }

    private List<Map<String, Object>> parseTotalTaxes(Element infoFactura, List<Map<String, Object>> detalles) {
        Element totalConImpuestos = child(infoFactura, "totalConImpuestos");
        if (totalConImpuestos != null) {
            List<Map<String, Object>> taxes = new ArrayList<>();
            for (Element totalImpuesto : children(totalConImpuestos, "totalImpuesto")) {
                taxes.add(buildTaxMap(totalImpuesto));
            }
            if (!taxes.isEmpty()) {
                return taxes;
            }
        }

        Map<String, TaxAggregate> aggregates = new LinkedHashMap<>();
        for (Map<String, Object> detalle : detalles) {
            for (Object rawTax : listValue(detalle.get("impuestos"))) {
                if (!(rawTax instanceof Map<?, ?> taxMap)) {
                    continue;
                }
                String code = string(taxMap.get("codigo"), "2");
                String percentageCode = string(taxMap.get("codigoPorcentaje"), "0");
                String key = code + "|" + percentageCode;
                TaxAggregate aggregate = aggregates.computeIfAbsent(key, ignored -> new TaxAggregate(code, percentageCode));
                aggregate.tarifa = decimal(string(taxMap.get("tarifa"), "0"));
                aggregate.base = aggregate.base.add(decimal(string(taxMap.get("baseImponible"), "0")));
                aggregate.value = aggregate.value.add(decimal(string(taxMap.get("valor"), "0")));
            }
        }
        return aggregates.values().stream()
                .map(this::buildTaxMap)
                .toList();
    }

    private Map<String, Object> buildTaxMap(Element impuesto) {
        String code = firstNonBlank(childText(impuesto, "codigo"), "2");
        String percentageCode = firstNonBlank(childText(impuesto, "codigoPorcentaje"), "0");
        BigDecimal tarifa = decimal(childText(impuesto, "tarifa"));
        BigDecimal base = decimal(firstNonBlank(childText(impuesto, "baseImponible"), childText(impuesto, "baseNoGraIva")));
        BigDecimal value = decimal(childText(impuesto, "valor"));
        return mapOf(
                "codigo", code,
                "codigoPorcentaje", percentageCode,
                "tarifa", money(tarifa),
                "baseImponible", money(base),
                "valor", money(value),
                "label", taxLabel(code, percentageCode, tarifa)
        );
    }

    private Map<String, Object> buildTaxMap(TaxAggregate aggregate) {
        return mapOf(
                "codigo", aggregate.code,
                "codigoPorcentaje", aggregate.percentageCode,
                "tarifa", money(aggregate.tarifa),
                "baseImponible", money(aggregate.base),
                "valor", money(aggregate.value),
                "label", taxLabel(aggregate.code, aggregate.percentageCode, aggregate.tarifa)
        );
    }

    private Totals calculateTotals(Element infoFactura, List<Map<String, Object>> detalles, List<Map<String, Object>> impuestos) {
        BigDecimal subtotalFromDetails = detalles.stream()
                .map(detail -> detail.get("subtotalNumber"))
                .filter(BigDecimal.class::isInstance)
                .map(BigDecimal.class::cast)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal subtotal = subtotalFromDetails.signum() > 0
                ? subtotalFromDetails
                : decimal(childText(infoFactura, "totalSinImpuestos"));
        BigDecimal discount = decimal(childText(infoFactura, "totalDescuento"));
        BigDecimal taxTotal = impuestos.stream()
                .map(tax -> decimal(string(tax.get("valor"), "0")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal iva15 = impuestos.stream()
                .filter(tax -> "2".equals(string(tax.get("codigo"), null)) && "4".equals(string(tax.get("codigoPorcentaje"), null)))
                .map(tax -> decimal(string(tax.get("valor"), "0")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal calculatedTotal = subtotal.add(taxTotal).subtract(discount);
        BigDecimal declaredTotal = decimal(childText(infoFactura, "importeTotal"));
        BigDecimal finalTotal = declaredTotal.signum() > 0 ? declaredTotal : calculatedTotal;
        return new Totals(
                amount(subtotal),
                amount(discount),
                amount(iva15),
                amount(taxTotal),
                amount(finalTotal),
                amount(finalTotal.subtract(calculatedTotal).abs())
        );
    }

    private List<Map<String, Object>> parseAdditionalInfo(Element root) {
        Element infoAdicional = child(root, "infoAdicional");
        if (infoAdicional == null) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Element campo : children(infoAdicional, "campoAdicional")) {
            result.add(mapOf(
                    "nombre", firstNonBlank(campo.getAttribute("nombre"), "Dato adicional"),
                    "valor", text(campo)
            ));
        }
        return result;
    }

    private String additionalInfoValue(Element root, String name) {
        Element infoAdicional = child(root, "infoAdicional");
        if (infoAdicional == null || isBlank(name)) {
            return null;
        }
        for (Element campo : children(infoAdicional, "campoAdicional")) {
            if (name.equalsIgnoreCase(campo.getAttribute("nombre"))) {
                return text(campo);
            }
        }
        return null;
    }

    private List<Map<String, Object>> parsePayments(Element infoFactura, BigDecimal total) {
        Element pagosElement = child(infoFactura, "pagos");
        if (pagosElement == null) {
            return List.of(mapOf(
                    "formaPago", "20 - Transferencia, deposito u otro pago bancario",
                    "total", money(total),
                    "plazo", "0",
                    "unidadTiempo", "dias"
            ));
        }
        List<Map<String, Object>> payments = new ArrayList<>();
        for (Element pago : children(pagosElement, "pago")) {
            payments.add(mapOf(
                    "formaPago", firstNonBlank(childText(pago, "formaPago"), "20"),
                    "total", money(decimal(firstNonBlank(childText(pago, "total"), money(total)))),
                    "plazo", firstNonBlank(childText(pago, "plazo"), "0"),
                    "unidadTiempo", firstNonBlank(childText(pago, "unidadTiempo"), "dias")
            ));
        }
        return payments.isEmpty() ? List.of(mapOf(
                "formaPago", "20 - Transferencia, deposito u otro pago bancario",
                "total", money(total),
                "plazo", "0",
                "unidadTiempo", "dias"
        )) : payments;
    }

    private Element parseFacturaRoot(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            org.w3c.dom.Document parsed = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            Element root = parsed.getDocumentElement();
            if ("autorizacion".equals(localName(root))) {
                Element comprobante = firstDescendant(root, "comprobante");
                if (comprobante != null && !isBlank(text(comprobante))) {
                    return parseFacturaRoot(text(comprobante));
                }
            }
            if (!"factura".equals(localName(root))) {
                throw new BadRequestException("El motor RIDE dinamico solo soporta facturas SRI 1.1.0");
            }
            return root;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("XML SRI invalido para generar RIDE: " + ex.getMessage());
        }
    }

    private String resolveXmlPayload(TaxDocumentEntity document) {
        String xml = document.getXmlPayload();
        if (isBlank(xml)) {
            xml = document.getSignedXmlPayload();
        }
        if (isBlank(xml)) {
            throw new BadRequestException("Valida el documento antes de generar el RIDE");
        }
        return xml;
    }

    private SriConfigurationEntity loadConfiguration(Long tenantId) {
        return sriConfigurationRepository.findByTenantId(tenantId).orElse(null);
    }

    private RideTemplateEntity resolveTemplate(Long tenantId) {
        return templateCache.computeIfAbsent(tenantId, this::loadTemplate);
    }

    private RideTemplateEntity loadTemplate(Long tenantId) {
        return rideTemplateRepository.findFirstByTenantIdAndActiveTrueOrderByCustomDescUpdatedAtDesc(tenantId)
                .orElseGet(() -> {
                    RideTemplateEntity entity = new RideTemplateEntity();
                    entity.setTenantId(tenantId);
                    entity.setTemplateCode(DEFAULT_TEMPLATE_CODE);
                    entity.setTemplateName("InsightVision RIDE");
                    entity.setLayoutJson(serializeLayout(defaultLayout()));
                    entity.setHtmlContent(defaultBodyTemplate());
                    entity.setCustom(false);
                    entity.setActive(true);
                    return entity;
                });
    }

    private RideTemplateView toView(RideTemplateEntity entity) {
        Map<String, Object> layout = resolveLayout(entity);
        return new RideTemplateView(
                entity.getId(),
                entity.getTemplateCode(),
                entity.getTemplateName(),
                layout,
                templateBody(entity),
                Boolean.TRUE.equals(entity.getCustom()),
                Boolean.TRUE.equals(entity.getActive()),
                entity.getUpdatedAt());
    }

    private String templateBody(RideTemplateEntity template) {
        String body = template.getHtmlContent();
        return body == null || body.isBlank() ? defaultBodyTemplate() : body;
    }

    private Map<String, Object> resolveLayout(RideTemplateEntity template) {
        if (template == null || template.getLayoutJson() == null || template.getLayoutJson().isBlank()) {
            return defaultLayout();
        }
        try {
            return mergeLayout(objectMapper.readValue(template.getLayoutJson(), MAP_TYPE));
        } catch (Exception ex) {
            return defaultLayout();
        }
    }

    private Map<String, Object> defaultLayout() {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("headerColor", "#123a5a");
        layout.put("accentColor", "#0f766e");
        layout.put("borderColor", "#d9e2ec");
        layout.put("fontFamily", "Inter, sans-serif");
        layout.put("logoUrl", null);
        layout.put("footerNote", "Documento generado por InsightVision");
        return layout;
    }

    private Map<String, Object> mergeLayout(Map<String, Object> customLayout) {
        Map<String, Object> layout = defaultLayout();
        if (customLayout == null) {
            return layout;
        }
        for (Map.Entry<String, Object> entry : customLayout.entrySet()) {
            if (entry.getKey() != null && layout.containsKey(entry.getKey())) {
                layout.put(entry.getKey(), entry.getValue());
            }
        }
        return layout;
    }

    private String serializeLayout(Map<String, Object> layout) {
        try {
            return objectMapper.writeValueAsString(mergeLayout(layout));
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo serializar layout RIDE");
        }
    }

    private void validateTemplateHtml(String html) {
        if (UNSAFE_TEMPLATE_PATTERN.matcher(html).find()) {
            throw new BadRequestException("La plantilla RIDE no puede contener scripts, iframes ni manejadores JavaScript");
        }
    }

    private String generateQrDataUri(String value) {
        if (isBlank(value)) {
            return "";
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(value, BarcodeFormat.QR_CODE, 220, 220);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            return "";
        }
    }

    private String renderLogo(Map<String, Object> layout) {
        String logoUrl = string(layout.get("logoUrl"), null);
        if (isBlank(logoUrl)) {
            return "<div class=\"ride-logo-box\">InsightVision</div>\n";
        }
        String safeLogo = logoUrl.trim();
        if (!(safeLogo.startsWith("https://") || safeLogo.startsWith("http://") || safeLogo.startsWith("data:image/"))) {
            return "<div class=\"ride-logo-box\">InsightVision</div>\n";
        }
        return "<div class=\"ride-logo-box\"><img src=\"" + escapeHtml(safeLogo) + "\" alt=\"Logo\" /></div>\n";
    }

    private List<String> buildLegends(boolean rimpe, boolean retentionAgent, String retentionResolution, boolean iva15) {
        List<String> legends = new ArrayList<>();
        if (rimpe) {
            legends.add(rimpeText());
        }
        if (retentionAgent) {
            legends.add(retentionAgentText(retentionResolution));
        }
        if (iva15) {
            legends.add("Tarifa 15% (Codigo 4)");
        }
        return legends;
    }

    private boolean hasIva15(List<Map<String, Object>> impuestos, List<Map<String, Object>> detalles) {
        for (Map<String, Object> tax : impuestos) {
            if ("2".equals(string(tax.get("codigo"), null)) && "4".equals(string(tax.get("codigoPorcentaje"), null))) {
                return true;
            }
        }
        for (Map<String, Object> detail : detalles) {
            for (Object rawTax : listValue(detail.get("impuestos"))) {
                if (rawTax instanceof Map<?, ?> tax
                        && "2".equals(string(tax.get("codigo"), null))
                        && "4".equals(string(tax.get("codigoPorcentaje"), null))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String taxLabel(String code, String percentageCode, BigDecimal tarifa) {
        if (!"2".equals(code)) {
            return "Impuesto " + code;
        }
        return switch (percentageCode) {
            case "4" -> "Tarifa 15%";
            case "5" -> "Tarifa 5%";
            case "0" -> "IVA 0%";
            case "6" -> "No objeto de impuesto";
            case "7" -> "Exento de IVA";
            default -> tarifa != null && tarifa.signum() > 0 ? "IVA " + money(tarifa) + "%" : "IVA";
        };
    }

    private String resolveDocumentLabel(String code) {
        return switch (code == null ? "" : code) {
            case "01" -> "Factura";
            case "04" -> "Nota de credito";
            default -> "Comprobante SRI";
        };
    }

    private String formatDocumentNumber(Element infoTributaria, TaxDocumentEntity document) {
        return firstNonBlank(childText(infoTributaria, "estab"), document.getEstablishmentCode())
                + "-" + firstNonBlank(childText(infoTributaria, "ptoEmi"), document.getEmissionPointCode())
                + "-" + firstNonBlank(childText(infoTributaria, "secuencial"), document.getSequentialNumber());
    }

    private Element requireDescendant(Element parent, String name) {
        Element child = firstDescendant(parent, name);
        if (child == null) {
            throw new BadRequestException("El XML SRI no contiene el bloque obligatorio " + name);
        }
        return child;
    }

    private Element firstDescendant(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        if (name.equals(localName(parent))) {
            return parent;
        }
        NodeList nodes = parent.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && name.equals(localName(element))) {
                return element;
            }
        }
        return null;
    }

    private Element child(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && name.equals(localName(element))) {
                return element;
            }
        }
        return null;
    }

    private List<Element> children(Element parent, String name) {
        if (parent == null) {
            return List.of();
        }
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && name.equals(localName(element))) {
                result.add(element);
            }
        }
        return result;
    }

    private String childText(Element parent, String name) {
        return text(child(parent, name));
    }

    private String text(Element element) {
        return element == null ? null : element.getTextContent().trim();
    }

    private String localName(Node node) {
        if (node == null) {
            return "";
        }
        String local = node.getLocalName();
        if (local != null) {
            return local;
        }
        String name = node.getNodeName();
        int separator = name.indexOf(':');
        return separator >= 0 ? name.substring(separator + 1) : name;
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length - 1; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private List<?> listValue(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private String stringValue(Map<String, Object> parent, String key, String nestedKey) {
        return value(nestedMap(parent, key), nestedKey);
    }

    private String value(Map<String, Object> map, String key) {
        return string(map.get(key), "");
    }

    private String string(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private BigDecimal decimal(String value) {
        if (isBlank(value)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(value.trim().replace(",", ".")).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal amount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value) {
        return amount(value).toPlainString();
    }

    private String decimalText(String value) {
        if (isBlank(value)) {
            return "0.00";
        }
        return decimal(value).toPlainString();
    }

    private String cssColor(Object raw, String fallback) {
        String color = string(raw, fallback).trim();
        if (color.matches("^#[0-9a-fA-F]{6}$") || color.matches("^#[0-9a-fA-F]{3}$")) {
            return color;
        }
        return fallback;
    }

    private String cssFont(Object raw, String fallback) {
        String font = string(raw, fallback).trim();
        if (font.length() > 80 || !font.matches("[A-Za-z0-9 ,._\\-]+")) {
            return fallback;
        }
        return font;
    }

    private String normalizeTemplateCode(String value) {
        String normalized = value == null || value.isBlank() ? DEFAULT_TEMPLATE_CODE : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_-]{1,60}")) {
            throw new BadRequestException("Codigo de plantilla RIDE invalido");
        }
        return normalized;
    }

    private String leftPad(String value, int length) {
        String digits = digitsOnly(value);
        if (digits.length() >= length) {
            return digits.substring(0, length);
        }
        return "0".repeat(length - digits.length()) + digits;
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String rimpeText() {
        return "CONTRIBUYENTE R\u00c9GIMEN RIMPE";
    }

    private String retentionAgentText(String resolution) {
        return "Agente de Retencion Resolucion No. " + firstNonBlank(resolution, "1");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record RideRenderResult(
            Long documentId,
            String accessKey,
            String html) {
    }

    public record RidePdfResult(
            Long documentId,
            String accessKey,
            String fileName,
            byte[] content) {
    }

    public record RideTemplateView(
            Long id,
            String templateCode,
            String templateName,
            Map<String, Object> layout,
            String htmlContent,
            Boolean custom,
            Boolean active,
            LocalDateTime updatedAt) {
    }

    public record UpsertRideTemplateCommand(
            String templateCode,
            String templateName,
            Map<String, Object> layout,
            String htmlContent,
            Boolean active) {
    }

    private record Totals(
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal iva15,
            BigDecimal tax,
            BigDecimal total,
            BigDecimal difference) {
    }

    private static class TaxAggregate {
        private final String code;
        private final String percentageCode;
        private BigDecimal tarifa = BigDecimal.ZERO;
        private BigDecimal base = BigDecimal.ZERO;
        private BigDecimal value = BigDecimal.ZERO;

        private TaxAggregate(String code, String percentageCode) {
            this.code = code;
            this.percentageCode = percentageCode;
        }
    }
}
