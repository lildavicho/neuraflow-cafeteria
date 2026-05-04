package com.ucacue.bar.erp.vision.interfaces.http.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record VisionStatisticsResponse(
        String period,
        String from,
        String to,
        Summary summary,
        List<LabelTotal> byLabel,
        List<CameraTotal> byCamera,
        List<TimelinePoint> timeline) {

    public record Summary(
            long totalEvents,
            long totalDetections,
            long activeCameras,
            String topLabel) {
    }

    public record LabelTotal(
            String label,
            long total) {
    }

    public record CameraTotal(
            String cameraCode,
            String cameraName,
            long totalDetections) {
    }

    public record TimelinePoint(
            OffsetDateTime periodStart,
            long totalDetections) {
    }
}
