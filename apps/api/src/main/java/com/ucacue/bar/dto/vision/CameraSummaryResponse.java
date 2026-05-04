package com.ucacue.bar.dto.vision;

import java.time.LocalDateTime;

public record CameraSummaryResponse(
        Long id,
        String name,
        String rtspUrl,
        String description,
        String location,
        Boolean enabled,
        String streamPath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastEventAt,
        long lastHourPeople) {
}
