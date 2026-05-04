import { Component, inject } from '@angular/core';
import { ToastService } from '../../../core/services/toast';

@Component({
  selector: 'app-toast-outlet',
  template: `
    <div class="to" aria-live="polite" aria-atomic="false">
      @for (toast of toastSvc.toasts(); track toast.id) {
        <div class="to__item" [attr.data-tone]="toast.tone" role="alert">
          <div class="to__icon">
            @switch (toast.tone) {
              @case ('success') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              }
              @case ('error') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
                </svg>
              }
              @case ('warning') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                </svg>
              }
              @default {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
                </svg>
              }
            }
          </div>
          <div class="to__body">
            <strong class="to__title">{{ toast.title }}</strong>
            @if (toast.message) {
              <span class="to__msg">{{ toast.message }}</span>
            }
          </div>
          <button type="button" class="to__close" (click)="toastSvc.dismiss(toast.id)" aria-label="Cerrar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .to {
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 10px;
      pointer-events: none;
      width: 340px;
    }

    .to__item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 16px;
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 10px;
      box-shadow: 0 8px 32px rgba(0,0,0,0.12);
      pointer-events: all;
      animation: slideUp var(--dur-normal) var(--ease-out) both;
      transition: opacity var(--dur-fast) ease;
      border-left: 3px solid var(--line-strong);
    }

    .to__item[data-tone="success"] { border-left-color: var(--ok); }
    .to__item[data-tone="error"]   { border-left-color: var(--danger); }
    .to__item[data-tone="warning"] { border-left-color: var(--warn); }
    .to__item[data-tone="info"]    { border-left-color: var(--info); }

    .to__icon {
      flex-shrink: 0;
      width: 20px;
      height: 20px;
      margin-top: 1px;
    }

    .to__icon svg { width: 100%; height: 100%; }

    [data-tone="success"] .to__icon { color: var(--ok); }
    [data-tone="error"]   .to__icon { color: var(--danger); }
    [data-tone="warning"] .to__icon { color: var(--warn); }
    [data-tone="info"]    .to__icon { color: var(--info); }

    .to__body {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .to__title {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-strong);
      line-height: 1.3;
    }

    .to__msg {
      font-size: 12px;
      color: var(--text-muted);
      line-height: 1.4;
    }

    .to__close {
      flex-shrink: 0;
      width: 20px;
      height: 20px;
      background: none;
      border: none;
      cursor: pointer;
      color: var(--text-faint);
      padding: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      transition: color var(--dur-fast) ease, background var(--dur-fast) ease;
    }

    .to__close:hover {
      color: var(--text);
      background: var(--surface);
    }

    .to__close svg { width: 14px; height: 14px; }
  `,
})
export class ToastOutlet {
  protected readonly toastSvc = inject(ToastService);
}
