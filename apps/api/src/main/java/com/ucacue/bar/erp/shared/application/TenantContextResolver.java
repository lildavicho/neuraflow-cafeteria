package com.ucacue.bar.erp.shared.application;

import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantRepository;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.exception.UnauthorizedException;
import com.ucacue.bar.repository.UserRepository;
import com.ucacue.bar.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TenantContextResolver {

    private static final String DEFAULT_TENANT_CODE = "default";
    private static final String TENANT_CODE_PATTERN = "^[A-Za-z0-9_-]{1,64}$";
    private static final String CURRENT_USER_ATTRIBUTE = TenantContextResolver.class.getName() + ".currentUser";
    private static final String TENANT_ATTRIBUTE_PREFIX = TenantContextResolver.class.getName() + ".tenant.";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public TenantEntity resolve(String tenantCode) {
        String effectiveTenantCode = resolveEffectiveTenantCode(tenantCode);
        validateTenantCode(effectiveTenantCode);
        authorizeTenantSelection(effectiveTenantCode);
        String cacheKey = TENANT_ATTRIBUTE_PREFIX + effectiveTenantCode.toLowerCase(Locale.ROOT);
        TenantEntity cached = requestAttribute(cacheKey, TenantEntity.class);
        if (cached != null) {
            return cached;
        }
        return tenantRepository.findByTenantCodeIgnoreCase(effectiveTenantCode)
                .map(tenant -> {
                    setRequestAttribute(cacheKey, tenant);
                    return tenant;
                })
                .orElseThrow(() -> new NotFoundException("Tenant no encontrado: " + effectiveTenantCode));
    }

    public TenantEntity resolveDefault() {
        return resolve(DEFAULT_TENANT_CODE);
    }

    public TenantEntity resolveCurrent() {
        return resolve(null);
    }

    private void validateTenantCode(String tenantCode) {
        if (!tenantCode.matches(TENANT_CODE_PATTERN)) {
            throw new BadRequestException("tenantCode invalido");
        }
    }

    private void authorizeTenantSelection(String tenantCode) {
        UserEntity user = currentUser();
        if (user == null) {
            if (DEFAULT_TENANT_CODE.equalsIgnoreCase(tenantCode)) {
                return;
            }
            if (currentAuthenticationHasRole("ROLE_ADMIN")) {
                return;
            }
            throw new AccessDeniedException("No puede seleccionar otro tenant");
        }

        if (Boolean.TRUE.equals(user.getPlatformAdmin())) {
            return;
        }

        String userTenantCode = user.getTenant() != null && user.getTenant().getTenantCode() != null
                ? user.getTenant().getTenantCode()
                : DEFAULT_TENANT_CODE;
        if (userTenantCode.equalsIgnoreCase(tenantCode)) {
            return;
        }
        throw new AccessDeniedException("No puede seleccionar otro tenant");
    }

    private boolean currentAuthenticationHasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private String resolveEffectiveTenantCode(String requestedTenantCode) {
        if (requestedTenantCode != null && !requestedTenantCode.isBlank()) {
            return requestedTenantCode.trim();
        }
        UserEntity user = currentUser();
        if (user != null && user.getTenant() != null && user.getTenant().getTenantCode() != null) {
            return user.getTenant().getTenantCode();
        }
        return DEFAULT_TENANT_CODE;
    }

    private UserEntity currentUser() {
        UserEntity cached = requestAttribute(CURRENT_USER_ATTRIBUTE, UserEntity.class);
        if (cached != null) {
            return cached;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            UserEntity user = customUserDetails.getUser();
            if (user != null && Boolean.TRUE.equals(user.getActive())) {
                setRequestAttribute(CURRENT_USER_ATTRIBUTE, user);
                return user;
            }
            return cacheCurrentUser(userRepository.findById(customUserDetails.getId())
                    .filter(item -> Boolean.TRUE.equals(item.getActive()))
                    .orElseThrow(() -> new UnauthorizedException("Usuario autenticado no existe")));
        }
        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return null;
        }
        return cacheCurrentUser(userRepository.findByEmailIgnoreCaseWithTenant(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario autenticado no existe")));
    }

    private UserEntity cacheCurrentUser(UserEntity user) {
        setRequestAttribute(CURRENT_USER_ATTRIBUTE, user);
        return user;
    }

    private <T> T requestAttribute(String name, Class<T> type) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    private void setRequestAttribute(String name, Object value) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null && value != null) {
            attributes.setAttribute(name, value, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
