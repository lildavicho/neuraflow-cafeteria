package com.ucacue.bar.dto.ml;

import java.util.List;

public record DemandForecastDTO(
        Long productId,
        String period,
        Integer daysAhead,
        List<Double> historical,
        List<Double> forecast,
        List<Double> lowerBound,
        List<Double> upperBound,
        Double accuracyScore) {
}
