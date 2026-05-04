package com.ucacue.bar.erp.vision.interfaces.http.dto;

import java.time.OffsetDateTime;

public record VisionErrorResponse(
        boolean success,
        String code,
        String message,
        OffsetDateTime processedAt) {

    public static VisionErrorResponse of(String code, String message) {
        return new VisionErrorResponse(false, code, message, OffsetDateTime.now());
    }
}
