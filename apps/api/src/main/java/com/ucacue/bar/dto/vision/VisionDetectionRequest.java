package com.ucacue.bar.dto.vision;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record VisionDetectionRequest(
        @NotNull
        Long cameraId,

        @Size(max = 120)
        String eventKey,

        LocalDateTime timestamp,

        @Min(0)
        Integer peopleCount,

        @Min(0)
        Integer uniquePeople,

        @Size(max = 30)
        String eventType,

        @Size(max = 40)
        String source,

        List<@Size(max = 64) String> trackIds) {
}
