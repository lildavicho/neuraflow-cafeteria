package com.ucacue.bar.erp.vision.application;

import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionAuditLogEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionProcessingLogEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionWebhookLogEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.repository.VisionAuditLogRepository;
import com.ucacue.bar.erp.vision.infrastructure.persistence.repository.VisionProcessingLogRepository;
import com.ucacue.bar.erp.vision.infrastructure.persistence.repository.VisionWebhookLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class VisionAuditService {

    private final VisionProcessingLogRepository processingLogRepository;
    private final VisionWebhookLogRepository webhookLogRepository;
    private final VisionAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logWebhook(Long tenantId,
                           Long eventId,
                           String idempotencyKey,
                           String status,
                           String responseCode,
                           String responseMessage,
                           Map<String, Object> headers,
                           Map<String, Object> payload) {
        VisionWebhookLogEntity log = new VisionWebhookLogEntity();
        log.setTenantId(tenantId);
        log.setEventId(eventId);
        log.setIdempotencyKey(idempotencyKey);
        log.setStatus(status);
        log.setResponseCode(responseCode);
        log.setResponseMessage(responseMessage);
        log.setRequestHeaders(headers == null ? Map.of() : headers);
        log.setPayloadJson(payload == null ? Map.of() : payload);
        webhookLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logProcessing(Long tenantId,
                              Long eventId,
                              Long cameraId,
                              String level,
                              String code,
                              String message,
                              Map<String, Object> metadata) {
        VisionProcessingLogEntity log = new VisionProcessingLogEntity();
        log.setTenantId(tenantId);
        log.setEventId(eventId);
        log.setCameraId(cameraId);
        log.setLevel(level == null || level.isBlank() ? "INFO" : level);
        log.setCode(code);
        log.setMessage(message);
        log.setMetadata(metadata == null ? Map.of() : metadata);
        processingLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAudit(Long tenantId,
                         Long eventId,
                         Long cameraId,
                         String action,
                         String message,
                         Map<String, Object> metadata) {
        VisionAuditLogEntity log = new VisionAuditLogEntity();
        log.setTenantId(tenantId);
        log.setAction(action);
        log.setResourceType("VISION_EVENT");
        log.setResourceId(eventId);
        log.setMetadata(metadata == null ? Map.of() : metadata);
        auditLogRepository.save(log);

        if (tenantId != null) {
            logProcessing(
                    tenantId,
                    eventId,
                    cameraId,
                    action != null && action.endsWith("_INVALID") ? "WARN" : "INFO",
                    action,
                    message,
                    metadata);
        }
    }
}
