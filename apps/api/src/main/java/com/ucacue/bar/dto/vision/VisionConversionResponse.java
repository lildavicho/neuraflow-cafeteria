package com.ucacue.bar.dto.vision;

import java.math.BigDecimal;

public record VisionConversionResponse(
        long people,
        long sales,
        BigDecimal revenue,
        BigDecimal revenuePerPerson,
        Double salesConversionRate) {
}
