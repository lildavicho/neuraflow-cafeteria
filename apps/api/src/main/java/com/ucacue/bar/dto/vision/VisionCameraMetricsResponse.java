package com.ucacue.bar.dto.vision;

import java.time.LocalDateTime;

public record VisionCameraMetricsResponse(
        Long cameraId,
        String cameraName,
        String location,
        Boolean enabled,
        long uniquePeople,
        long eventCount,
        LocalDateTime lastEventAt,
        long lastHourPeople) {
}
