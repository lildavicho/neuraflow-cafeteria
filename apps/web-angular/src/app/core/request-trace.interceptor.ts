import { HttpErrorResponse, HttpEventType, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import { RequestTrace } from './services/request-trace';

export const requestTraceInterceptor: HttpInterceptorFn = (req, next) => {
  const requestTrace = inject(RequestTrace);
  const traceId = requestTrace.nextClientTraceId();

  return next(
    req.clone({
      setHeaders: {
        'X-Trace-Id': traceId,
      },
    }),
  ).pipe(
    tap({
      next: (event) => {
        if (event.type === HttpEventType.Response) {
          requestTrace.remember(event.headers.get('X-Trace-Id') || traceId);
        }
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse) {
          requestTrace.remember(error.headers.get('X-Trace-Id') || error.error?.traceId || traceId);
          return;
        }
        requestTrace.remember(traceId);
      },
    }),
  );
};
