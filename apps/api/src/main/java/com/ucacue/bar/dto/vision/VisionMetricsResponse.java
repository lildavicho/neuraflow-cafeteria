package com.ucacue.bar.dto.vision;

import java.util.List;

public record VisionMetricsResponse(
        VisionRangeResponse range,
        VisionSummaryResponse summary,
        VisionConversionResponse conversion,
        List<VisionHourlyPointResponse> hourlySeries,
        List<VisionPeakHourResponse> peakHours,
        List<VisionCameraMetricsResponse> cameras) {
}
