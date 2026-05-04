package com.ucacue.bar.erp.commercial.application;

import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class TenantModuleGuardAspect {

    private final TenantFeatureService tenantFeatureService;

    @Before("@within(com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule) || @annotation(com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule)")
    public void validateTenantAccess(JoinPoint joinPoint) {
        RequireTenantModule annotation = resolveAnnotation(joinPoint);
        if (annotation == null) {
            return;
        }

        String tenantCode = resolveTenantCode();
        tenantFeatureService.assertModuleEnabled(tenantCode, annotation.value());
    }

    private RequireTenantModule resolveAnnotation(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequireTenantModule annotation = method.getAnnotation(RequireTenantModule.class);
        if (annotation != null) {
            return annotation;
        }
        return joinPoint.getTarget().getClass().getAnnotation(RequireTenantModule.class);
    }

    private String resolveTenantCode() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return "default";
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        String tenantCode = request.getHeader("X-Tenant-Code");
        return (tenantCode == null || tenantCode.isBlank()) ? "default" : tenantCode;
    }
}
