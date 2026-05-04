import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, isDevMode, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withViewTransitions } from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';
import { authErrorInterceptor } from './core/auth-error.interceptor';
import { authTokenInterceptor } from './core/auth-token.interceptor';
import { requestTraceInterceptor } from './core/request-trace.interceptor';
import { tenantContextInterceptor } from './core/tenant-context.interceptor';
import { routes } from './routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withViewTransitions({ skipInitialTransition: true })),
    provideHttpClient(
      withInterceptors([
        requestTraceInterceptor,
        tenantContextInterceptor,
        authTokenInterceptor,
        authErrorInterceptor,
      ]),
    ),
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000',
    }),
  ],
};
