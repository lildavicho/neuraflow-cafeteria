package com.ucacue.bar.erp.platform.application;

import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.BranchEntity;
import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.SequenceEntity;
import com.ucacue.bar.erp.platform.infrastructure.persistence.entity.SequenceEntity.ResetPolicy;
import com.ucacue.bar.erp.platform.infrastructure.persistence.repository.BranchRepository;
import com.ucacue.bar.erp.platform.infrastructure.persistence.repository.SequenceRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SequenceService {

    private final SequenceRepository sequenceRepository;
    private final BranchRepository branchRepository;
    private final TenantContextResolver tenantContextResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SequenceEntity> list() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return sequenceRepository.findByTenantIdOrderByCodeAsc(tenantId);
    }

    @Transactional(readOnly = true)
    public SequenceEntity getById(Long id) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return sequenceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Secuencia no encontrada: " + id));
    }

    @Transactional
    public SequenceEntity create(CreateSequenceCommand cmd) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();

        SequenceEntity seq = new SequenceEntity();
        seq.setTenantId(tenantId);
        seq.setCode(cmd.code().trim().toUpperCase());
        seq.setName(cmd.name());
        seq.setPrefix(cmd.prefix());
        seq.setSuffix(cmd.suffix());
        seq.setPadLength(cmd.padLength() != null ? cmd.padLength() : 7);
        seq.setIncrementStep(cmd.incrementStep() != null ? cmd.incrementStep() : 1);
        seq.setResetPolicy(cmd.resetPolicy() != null ? cmd.resetPolicy() : ResetPolicy.NEVER);
        seq.setCurrentNumber(0L);

        if (cmd.branchId() != null) {
            BranchEntity branch = branchRepository.findByIdAndTenantId(cmd.branchId(), tenantId)
                    .orElseThrow(() -> new BadRequestException("Sucursal no encontrada: " + cmd.branchId()));
            seq.setBranch(branch);
        }

        SequenceEntity saved = sequenceRepository.save(seq);
        auditService.logAction(null, "CREATE", "SEQUENCE", saved.getId(),
                "Secuencia creada: " + saved.getCode());
        return saved;
    }

    @Transactional
    public SequenceEntity update(Long id, UpdateSequenceCommand cmd) {
        SequenceEntity existing = getById(id);
        if (cmd.name() != null) existing.setName(cmd.name());
        if (cmd.prefix() != null) existing.setPrefix(cmd.prefix());
        if (cmd.suffix() != null) existing.setSuffix(cmd.suffix());
        if (cmd.padLength() != null) existing.setPadLength(cmd.padLength());
        if (cmd.incrementStep() != null) existing.setIncrementStep(cmd.incrementStep());
        if (cmd.resetPolicy() != null) existing.setResetPolicy(cmd.resetPolicy());
        if (cmd.active() != null) existing.setActive(cmd.active());

        SequenceEntity saved = sequenceRepository.save(existing);
        auditService.logAction(null, "UPDATE", "SEQUENCE", saved.getId(),
                "Secuencia actualizada: " + saved.getCode());
        return saved;
    }

    /**
     * Generates the next formatted number from a sequence.
     * Uses pessimistic locking to guarantee uniqueness under concurrency.
     * Supports branch-specific sequences with global fallback.
     */
    @Transactional
    public String nextNumber(String code, Long branchId) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();

        SequenceEntity sequence;
        if (branchId != null) {
            List<SequenceEntity> candidates = sequenceRepository.findByCodeWithBranchFallback(tenantId, code, branchId);
            if (candidates.isEmpty()) {
                throw new NotFoundException("No hay secuencia activa para código: " + code);
            }
            sequence = candidates.get(0);
        } else {
            sequence = sequenceRepository.findGlobalByCode(tenantId, code)
                    .orElseThrow(() -> new NotFoundException("No hay secuencia global activa para código: " + code));
        }

        // Lock and increment
        SequenceEntity locked = sequenceRepository.findWithLock(sequence.getId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Secuencia no disponible"));

        // Check reset policy
        applyResetIfNeeded(locked);

        long nextVal = locked.getCurrentNumber() + locked.getIncrementStep();
        locked.setCurrentNumber(nextVal);
        sequenceRepository.save(locked);

        return formatNumber(locked, nextVal);
    }

    /**
     * Preview the next number without incrementing (for UI display).
     */
    @Transactional(readOnly = true)
    public String previewNext(String code, Long branchId) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();

        SequenceEntity sequence;
        if (branchId != null) {
            List<SequenceEntity> candidates = sequenceRepository.findByCodeWithBranchFallback(tenantId, code, branchId);
            if (candidates.isEmpty()) return "N/A";
            sequence = candidates.get(0);
        } else {
            sequence = sequenceRepository.findGlobalByCode(tenantId, code).orElse(null);
            if (sequence == null) return "N/A";
        }

        long nextVal = sequence.getCurrentNumber() + sequence.getIncrementStep();
        return formatNumber(sequence, nextVal);
    }

    private String formatNumber(SequenceEntity seq, long number) {
        String padded = String.format("%0" + seq.getPadLength() + "d", number);
        String prefix = seq.getPrefix() != null ? seq.getPrefix() : "";
        String suffix = seq.getSuffix() != null ? seq.getSuffix() : "";
        return prefix + padded + suffix;
    }

    private void applyResetIfNeeded(SequenceEntity seq) {
        if (seq.getResetPolicy() == ResetPolicy.NEVER) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastReset = seq.getLastResetAt();

        boolean shouldReset = false;
        if (lastReset == null) {
            shouldReset = false; // first use, don't reset
        } else if (seq.getResetPolicy() == ResetPolicy.YEARLY) {
            shouldReset = now.getYear() > lastReset.getYear();
        } else if (seq.getResetPolicy() == ResetPolicy.MONTHLY) {
            shouldReset = now.getYear() > lastReset.getYear()
                    || (now.getYear() == lastReset.getYear() && now.getMonthValue() > lastReset.getMonthValue());
        }

        if (shouldReset) {
            seq.setCurrentNumber(0L);
            seq.setLastResetAt(now);
        }
    }

    // ── Commands ─────────────────────────────────────────────────────

    public record CreateSequenceCommand(
            String code,
            String name,
            Long branchId,
            String prefix,
            String suffix,
            Integer padLength,
            Integer incrementStep,
            ResetPolicy resetPolicy
    ) {}

    public record UpdateSequenceCommand(
            String name,
            String prefix,
            String suffix,
            Integer padLength,
            Integer incrementStep,
            ResetPolicy resetPolicy,
            Boolean active
    ) {}
}
