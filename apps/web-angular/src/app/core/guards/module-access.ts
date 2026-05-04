import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { SessionContext } from '../services/session-context';

export const moduleAccessGuard: CanActivateFn = async (route) => {
  const router = inject(Router);
  const sessionContext = inject(SessionContext);
  const requiredModule = route.data['requiredModule'] as string | undefined;

  if (!requiredModule) {
    return true;
  }

  const hasModule = await sessionContext.hasModule(requiredModule);
  if (hasModule) {
    sessionContext.clearModuleDenied();
    return true;
  }

  sessionContext.registerModuleDenied(requiredModule);
  return router.createUrlTree([sessionContext.fallbackRoute()]);
};
