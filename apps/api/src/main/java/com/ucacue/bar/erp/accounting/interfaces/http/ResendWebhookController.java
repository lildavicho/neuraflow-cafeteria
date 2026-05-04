package com.ucacue.bar.erp.accounting.interfaces.http;

import com.ucacue.bar.erp.accounting.application.ResendWebhookService;
import com.ucacue.bar.erp.accounting.application.ResendWebhookService.WebhookProcessResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/integrations/resend")
@RequiredArgsConstructor
@Tag(name = "Resend Webhook", description = "Webhook publico protegido por firma Svix/Resend")
public class ResendWebhookController {

    private final ResendWebhookService resendWebhookService;

    @PostMapping("/webhook")
    @Operation(summary = "Recibir webhook de Resend")
    public ResponseEntity<WebhookProcessResult> webhook(@RequestHeader HttpHeaders headers,
                                                        @RequestBody(required = false) String rawBody) {
        return ResponseEntity.ok(resendWebhookService.process(headers, rawBody == null ? "" : rawBody));
    }
}
