package com.ucacue.bar.erp.vision.application;

import com.ucacue.bar.erp.vision.application.VisionTenantResolver.ResolvedVisionTenant;
import com.ucacue.bar.erp.vision.application.VisionValidationService.ValidatedVisionEvent;
import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionCameraEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.entity.VisionEventEntity;
import com.ucacue.bar.erp.vision.infrastructure.persistence.repository.VisionDetectionRepository;
import com.ucacue.bar.erp.vision.infrastructure.persistence.repository.VisionEventRepository;
import com.ucacue.bar.erp.vision.interfaces.http.dto.VisionEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisionEventServiceIdempotencyTest {

    private final VisionTenantResolver tenantResolver = mock(VisionTenantResolver.class);
    private final VisionValidationService validationService = mock(VisionValidationService.class);
    private final VisionEventRepository eventRepository = mock(VisionEventRepository.class);
    private final VisionDetectionRepository detectionRepository = mock(VisionDetectionRepository.class);
    private final VisionStatisticsService statisticsService = mock(VisionStatisticsService.class);
    private final VisionAuditService auditService = mock(VisionAuditService.class);
    private final PlatformTransactionManager txManager = new NoopTransactionManager();

    @Test
    void duplicateInsertRaceReturnsDuplicateResponseWithoutStats() {
        VisionCameraEntity camera = new VisionCameraEntity();
        camera.setId(20L);
        camera.setTenantId(10L);
        camera.setCode("CAM-001");
        camera.setName("Entrada");
        VisionEventRequest request = new VisionEventRequest(
                "CAM-001",
                null,
                OffsetDateTime.now(),
                "frame-1",
                "yolo-service",
                null,
                null,
                List.of(),
                Map.of());
        VisionEventEntity existing = new VisionEventEntity();
        existing.setId(99L);
        existing.setTenantId(10L);
        existing.setCameraId(20L);
        existing.setTotalDetections(0);

        when(tenantResolver.resolveFromApiKey("vk_live_test"))
                .thenReturn(new ResolvedVisionTenant(10L, 1L, "vk_live"));
        when(validationService.validate(10L, request))
                .thenReturn(new ValidatedVisionEvent(camera, null));
        when(eventRepository.findByTenantIdAndIdempotencyKey(10L, "idem-1"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(eventRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uk_vision_events_tenant_idempotency"));

        VisionEventService service = new VisionEventService(
                tenantResolver,
                validationService,
                eventRepository,
                detectionRepository,
                statisticsService,
                auditService,
                txManager);

        var response = service.receiveEvent("vk_live_test", "idem-1", request);

        assertThat(response.success()).isTrue();
        assertThat(response.duplicate()).isTrue();
        assertThat(response.eventId()).isEqualTo("99");
        verify(detectionRepository, never()).saveAll(any());
        verify(statisticsService, never()).updateForEvent(any(), any());
        verify(auditService).logAudit(eq(10L), eq(99L), eq(20L), eq("VISION_EVENT_DUPLICATED"), any(), any());
    }

    private static final class NoopTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
        }
    }
}
