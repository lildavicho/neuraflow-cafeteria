package com.ucacue.bar.erp.accounting.infrastructure.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ResendEmailClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesMessageIdAndSendsReplyTo() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/emails", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"id\":\"resend-message-123\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        SriProperties properties = new SriProperties();
        properties.getRide().getResend().setEnabled(true);
        properties.getRide().getResend().setApiKey("test-key");
        properties.getRide().getResend().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.getRide().getResend().setFromEmail("soporte@insightvisionia.cloud");
        properties.getRide().getResend().setReplyTo("soporte@insightvisionia.cloud");

        ResendEmailClient client = new ResendEmailClient(properties, new ObjectMapper());
        ResendSendResult result = client.send(new ResendEmailClient.SendRequest(
                "cliente@example.com",
                "Factura",
                "<p>Hola</p>",
                "soporte@insightvisionia.cloud",
                "soporte@insightvisionia.cloud",
                "Insight Vision IA",
                List.of()));

        assertThat(result.success()).isTrue();
        assertThat(result.messageId()).isEqualTo("resend-message-123");
        assertThat(body.get()).contains("\"reply_to\":\"soporte@insightvisionia.cloud\"");
        assertThat(body.get()).doesNotContain("test-key");
    }
}
