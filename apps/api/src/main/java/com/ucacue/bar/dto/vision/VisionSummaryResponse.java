package com.ucacue.bar.dto.vision;

import java.math.BigDecimal;

public record VisionSummaryResponse(
        int registeredCameras,
        int activeCameras,
        long uniquePeople,
        long sales,
        BigDecimal revenue) {
}
