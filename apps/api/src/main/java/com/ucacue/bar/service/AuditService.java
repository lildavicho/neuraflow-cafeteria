package com.ucacue.bar.service;

import com.ucacue.bar.entity.AuditLogEntity;
import com.ucacue.bar.entity.LoginAuditEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.repository.AuditLogRepository;
import com.ucacue.bar.repository.LoginAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final LoginAuditRepository loginAuditRepository;
    private final TenantContextResolver tenantContextResolver;

    /** Convenience overload for service-layer business events (no HTTP context). */
    public void logAction(String userEmail, String action,
                          String entityType, Long entityId, String details) {
        logAction(null, userEmail, action, entityType, entityId, details, null, null);
    }

    @Async
    @Transactional
    public void logAction(Long userId, String userEmail, String action,
                          String entityType, Long entityId, String details,
                          String ipAddress, String userAgent) {
        try {
            Long tenantId = resolveTenantIdSafe();
            auditLogRepository.save(AuditLogEntity.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .userEmail(userEmail)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build());
        } catch (Exception e) {
            log.error("Error saving audit log", e);
        }
    }

    @Async
    @Transactional
    public void logLogin(Long userId, String email, String action, boolean success,
                         String ipAddress, String userAgent, String failureReason) {
        try {
            Long tenantId = resolveTenantIdSafe();
            loginAuditRepository.save(LoginAuditEntity.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .email(email)
                    .action(action)
                    .success(success)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .failureReason(failureReason)
                    .build());
        } catch (Exception e) {
            log.error("Error saving login audit", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntity> listAuditLogs(Long userId, String action, String entityType,
                                               LocalDateTime from, LocalDateTime to,
                                               int page, int size) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        if (userId != null) {
            return auditLogRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId, PageRequest.of(page, size));
        }
        if (action != null) {
            return auditLogRepository.findByTenantIdAndActionOrderByCreatedAtDesc(tenantId, action, PageRequest.of(page, size));
        }
        if (entityType != null) {
            return auditLogRepository.findByTenantIdAndEntityTypeOrderByCreatedAtDesc(tenantId, entityType, PageRequest.of(page, size));
        }
        if (from != null && to != null) {
            return auditLogRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, from, to, PageRequest.of(page, size));
        }
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<LoginAuditEntity> listLoginAudit(String email, int page, int size) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        if (email != null) {
            return loginAuditRepository.findByTenantIdAndEmailOrderByCreatedAtDesc(tenantId, email, PageRequest.of(page, size));
        }
        return loginAuditRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size));
    }

    private Long resolveTenantIdSafe() {
        try {
            return tenantContextResolver.resolveCurrent().getId();
        } catch (Exception ex) {
            try {
                return tenantContextResolver.resolveDefault().getId();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    @Transactional(readOnly = true)
    public long countRecentFailedLogins(String email, int minutes) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("El email es obligatorio");
        }
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return loginAuditRepository.countByEmailAndSuccessFalseAndCreatedAtAfter(email, since);
    }
}
