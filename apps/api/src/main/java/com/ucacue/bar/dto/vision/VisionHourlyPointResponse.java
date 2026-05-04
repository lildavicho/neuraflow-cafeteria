package com.ucacue.bar.dto.vision;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VisionHourlyPointResponse(
        LocalDateTime hourStart,
        long people,
        long sales,
        BigDecimal revenue,
        Double salesConversionRate) {
}
