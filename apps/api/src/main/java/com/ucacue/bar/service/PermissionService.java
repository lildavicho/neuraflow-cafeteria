package com.ucacue.bar.service;

import com.ucacue.bar.entity.PermissionEntity;
import com.ucacue.bar.entity.RolePermissionEntity;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.entity.UserEntity.UserRole;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.repository.PermissionRepository;
import com.ucacue.bar.repository.RolePermissionRepository;
import com.ucacue.bar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final TenantContextResolver tenantContextResolver;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PermissionEntity> listAll() {
        return permissionRepository.findAllByOrderByModuleCodeAscCodeAsc();
    }

    /**
     * Resuelve los códigos de permiso efectivos para el usuario en el tenant activo.
     * Usa override por tenant si existe, o defaults globales en caso contrario.
     */
    @Transactional(readOnly = true)
    public Set<String> resolvePermissionsFor(UserEntity user) {
        if (user == null || user.getRole() == null) {
            return Set.of();
        }
        Long tenantId;
        try {
            tenantId = tenantContextResolver.resolveCurrent().getId();
        } catch (RuntimeException ex) {
            tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        }
        return resolveForRoleAndTenant(user.getRole(), tenantId);
    }

    @Transactional(readOnly = true)
    public Set<String> resolveForRoleAndTenant(UserRole role, Long tenantId) {
        if (role == null) {
            return Set.of();
        }
        if (tenantId == null) {
            return rolePermissionRepository.findDefaultsByRole(role).stream()
                    .map(rp -> rp.getPermission().getCode())
                    .collect(Collectors.toUnmodifiableSet());
        }
        return rolePermissionRepository.findEffective(role, tenantId).stream()
                .map(rp -> rp.getPermission().getCode())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasPermission(UserEntity user, String permissionCode) {
        if (user == null || permissionCode == null) return false;
        return resolvePermissionsFor(user).contains(permissionCode);
    }

    /**
     * SpEL-friendly: @permissionService.can('orders.create')
     * Resuelve el usuario autenticado y evalúa si tiene el permiso en el tenant activo.
     * Platform admins pasan siempre.
     */
    @Transactional(readOnly = true)
    public boolean can(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        String email = auth.getName();
        if (email == null || email.isBlank()) return false;
        return userRepository.findByEmailIgnoreCaseWithTenant(email)
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .map(u -> Boolean.TRUE.equals(u.getPlatformAdmin())
                        || resolvePermissionsFor(u).contains(permissionCode))
                .orElse(false);
    }

    /**
     * Reemplaza la lista de permisos para (role, tenant). Pasar lista vacía
     * equivale a revocar todos los permisos del rol en ese tenant (override vacío).
     */
    @Transactional
    public void setTenantPermissions(UserRole role, List<String> permissionCodes) {
        if (role == null) throw new BadRequestException("El rol es obligatorio");
        Long tenantId = tenantContextResolver.resolveCurrent().getId();

        rolePermissionRepository.deleteByRoleAndTenantId(role, tenantId);
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            log.info("Permissions cleared for role={} tenant={}", role, tenantId);
            return;
        }

        for (String code : permissionCodes) {
            if (code == null || code.isBlank()) continue;
            PermissionEntity permission = permissionRepository.findByCode(code.trim())
                    .orElseThrow(() -> new NotFoundException("Permiso no encontrado: " + code));
            rolePermissionRepository.save(RolePermissionEntity.builder()
                    .role(role)
                    .permission(permission)
                    .tenantId(tenantId)
                    .build());
        }
        log.info("Assigned {} permissions to role={} tenant={}", permissionCodes.size(), role, tenantId);
    }

    /** Restablece el rol a defaults globales eliminando las overrides de este tenant. */
    @Transactional
    public void resetToDefaults(UserRole role) {
        if (role == null) throw new BadRequestException("El rol es obligatorio");
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        rolePermissionRepository.deleteByRoleAndTenantId(role, tenantId);
        log.info("Permissions reset to defaults for role={} tenant={}", role, tenantId);
    }
}
