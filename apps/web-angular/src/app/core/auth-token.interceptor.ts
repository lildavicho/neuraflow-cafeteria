import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthSession } from './services/auth-session';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const authSession = inject(AuthSession);
  const token = authSession.token();
  if (!token) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    }),
  );
};
