import { Component, input } from '@angular/core';

@Component({
  selector: 'app-request-feedback',
  template: `
    <div class="rfb" [attr.data-tone]="tone()">
      <div class="rfb__icon" aria-hidden="true">
        @switch (tone()) {
          @case ('success') {
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          }
          @case ('error') {
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
            </svg>
          }
          @case ('warning') {
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
            </svg>
          }
          @default {
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
            </svg>
          }
        }
      </div>
      <div class="rfb__body">
        <p class="rfb__msg">{{ message() }}</p>
        @if (traceId() && !hideTrace()) {
          <small class="rfb__trace">Referencia técnica: {{ traceShort() }}</small>
        }
      </div>
    </div>
  `,
  styles: `
    :host {
      display: block;
      min-width: 0;
    }

    .rfb {
      display: flex;
      align-items: flex-start;
      gap: 0.9rem;
      padding: 0.95rem 1rem;
      border-radius: 20px;
      border: 1px solid var(--line-subtle);
      background:
        linear-gradient(135deg, color-mix(in srgb, var(--surface-muted) 88%, transparent), transparent 92%),
        var(--bg-panel);
      box-shadow: var(--shadow-xs);
      transition:
        border-color var(--dur-fast) var(--ease-in-out),
        background-color var(--dur-fast) var(--ease-in-out),
        transform var(--dur-fast) var(--ease-out);
    }

    .rfb:hover {
      transform: translateY(-1px);
    }

    .rfb__icon {
      width: 2rem;
      height: 2rem;
      flex-shrink: 0;
      margin-top: 1px;
      display: grid;
      place-items: center;
      border-radius: 999px;
      background: color-mix(in srgb, var(--surface) 82%, transparent);
      border: 1px solid color-mix(in srgb, var(--line-subtle) 86%, transparent);
    }

    .rfb__icon svg {
      width: 1rem;
      height: 1rem;
    }

    .rfb__body {
      display: grid;
      gap: 0.25rem;
      min-width: 0;
    }

    .rfb__msg {
      font-size: 0.83rem;
      font-weight: 500;
      line-height: 1.55;
      margin: 0;
      color: var(--text);
    }

    .rfb__trace {
      font-size: 0.7rem;
      font-weight: 600;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: var(--text-faint);
    }

    .rfb[data-tone='success'] {
      border-color: var(--ok-border);
      background:
        linear-gradient(135deg, color-mix(in srgb, var(--ok-bg) 92%, transparent), transparent 88%),
        var(--bg-panel);
    }

    .rfb[data-tone='success'] .rfb__icon {
      color: var(--ok);
      background: color-mix(in srgb, var(--ok-bg) 88%, transparent);
      border-color: color-mix(in srgb, var(--ok-border) 88%, transparent);
    }

    .rfb[data-tone='success'] .rfb__msg {
      color: var(--ok);
    }

    .rfb[data-tone='error'] {
      border-color: var(--danger-border);
      background:
        linear-gradient(135deg, color-mix(in srgb, var(--danger-bg) 92%, transparent), transparent 88%),
        var(--bg-panel);
    }

    .rfb[data-tone='error'] .rfb__icon {
      color: var(--danger);
      background: color-mix(in srgb, var(--danger-bg) 88%, transparent);
      border-color: color-mix(in srgb, var(--danger-border) 88%, transparent);
    }

    .rfb[data-tone='error'] .rfb__msg {
      color: var(--danger);
    }

    .rfb[data-tone='warning'] {
      border-color: var(--warn-border);
      background:
        linear-gradient(135deg, color-mix(in srgb, var(--warn-bg) 92%, transparent), transparent 88%),
        var(--bg-panel);
    }

    .rfb[data-tone='warning'] .rfb__icon {
      color: var(--warn);
      background: color-mix(in srgb, var(--warn-bg) 88%, transparent);
      border-color: color-mix(in srgb, var(--warn-border) 88%, transparent);
    }

    .rfb[data-tone='warning'] .rfb__msg {
      color: var(--warn);
    }

    .rfb[data-tone='info'] {
      border-color: var(--info-border);
      background:
        linear-gradient(135deg, color-mix(in srgb, var(--info-bg) 92%, transparent), transparent 88%),
        var(--bg-panel);
    }

    .rfb[data-tone='info'] .rfb__icon {
      color: var(--info);
      background: color-mix(in srgb, var(--info-bg) 88%, transparent);
      border-color: color-mix(in srgb, var(--info-border) 88%, transparent);
    }

    .rfb[data-tone='info'] .rfb__msg {
      color: var(--info);
    }
  `,
})
export class RequestFeedback {
  readonly tone = input<'success' | 'error' | 'warning' | 'info'>('info');
  readonly message = input.required<string>();
  readonly traceId = input<string>();
  readonly hideTrace = input<boolean>(false);

  protected traceShort(): string {
    const trace = this.traceId();
    if (!trace) return '';
    const compact = trace.replace(/-/g, '');
    return compact.length > 8 ? compact.slice(0, 8) : compact;
  }
}
