import { HttpInterceptorFn } from '@angular/common/http';

export const tenantContextInterceptor: HttpInterceptorFn = (req, next) => {
  const tenantCode = resolveTenantCode();

  return next(
    req.clone({
      setHeaders: {
        'X-Tenant-Code': tenantCode,
      },
    }),
  );
};

function resolveTenantCode(): string {
  if (typeof localStorage === 'undefined') {
    return 'default';
  }

  const storedTenant = localStorage.getItem('tenantCode') || 'default';
  const userTenant = readUserTenantCode();
  if (userTenant && !readUserPlatformAdmin()) {
    return userTenant;
  }
  return storedTenant;
}

function readUserTenantCode(): string {
  const raw = localStorage.getItem('auth.user');
  if (!raw) {
    return '';
  }
  try {
    const parsed = JSON.parse(raw) as { tenantCode?: unknown };
    return typeof parsed.tenantCode === 'string' ? parsed.tenantCode : '';
  } catch {
    return '';
  }
}

function readUserPlatformAdmin(): boolean {
  const raw = localStorage.getItem('auth.user');
  if (!raw) {
    return false;
  }
  try {
    const parsed = JSON.parse(raw) as { platformAdmin?: unknown };
    return parsed.platformAdmin === true;
  } catch {
    return false;
  }
}
