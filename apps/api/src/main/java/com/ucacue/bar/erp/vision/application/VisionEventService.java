package com.ucacue.bar.erp.vision.application;

import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.BranchEntity;
import com.ucacue.bar.erp.vision.application.VisionTenantResolver.ResolvedVisionTenant;
import com.ucacue.bar.erp.vision.application.VisionValidationService.ValidatedVisionEvent;
import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionCameraEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionDetectionEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionEventEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.repository.VisionDetectionRepository;
import com.ucacue.bar.erp.vision.infrastructure.persistence.repository.VisionEventRepository;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionEventRequest;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VisionEventService {

    private final VisionTenantResolver visionTenantResolver;
    private final VisionValidationService visionValidationService;
    private final VisionEventRepository visionEventRepository;
    private final VisionDetectionRepository visionDetectionRepository;
    private final VisionStatisticsService visionStatisticsService;
    private final VisionAuditService visionAuditService;
    private final PlatformTransactionManager transactionManager;

    public VisionEventResponse receiveEvent(String apiKey,
                                            String idempotencyKey,
                                            VisionEventRequest request) {
        Long tenantId = null;
        VisionCameraEntity camera = null;
        try {
            ResolvedVisionTenant resolvedTenant = visionTenantResolver.resolveFromApiKey(apiKey);
            tenantId = resolvedTenant.tenantId();
            ValidatedVisionEvent validated = visionValidationService.validate(tenantId, request);
            camera = validated.camera();

            String normalizedIdempotencyKey = normalize(idempotencyKey);
            VisionEventResponse duplicate = duplicateResponse(tenantId, camera, request, normalizedIdempotencyKey);
            if (duplicate != null) {
                auditDuplicate(tenantId, camera, request, normalizedIdempotencyKey, duplicate.eventId());
                return duplicate;
            }

            try {
                Long resolvedTenantId = tenantId;
                VisionEventResponse response = new TransactionTemplate(transactionManager)
                        .execute(status -> storeEvent(resolvedTenantId, validated, request, normalizedIdempotencyKey));
                auditSuccess(tenantId, camera, request, normalizedIdempotencyKey, response);
                return response;
            } catch (DataIntegrityViolationException ex) {
                VisionEventResponse raceDuplicate = duplicateResponse(tenantId, camera, request, normalizedIdempotencyKey);
                if (raceDuplicate != null) {
                    auditDuplicate(tenantId, camera, request, normalizedIdempotencyKey, raceDuplicate.eventId());
                    return raceDuplicate;
                }
                auditInternalError(tenantId, camera, request, normalizedIdempotencyKey, ex);
                throw ex;
            }
        } catch (VisionApiException ex) {
            auditVisionError(tenantId, camera, request, normalize(idempotencyKey), ex);
            throw ex;
        } catch (RuntimeException ex) {
            auditInternalError(tenantId, camera, request, normalize(idempotencyKey), ex);
            throw new VisionApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "VISION_INTERNAL_ERROR",
                    "Vision event could not be processed");
        }
    }

    private VisionEventResponse storeEvent(Long tenantId,
                                           ValidatedVisionEvent validated,
                                           VisionEventRequest request,
                                           String normalizedIdempotencyKey) {
        VisionEventEntity event = buildEvent(tenantId, validated.camera(), validated.branch(), request, normalizedIdempotencyKey);
        event = visionEventRepository.saveAndFlush(event);

        List<VisionDetectionEntity> detections = buildDetections(event, request);
        if (!detections.isEmpty()) {
            visionDetectionRepository.saveAll(detections);
        }
        visionStatisticsService.updateForEvent(event, detections);

        event.setStatus(VisionEventEntity.Status.PROCESSED);
        visionEventRepository.save(event);

        return new VisionEventResponse(
                true,
                null,
                String.valueOf(event.getId()),
                true,
                validated.camera().getCode(),
                validated.branch() != null ? validated.branch().getCode() : null,
                request.detections() != null ? request.detections().size() : 0,
                detections.size(),
                false,
                true,
                OffsetDateTime.now(),
                "Vision event stored successfully");
    }

    private VisionEventResponse duplicateResponse(Long tenantId,
                                                  VisionCameraEntity camera,
                                                  VisionEventRequest request,
                                                  String idempotencyKey) {
        if (idempotencyKey != null) {
            VisionEventResponse response = visionEventRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                    .map(existing -> duplicateResponse(existing, camera, request))
                    .orElse(null);
            if (response != null) {
                return response;
            }
        }
        if (!isBlank(request.frameId())) {
            return visionEventRepository.findByTenantIdAndCameraIdAndFrameId(tenantId, camera.getId(), request.frameId().trim())
                    .map(existing -> duplicateResponse(existing, camera, request))
                    .orElse(null);
        }
        return null;
    }

    private VisionEventResponse duplicateResponse(VisionEventEntity existing,
                                                  VisionCameraEntity camera,
                                                  VisionEventRequest request) {
        return new VisionEventResponse(
                true,
                "VISION_EVENT_ALREADY_PROCESSED",
                String.valueOf(existing.getId()),
                true,
                camera.getCode(),
                request.locationCode(),
                request.detections() != null ? request.detections().size() : 0,
                existing.getTotalDetections() != null ? existing.getTotalDetections() : 0,
                true,
                false,
                OffsetDateTime.now(),
                "Event was already processed");
    }

    private void auditSuccess(Long tenantId,
                              VisionCameraEntity camera,
                              VisionEventRequest request,
                              String idempotencyKey,
                              VisionEventResponse response) {
        Map<String, Object> metadata = auditMetadata(camera, request, idempotencyKey);
        metadata.put("storedDetections", response.storedDetections());
        visionAuditService.logAudit(
                tenantId,
                parseLong(response.eventId()),
                camera != null ? camera.getId() : null,
                "VISION_EVENT_RECEIVED",
                "Vision event stored successfully",
                metadata);
        visionAuditService.logWebhook(
                tenantId,
                parseLong(response.eventId()),
                idempotencyKey,
                "PROCESSED",
                "VISION_EVENT_RECEIVED",
                "Vision event stored successfully",
                Map.of(),
                metadata);
    }

    private void auditDuplicate(Long tenantId,
                                VisionCameraEntity camera,
                                VisionEventRequest request,
                                String idempotencyKey,
                                String eventId) {
        Map<String, Object> metadata = auditMetadata(camera, request, idempotencyKey);
        visionAuditService.logAudit(
                tenantId,
                parseLong(eventId),
                camera != null ? camera.getId() : null,
                "VISION_EVENT_DUPLICATED",
                "Vision event was already processed",
                metadata);
        visionAuditService.logWebhook(
                tenantId,
                parseLong(eventId),
                idempotencyKey,
                "DUPLICATED",
                "VISION_EVENT_DUPLICATED",
                "Vision event was already processed",
                Map.of(),
                metadata);
    }

    private void auditVisionError(Long tenantId,
                                  VisionCameraEntity camera,
                                  VisionEventRequest request,
                                  String idempotencyKey,
                                  VisionApiException ex) {
        String action = switch (ex.code()) {
            case "VISION_PAYLOAD_INVALID" -> "VISION_PAYLOAD_INVALID";
            case "VISION_API_KEY_REQUIRED", "VISION_API_KEY_INVALID" -> "VISION_API_KEY_INVALID";
            case "CAMERA_NOT_FOUND_FOR_TENANT", "CAMERA_LOCATION_MISMATCH", "LOCATION_NOT_FOUND_FOR_TENANT" ->
                    "VISION_CAMERA_NOT_FOUND";
            default -> "VISION_INTERNAL_ERROR";
        };
        Map<String, Object> metadata = auditMetadata(camera, request, idempotencyKey);
        metadata.put("errorCode", ex.code());
        visionAuditService.logAudit(tenantId, null, camera != null ? camera.getId() : null, action, ex.getMessage(), metadata);
        visionAuditService.logWebhook(tenantId, null, idempotencyKey, "REJECTED", action, ex.getMessage(), Map.of(), metadata);
    }

    private void auditInternalError(Long tenantId,
                                    VisionCameraEntity camera,
                                    VisionEventRequest request,
                                    String idempotencyKey,
                                    RuntimeException ex) {
        Map<String, Object> metadata = auditMetadata(camera, request, idempotencyKey);
        metadata.put("errorType", ex.getClass().getSimpleName());
        visionAuditService.logAudit(
                tenantId,
                null,
                camera != null ? camera.getId() : null,
                "VISION_INTERNAL_ERROR",
                "Vision event processing failed",
                metadata);
        visionAuditService.logWebhook(
                tenantId,
                null,
                idempotencyKey,
                "FAILED",
                "VISION_INTERNAL_ERROR",
                "Vision event processing failed",
                Map.of(),
                metadata);
    }

    private Map<String, Object> auditMetadata(VisionCameraEntity camera,
                                              VisionEventRequest request,
                                              String idempotencyKey) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("idempotencyKeyPresent", idempotencyKey != null);
        if (camera != null) {
            metadata.put("cameraId", camera.getId());
            metadata.put("cameraCode", camera.getCode());
        } else if (request != null && request.cameraCode() != null) {
            metadata.put("cameraCode", request.cameraCode());
        }
        if (request != null) {
            metadata.put("frameId", normalize(request.frameId()));
            metadata.put("source", normalize(request.source()));
            metadata.put("detectionCount", request.detections() != null ? request.detections().size() : 0);
            if (request.model() != null) {
                metadata.put("modelName", normalize(request.model().name()));
                metadata.put("modelVersion", normalize(request.model().version()));
            }
        }
        return metadata;
    }

    private Long parseLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private VisionEventEntity buildEvent(Long tenantId,
                                         VisionCameraEntity camera,
                                         BranchEntity branch,
                                         VisionEventRequest request,
                                         String idempotencyKey) {
        VisionEventEntity event = new VisionEventEntity();
        event.setTenantId(tenantId);
        event.setCameraId(camera.getId());
        event.setBranchId(branch != null ? branch.getId() : camera.getBranchId());
        event.setEventTimestamp(request.timestamp());
        event.setReceivedAt(OffsetDateTime.now());
        event.setFrameId(normalize(request.frameId()));
        event.setIdempotencyKey(idempotencyKey);
        event.setSource(firstNonBlank(request.source(), "yolo-service"));
        event.setTotalDetections(request.detections() != null ? request.detections().size() : 0);
        event.setStatus(VisionEventEntity.Status.RECEIVED);
        event.setMetadata(request.metadata() == null ? Map.of() : request.metadata());
        event.setProcessingMs(intMetadata(request.metadata(), "processingMs"));

        if (request.model() != null) {
            event.setModelName(normalize(request.model().name()));
            event.setModelVersion(normalize(request.model().version()));
            event.setConfidenceThreshold(request.model().confidenceThreshold());
        }
        if (request.image() != null) {
            event.setImageWidth(request.image().width());
            event.setImageHeight(request.image().height());
            event.setImageUri(normalize(request.image().uri()));
        }
        return event;
    }

    private List<VisionDetectionEntity> buildDetections(VisionEventEntity event, VisionEventRequest request) {
        List<VisionDetectionEntity> detections = new ArrayList<>();
        if (request.detections() == null) {
            return detections;
        }
        for (VisionEventRequest.Detection item : request.detections()) {
            VisionDetectionEntity detection = new VisionDetectionEntity();
            detection.setTenantId(event.getTenantId());
            detection.setEventId(event.getId());
            detection.setCameraId(event.getCameraId());
            detection.setBranchId(event.getBranchId());
            detection.setLabel(item.label().trim());
            detection.setConfidence(scale(item.confidence(), 5));
            detection.setBboxX(scale(item.bbox().x(), 4));
            detection.setBboxY(scale(item.bbox().y(), 4));
            detection.setBboxWidth(scale(item.bbox().width(), 4));
            detection.setBboxHeight(scale(item.bbox().height(), 4));
            detection.setTrackingId(normalize(item.trackingId()));
            detection.setMetadata(item.metadata() == null ? Map.of() : item.metadata());
            detections.add(detection);
        }
        return detections;
    }

    private BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, java.math.RoundingMode.HALF_UP);
    }

    private Integer intMetadata(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstNonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
