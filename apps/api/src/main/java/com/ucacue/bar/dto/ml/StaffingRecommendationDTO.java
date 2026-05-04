package com.ucacue.bar.dto.ml;

import java.time.LocalDateTime;

public record StaffingRecommendationDTO(
        LocalDateTime datetime,
        Long predictedOrders,
        Integer recommendedStaff) {
}
