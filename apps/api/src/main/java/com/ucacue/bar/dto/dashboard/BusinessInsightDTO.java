package com.ucacue.bar.dto.dashboard;

public record BusinessInsightDTO(
        String title,
        String explanation,
        String impact,
        String actionRecommended,
        String tone,
        String category) {
}
