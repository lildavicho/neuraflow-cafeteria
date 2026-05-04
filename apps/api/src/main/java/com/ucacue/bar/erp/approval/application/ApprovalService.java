package com.ucacue.bar.erp.approval.application;

import com.ucacue.bar.erp.approval.domain.ApprovalTypes.ApprovalAction;
import com.ucacue.bar.erp.approval.domain.ApprovalTypes.ApprovalStatus;
import com.ucacue.bar.erp.approval.infrastructure.persistence.entity.*;
import com.ucacue.bar.erp.approval.infrastructure.persistence.repository.*;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalDefinitionRepository definitionRepository;
    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalActionRepository actionRepository;
    private final TenantContextResolver tenantContextResolver;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    // ── Definition CRUD ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ApprovalDefinitionEntity> listDefinitions() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return definitionRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    @Transactional(readOnly = true)
    public ApprovalDefinitionEntity getDefinition(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return definitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Flujo de aprobación no encontrado: " + id));
    }

    @Transactional
    public ApprovalDefinitionEntity createDefinition(CreateDefinitionCommand cmd) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();

        definitionRepository.findByTenantIdAndCode(tenantId, cmd.code())
                .ifPresent(e -> { throw new BadRequestException("Ya existe un flujo con código: " + cmd.code()); });

        ApprovalDefinitionEntity def = new ApprovalDefinitionEntity();
        def.setTenantId(tenantId);
        def.setCode(cmd.code());
        def.setName(cmd.name());
        def.setEntityType(cmd.entityType());
        def.setConditionExpr(cmd.conditionExpr());

        if (cmd.steps() != null) {
            int order = 1;
            for (StepCommand stepCmd : cmd.steps()) {
                ApprovalStepEntity step = new ApprovalStepEntity();
                step.setTenantId(tenantId);
                step.setDefinition(def);
                step.setStepOrder(order++);
                step.setName(stepCmd.name());
                step.setApproverRole(stepCmd.approverRole());
                step.setRequiredCount(stepCmd.requiredCount() != null ? stepCmd.requiredCount() : 1);
                def.getSteps().add(step);
            }
        }

        ApprovalDefinitionEntity saved = definitionRepository.save(def);
        auditService.logAction(null, "CREATE", "APPROVAL_DEFINITION", saved.getId(),
                "Flujo de aprobación creado: " + saved.getName());
        return saved;
    }

    @Transactional
    public ApprovalDefinitionEntity updateDefinition(Long id, UpdateDefinitionCommand cmd) {
        ApprovalDefinitionEntity existing = getDefinition(id);
        if (cmd.name() != null) existing.setName(cmd.name());
        if (cmd.conditionExpr() != null) existing.setConditionExpr(cmd.conditionExpr());
        if (cmd.active() != null) existing.setActive(cmd.active());

        ApprovalDefinitionEntity saved = definitionRepository.save(existing);
        auditService.logAction(null, "UPDATE", "APPROVAL_DEFINITION", saved.getId(),
                "Flujo de aprobación actualizado: " + saved.getName());
        return saved;
    }

    // ── Instance management ──────────────────────────────────────────

    @Transactional
    public ApprovalInstanceEntity requestApproval(String entityType, Long entityId, Long requestedBy) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();

        instanceRepository.findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId)
                .ifPresent(existing -> {
                    if (existing.getStatus() == ApprovalStatus.PENDING) {
                        throw new BadRequestException("Ya existe una solicitud de aprobación pendiente para esta entidad");
                    }
                });

        List<ApprovalDefinitionEntity> definitions =
                definitionRepository.findActiveByEntityType(tenantId, entityType);

        if (definitions.isEmpty()) {
            throw new BadRequestException("No hay flujos de aprobación configurados para: " + entityType);
        }

        ApprovalDefinitionEntity definition = definitions.get(0);

        ApprovalInstanceEntity instance = new ApprovalInstanceEntity();
        instance.setTenantId(tenantId);
        instance.setDefinition(definition);
        instance.setEntityType(entityType);
        instance.setEntityId(entityId);
        instance.setCurrentStep(1);
        instance.setStatus(ApprovalStatus.PENDING);
        instance.setRequestedBy(requestedBy);

        ApprovalInstanceEntity saved = instanceRepository.save(instance);
        auditService.logAction(null, "CREATE", "APPROVAL_INSTANCE", saved.getId(),
                "Solicitud de aprobación creada para " + entityType + " #" + entityId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ApprovalInstanceEntity> listPending(Pageable pageable) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return instanceRepository.findPendingWithDefinition(tenantId, ApprovalStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public ApprovalInstanceEntity getInstance(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return instanceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Solicitud de aprobación no encontrada: " + id));
    }

    @Transactional
    public ApprovalInstanceEntity processAction(Long instanceId, ApprovalAction action,
                                                 Long userId, String userName, String comment) {
        ApprovalInstanceEntity instance = getInstance(instanceId);

        if (instance.getStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Esta solicitud ya no está pendiente. Estado: " + instance.getStatus());
        }

        ApprovalActionEntity actionEntity = new ApprovalActionEntity();
        actionEntity.setTenantId(instance.getTenantId());
        actionEntity.setInstance(instance);
        actionEntity.setStepOrder(instance.getCurrentStep());
        actionEntity.setAction(action);
        actionEntity.setUserId(userId);
        actionEntity.setUserName(userName);
        actionEntity.setComment(comment);
        actionRepository.save(actionEntity);

        if (action == ApprovalAction.REJECT) {
            instance.setStatus(ApprovalStatus.REJECTED);
            auditService.logAction(null, "REJECT", "APPROVAL_INSTANCE", instanceId,
                    "Solicitud rechazada por " + userName);
        } else if (action == ApprovalAction.APPROVE) {
            long approvalCount = actionRepository.countByInstanceIdAndStepOrderAndAction(
                    instanceId, instance.getCurrentStep(), ApprovalAction.APPROVE);

            ApprovalStepEntity currentStep = instance.getDefinition().getSteps().stream()
                    .filter(s -> s.getStepOrder().equals(instance.getCurrentStep()))
                    .findFirst()
                    .orElse(null);

            int requiredCount = currentStep != null ? currentStep.getRequiredCount() : 1;

            if (approvalCount >= requiredCount) {
                int totalSteps = instance.getDefinition().getSteps().size();
                if (instance.getCurrentStep() >= totalSteps) {
                    instance.setStatus(ApprovalStatus.APPROVED);
                    auditService.logAction(null, "APPROVE", "APPROVAL_INSTANCE", instanceId,
                            "Solicitud aprobada completamente por " + userName);
                } else {
                    instance.setCurrentStep(instance.getCurrentStep() + 1);
                    auditService.logAction(null, "ADVANCE_STEP", "APPROVAL_INSTANCE", instanceId,
                            "Paso " + (instance.getCurrentStep() - 1) + " aprobado, avanza a paso " + instance.getCurrentStep());
                }
            }
        }

        ApprovalInstanceEntity saved = instanceRepository.save(instance);
        if (saved.getStatus() != ApprovalStatus.PENDING) {
            publishStatusChanged(saved);
        }
        return saved;
    }

    @Transactional
    public ApprovalInstanceEntity cancel(Long instanceId) {
        ApprovalInstanceEntity instance = getInstance(instanceId);
        if (instance.getStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Solo se pueden cancelar solicitudes pendientes");
        }
        instance.setStatus(ApprovalStatus.CANCELLED);
        auditService.logAction(null, "CANCEL", "APPROVAL_INSTANCE", instanceId,
                "Solicitud de aprobación cancelada");
        ApprovalInstanceEntity saved = instanceRepository.save(instance);
        publishStatusChanged(saved);
        return saved;
    }

    private void publishStatusChanged(ApprovalInstanceEntity instance) {
        eventPublisher.publishEvent(new ApprovalStatusChangedEvent(
                instance.getTenantId(),
                instance.getId(),
                instance.getEntityType(),
                instance.getEntityId(),
                instance.getStatus()));
    }

    @Transactional(readOnly = true)
    public long countPending() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return instanceRepository.countByTenantIdAndStatus(tenantId, ApprovalStatus.PENDING);
    }

    // ── Commands ─────────────────────────────────────────────────────

    public record CreateDefinitionCommand(
            String code,
            String name,
            String entityType,
            String conditionExpr,
            List<StepCommand> steps
    ) {}

    public record StepCommand(
            String name,
            String approverRole,
            Integer requiredCount
    ) {}

    public record UpdateDefinitionCommand(
            String name,
            String conditionExpr,
            Boolean active
    ) {}
}
