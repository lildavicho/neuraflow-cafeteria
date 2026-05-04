package com.ucacue.bar.erp.vision.interfaces.http.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record VisionEventRequest(
        String cameraCode,
        String locationCode,
        OffsetDateTime timestamp,
        String frameId,
        String source,
        Model model,
        Image image,
        List<Detection> detections,
        Map<String, Object> metadata) {

    public record Model(
            String name,
            String version,
            BigDecimal confidenceThreshold) {
    }

    public record Image(
            Integer width,
            Integer height,
            String uri) {
    }

    public record Detection(
            String label,
            BigDecimal confidence,
            BBox bbox,
            String trackingId,
            Map<String, Object> metadata) {
    }

    public record BBox(
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height) {
    }
}
