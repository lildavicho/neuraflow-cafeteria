package com.ucacue.bar.dto.ml;

import java.math.BigDecimal;

public record MlProductInsightDTO(
        Long id,
        String code,
        String name,
        String categoryName,
        String imageUrl,
        BigDecimal price,
        Long observedUnits,
        Double confidenceScore) {
}
