import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { ApiErrorPayload } from '../models/api-error';
import { UiFeedback } from '../models/ui-feedback';
import { RequestTrace } from './request-trace';

@Injectable({ providedIn: 'root' })
export class HttpFeedback {
  private readonly requestTrace = inject(RequestTrace);

  fromError(error: unknown, fallback: string): UiFeedback {
    if (!(error instanceof HttpErrorResponse)) {
      return {
        tone: 'error',
        message: fallback,
        traceId: this.requestTrace.current() || undefined,
      };
    }

    const payload = this.extractPayload(error);
    const traceId =
      payload?.traceId || error.headers.get('X-Trace-Id') || this.requestTrace.current() || undefined;

    if (payload?.validationErrors && Object.keys(payload.validationErrors).length) {
      return {
        tone: 'error',
        message: Object.values(payload.validationErrors).join(' '),
        traceId,
        code: payload.code,
        status: error.status,
      };
    }

    if (error.status === 403 || payload?.code === 'ACCESS_DENIED') {
      return {
        tone: 'warning',
        message: 'Tu acceso actual no incluye esta sección o esta acción.',
        traceId,
        code: payload?.code,
        status: error.status,
      };
    }

    if (error.status === 503 || payload?.code === 'SERVICE_UNAVAILABLE') {
      return {
        tone: 'warning',
        message: payload?.message || 'Este servicio no está disponible por ahora. Intenta nuevamente en unos minutos.',
        traceId,
        code: payload?.code,
        status: error.status,
      };
    }

    if (error.status === 401) {
      return {
        tone: 'warning',
        message: payload?.message || 'Tu sesión venció o las credenciales no son correctas.',
        traceId,
        code: payload?.code,
        status: error.status,
      };
    }

    if (error.status === 0) {
      return {
        tone: 'error',
        message: 'No pudimos conectar el sistema en este momento. Revisa tu conexión e inténtalo de nuevo.',
        traceId,
        status: error.status,
      };
    }

    return {
      tone: 'error',
      message: payload?.message || fallback,
      traceId,
      code: payload?.code,
      status: error.status,
    };
  }

  success(message: string): UiFeedback {
    return {
      tone: 'success',
      message,
      traceId: this.requestTrace.current() || undefined,
    };
  }

  info(message: string): UiFeedback {
    return {
      tone: 'info',
      message,
      traceId: this.requestTrace.current() || undefined,
    };
  }

  warning(message: string): UiFeedback {
    return {
      tone: 'warning',
      message,
      traceId: this.requestTrace.current() || undefined,
    };
  }

  private extractPayload(error: HttpErrorResponse): ApiErrorPayload | null {
    if (error.error && typeof error.error === 'object') {
      return error.error as ApiErrorPayload;
    }
    return null;
  }
}
