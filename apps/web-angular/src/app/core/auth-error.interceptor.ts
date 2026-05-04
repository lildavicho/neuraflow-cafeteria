import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

const PUBLIC_ENDPOINT_FRAGMENTS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/forgot-password'];

export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        const url = req.url ?? '';
        const isPublic = PUBLIC_ENDPOINT_FRAGMENTS.some((fragment) => url.includes(fragment));
        if (!isPublic) {
          try {
            localStorage.removeItem('auth.token');
            localStorage.removeItem('auth.refreshToken');
            localStorage.removeItem('auth.user');
          } catch {
            /* ignore storage errors */
          }
          const currentUrl = router.url;
          const onAuthRoute = currentUrl.startsWith('/login') || currentUrl.startsWith('/register') || currentUrl.startsWith('/forgot-password');
          if (!onAuthRoute) {
            void router.navigate(['/login'], { queryParams: { reason: 'session-expired' } });
          }
        }
      }
      return throwError(() => error);
    }),
  );
};
