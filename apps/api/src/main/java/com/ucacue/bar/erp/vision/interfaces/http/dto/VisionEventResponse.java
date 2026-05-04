package com.ucacue.bar.erp.vision.interfaces.http.dto;

import java.time.OffsetDateTime;

public record VisionEventResponse(
        boolean success,
        String code,
        String eventId,
        boolean tenantResolved,
        String cameraCode,
        String locationCode,
        int receivedDetections,
        int storedDetections,
        boolean duplicate,
        boolean statisticsUpdated,
        OffsetDateTime processedAt,
        String message) {
}
