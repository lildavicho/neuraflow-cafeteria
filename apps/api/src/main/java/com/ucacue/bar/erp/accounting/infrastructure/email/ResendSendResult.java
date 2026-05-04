package com.ucacue.bar.erp.accounting.infrastructure.email;

import java.time.Instant;

public record ResendSendResult(
        boolean success,
        String provider,
        String messageId,
        int statusCode,
        String errorCode,
        String errorMessage,
        boolean retryable,
        Instant sentAt,
        String rawResponse) {

    public static ResendSendResult disabled(String code, String message) {
        return new ResendSendResult(false, "RESEND", null, 0, code, message, false, null, null);
    }
}
