package com.ucacue.bar.dto.vision;

import java.time.LocalDateTime;

public record VisionEventIngestResponse(
        boolean created,
        Long eventId,
        Long cameraId,
        String cameraName,
        String eventKey,
        LocalDateTime timestamp,
        LocalDateTime hourStart,
        long hourUniquePeople) {
}
