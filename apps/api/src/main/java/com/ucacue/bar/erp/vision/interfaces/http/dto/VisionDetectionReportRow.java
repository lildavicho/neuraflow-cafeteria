package com.ucacue.bar.erp.vision.interfaces.http.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record VisionDetectionReportRow(
        String eventId,
        OffsetDateTime eventTimestamp,
        String cameraCode,
        String cameraName,
        String locationCode,
        String label,
        BigDecimal confidence,
        BigDecimal bboxX,
        BigDecimal bboxY,
        BigDecimal bboxWidth,
        BigDecimal bboxHeight,
        String trackingId) {
}
