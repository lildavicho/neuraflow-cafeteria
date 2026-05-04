package com.ucacue.bar.erp.accounting.infrastructure.sri;

import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
@RequiredArgsConstructor
public class SriSoapClient {

    private final SriProperties sriProperties;

    public ReceiptResponse submitReceipt(String endpointUrl, String signedXml) {
        String envelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ec="http://ec.gob.sri.ws.recepcion">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <ec:validarComprobante>
                      <xml>%s</xml>
                    </ec:validarComprobante>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(Base64.getEncoder().encodeToString(signedXml.getBytes(StandardCharsets.UTF_8)));

        TransportResult transport = post(endpointUrl, envelope);
        if (transport.transportError()) {
            return new ReceiptResponse(endpointUrl, envelope, transport.responseBody(), transport.httpStatus(), false,
                    true, "NETWORK_ERROR", transport.providerMessage(), List.of());
        }
        if (!transport.isSuccessfulHttp()) {
            return new ReceiptResponse(endpointUrl, envelope, transport.responseBody(), transport.httpStatus(), false,
                    false, "HTTP_ERROR", transport.providerMessage(), List.of());
        }

        try {
            Document document = parse(transport.responseBody());
            String status = firstText(document.getDocumentElement(), "estado");
            List<SoapMessage> messages = extractMessages(document);
            String providerMessage = renderMessages(messages, status);
            return new ReceiptResponse(endpointUrl, envelope, transport.responseBody(), transport.httpStatus(), true,
                    false, status != null ? status : "UNKNOWN", providerMessage, messages);
        } catch (IllegalStateException ex) {
            return new ReceiptResponse(endpointUrl, envelope, transport.responseBody(), transport.httpStatus(), false,
                    false, "UNPARSEABLE_RESPONSE", ex.getMessage(), List.of());
        }
    }

    public AuthorizationResponse queryAuthorization(String endpointUrl, String accessKey) {
        String envelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ec="http://ec.gob.sri.ws.autorizacion">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <ec:autorizacionComprobante>
                      <claveAccesoComprobante>%s</claveAccesoComprobante>
                    </ec:autorizacionComprobante>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(escapeXml(accessKey));

        TransportResult transport = post(endpointUrl, envelope);
        if (transport.transportError()) {
            return new AuthorizationResponse(endpointUrl, envelope, transport.responseBody(), transport.httpStatus(),
                    false, true, "NETWORK_ERROR", transport.providerMessage(), null, null, null, List.of());
        }
        if (!transport.isSuccessfulHttp()) {
            return new AuthorizationResponse(endpointUrl, envelope, transport.responseBody(), transport.httpStatus(),
                    false, false, "HTTP_ERROR", transport.providerMessage(), null, null, null, List.of());
        }

        try {
            Document document = parse(transport.responseBody());
            Element authorization = firstElement(document.getDocumentElement(), "autorizacion");
            if (authorization == null) {
                return new AuthorizationResponse(endpointUrl, envelope, transport.responseBody(), transport.httpStatus(),
                        true, false, "PROCESSING", "El SRI aun no retorna una autorizacion para la clave consultada.",
                        null, null, null, List.of());
            }

            String status = firstText(authorization, "estado");
            String authorizationNumber = firstText(authorization, "numeroAutorizacion");
            String authorizedXml = firstText(authorization, "comprobante");
            LocalDateTime authorizedAt = parseDateTime(firstText(authorization, "fechaAutorizacion"));
            List<SoapMessage> messages = extractMessages(authorization);
            String providerMessage = renderMessages(messages, status);

            return new AuthorizationResponse(endpointUrl, envelope, transport.responseBody(), transport.httpStatus(), true,
                    false, status != null ? status : "UNKNOWN", providerMessage, authorizationNumber, authorizedAt,
                    authorizedXml, messages);
        } catch (IllegalStateException ex) {
            return new AuthorizationResponse(endpointUrl, envelope, transport.responseBody(), transport.httpStatus(),
                    false, false, "UNPARSEABLE_RESPONSE", ex.getMessage(), null, null, null, List.of());
        }
    }

    private TransportResult post(String endpointUrl, String envelope) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(sriProperties.getConnectTimeoutMs()))
                    .build();

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpointUrl))
                    .timeout(Duration.ofMillis(sriProperties.getReadTimeoutMs()))
                    .header("Content-Type", "text/xml; charset=UTF-8")
                    .header("Accept", "text/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String providerMessage = response.statusCode() >= 200 && response.statusCode() < 300
                    ? "OK"
                    : "El WS del SRI respondio con HTTP " + response.statusCode();
            return new TransportResult(response.statusCode(), response.body(), false, providerMessage);
        } catch (Exception ex) {
            return new TransportResult(null, null, true, ex.getMessage());
        }
    }

    private Document parse(String body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo interpretar la respuesta XML del SRI", ex);
        }
    }

    private List<SoapMessage> extractMessages(Document document) {
        return extractMessages(document.getDocumentElement());
    }

    private List<SoapMessage> extractMessages(Element root) {
        List<SoapMessage> messages = new ArrayList<>();
        NodeList nodes = root.getElementsByTagNameNS("*", "mensaje");
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node instanceof Element element) {
                messages.add(new SoapMessage(
                        firstText(element, "identificador"),
                        normalize(firstText(element, "mensaje")),
                        normalize(firstText(element, "informacionAdicional")),
                        normalize(firstText(element, "tipo"))));
            }
        }
        return messages;
    }

    private Element firstElement(Element root, String localName) {
        NodeList nodes = root.getElementsByTagNameNS("*", localName);
        for (int index = 0; index < nodes.getLength(); index++) {
            if (nodes.item(index) instanceof Element element) {
                return element;
            }
        }
        return null;
    }

    private String firstText(Element root, String localName) {
        Element element = firstElement(root, localName);
        return element != null ? normalize(element.getTextContent()) : null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim()).toLocalDateTime();
        } catch (Exception ex) {
            return null;
        }
    }

    private String renderMessages(List<SoapMessage> messages, String fallback) {
        if (messages == null || messages.isEmpty()) {
            return fallback != null ? fallback : "Sin detalle adicional";
        }
        return messages.stream()
                .map(message -> {
                    StringBuilder builder = new StringBuilder();
                    if (message.identifier() != null && !message.identifier().isBlank()) {
                        builder.append('[').append(message.identifier()).append("] ");
                    }
                    if (message.message() != null) {
                        builder.append(message.message());
                    }
                    if (message.additionalInfo() != null && !message.additionalInfo().isBlank()) {
                        builder.append(" - ").append(message.additionalInfo());
                    }
                    return builder.toString();
                })
                .filter(item -> !item.isBlank())
                .reduce((left, right) -> left + " | " + right)
                .orElse(fallback != null ? fallback : "Sin detalle adicional");
    }

    private String normalize(String value) {
        return value != null ? value.trim() : null;
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record TransportResult(
            Integer httpStatus,
            String responseBody,
            boolean transportError,
            String providerMessage) {

        private boolean isSuccessfulHttp() {
            return httpStatus != null && httpStatus >= 200 && httpStatus < 300;
        }
    }

    public record SoapMessage(
            String identifier,
            String message,
            String additionalInfo,
            String type) {
    }

    public record ReceiptResponse(
            String endpointUrl,
            String requestPayload,
            String responsePayload,
            Integer httpStatus,
            boolean success,
            boolean transportError,
            String providerStatus,
            String providerMessage,
            List<SoapMessage> messages) {
    }

    public record AuthorizationResponse(
            String endpointUrl,
            String requestPayload,
            String responsePayload,
            Integer httpStatus,
            boolean success,
            boolean transportError,
            String providerStatus,
            String providerMessage,
            String authorizationNumber,
            LocalDateTime authorizedAt,
            String authorizedXml,
            List<SoapMessage> messages) {
    }
}
