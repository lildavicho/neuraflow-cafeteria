package com.ucacue.bar.erp.vision.application;

import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.BranchEntity;
import com.ucacue.bar.erp.platform.infrastructure.persistence.repository.BranchRepository;
import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionCameraEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.repository.VisionCameraRepository;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisionValidationService {

    private static final int MAX_DETECTIONS_PER_EVENT = 500;

    private final VisionCameraRepository visionCameraRepository;
    private final BranchRepository branchRepository;

    public ValidatedVisionEvent validate(Long tenantId, VisionEventRequest request) {
        validatePayload(request);

        VisionCameraEntity camera = visionCameraRepository
                .findByTenantIdAndCodeIgnoreCaseAndStatus(
                        tenantId,
                        request.cameraCode().trim(),
                        VisionCameraEntity.Status.ACTIVE)
                .orElseThrow(() -> new VisionApiException(
                        HttpStatus.NOT_FOUND,
                        "CAMERA_NOT_FOUND_FOR_TENANT",
                        "Camera does not exist or is not active for this tenant"));

        BranchEntity branch = null;
        if (!isBlank(request.locationCode())) {
            branch = branchRepository.findByTenantIdAndCodeIgnoreCase(tenantId, request.locationCode().trim())
                    .filter(item -> Boolean.TRUE.equals(item.getActive()))
                    .orElseThrow(() -> new VisionApiException(
                            HttpStatus.NOT_FOUND,
                            "LOCATION_NOT_FOUND_FOR_TENANT",
                            "Location does not exist or is not active for this tenant"));
            if (camera.getBranchId() != null && !camera.getBranchId().equals(branch.getId())) {
                throw new VisionApiException(
                        HttpStatus.FORBIDDEN,
                        "CAMERA_LOCATION_MISMATCH",
                        "Camera does not belong to the requested location for this tenant");
            }
        }

        return new ValidatedVisionEvent(camera, branch);
    }

    private void validatePayload(VisionEventRequest request) {
        if (request == null) {
            invalid("Request body is required");
        }
        if (isBlank(request.cameraCode())) {
            invalid("cameraCode is required");
        }
        if (request.timestamp() == null) {
            invalid("timestamp is required");
        }
        if (request.image() != null) {
            if (request.image().width() != null && request.image().width() <= 0) {
                invalid("image.width must be greater than zero");
            }
            if (request.image().height() != null && request.image().height() <= 0) {
                invalid("image.height must be greater than zero");
            }
        }

        List<VisionEventRequest.Detection> detections = request.detections();
        if (detections == null) {
            invalid("detections is required");
        }
        if (detections.size() > MAX_DETECTIONS_PER_EVENT) {
            invalid("detections exceeds maximum allowed size");
        }
        for (int i = 0; i < detections.size(); i++) {
            validateDetection(detections.get(i), i);
        }
    }

    private void validateDetection(VisionEventRequest.Detection detection, int index) {
        if (detection == null) {
            invalid("detections[" + index + "] is required");
        }
        if (isBlank(detection.label())) {
            invalid("detections[" + index + "].label is required");
        }
        if (detection.confidence() == null
                || detection.confidence().compareTo(BigDecimal.ZERO) < 0
                || detection.confidence().compareTo(BigDecimal.ONE) > 0) {
            invalid("detections[" + index + "].confidence must be between 0 and 1");
        }
        if (detection.bbox() == null) {
            invalid("detections[" + index + "].bbox is required");
        }
        if (detection.bbox().x() == null || detection.bbox().y() == null) {
            invalid("detections[" + index + "].bbox x/y are required");
        }
        if (detection.bbox().width() == null || detection.bbox().width().compareTo(BigDecimal.ZERO) <= 0) {
            invalid("detections[" + index + "].bbox.width must be greater than zero");
        }
        if (detection.bbox().height() == null || detection.bbox().height().compareTo(BigDecimal.ZERO) <= 0) {
            invalid("detections[" + index + "].bbox.height must be greater than zero");
        }
    }

    private void invalid(String message) {
        throw new VisionApiException(HttpStatus.BAD_REQUEST, "VISION_PAYLOAD_INVALID", message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ValidatedVisionEvent(
            VisionCameraEntity camera,
            BranchEntity branch) {
    }
}
