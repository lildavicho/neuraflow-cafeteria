package com.ucacue.bar.dto.vision;

import java.time.LocalDateTime;

public record VisionPeakHourResponse(
        LocalDateTime hourStart,
        long people,
        long sales,
        Double salesConversionRate) {
}
