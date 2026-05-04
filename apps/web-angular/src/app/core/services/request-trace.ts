import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class RequestTrace {
  readonly current = signal('');

  nextClientTraceId(): string {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
      return crypto.randomUUID();
    }

    return `trace-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
  }

  remember(traceId?: string | null): void {
    if (!traceId?.trim()) {
      return;
    }
    this.current.set(traceId.trim());
  }
}
