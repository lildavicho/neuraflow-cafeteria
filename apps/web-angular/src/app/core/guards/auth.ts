import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthSession } from '../services/auth-session';

export const authGuard: CanActivateFn = (_, state) => {
  const authSession = inject(AuthSession);
  const router = inject(Router);

  if (authSession.isAuthenticated()) {
    return true;
  }

  const redirect = state.url && state.url !== '/' ? state.url : undefined;
  return router.createUrlTree(['/login'], {
    queryParams: redirect ? { redirect } : undefined,
  });
};

export const guestGuard: CanActivateFn = () => {
  const authSession = inject(AuthSession);
  const router = inject(Router);

  if (!authSession.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/']);
};

export const platformAdminGuard: CanActivateFn = () => {
  const authSession = inject(AuthSession);
  const router = inject(Router);

  if (authSession.user()?.platformAdmin === true) {
    return true;
  }

  return router.createUrlTree(['/']);
};
