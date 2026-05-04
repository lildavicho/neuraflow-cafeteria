package com.ucacue.bar.erp.vision.interfaces.http.dto;

import java.time.OffsetDateTime;

public record VisionRecentEventResponse(
        String eventId,
        String cameraCode,
        String cameraName,
        String locationCode,
        OffsetDateTime eventTimestamp,
        String frameId,
        int totalDetections,
        String status,
        OffsetDateTime receivedAt) {
}
