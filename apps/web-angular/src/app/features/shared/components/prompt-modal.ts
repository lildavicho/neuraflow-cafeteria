import { ChangeDetectionStrategy, Component, HostListener, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

/**
 * Modal de prompt que sustituye a `window.prompt()`.
 * Pensado para flujos como cobrar CxC, pagar CxP, ingresar notas, etc.
 *
 * Uso:
 *   <app-prompt-modal
 *     [open]="showPrompt()"
 *     title="Registrar cobro"
 *     label="Monto recibido"
 *     [initialValue]="defaultAmount"
 *     type="number"
 *     (confirm)="onConfirm($event)"
 *     (cancel)="showPrompt.set(false)"
 *   />
 */
@Component({
  selector: 'app-prompt-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  template: `
    @if (open()) {
      <div class="pm__backdrop" (click)="onCancel()">
        <div class="pm__dialog" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
          <header class="pm__head">
            <h3 class="pm__title">{{ title() }}</h3>
            @if (subtitle()) {
              <p class="pm__subtitle">{{ subtitle() }}</p>
            }
          </header>
          <form class="pm__body" (submit)="onConfirm(); $event.preventDefault()">
            @if (label()) {
              <label class="pm__label" [attr.for]="inputId">{{ label() }}</label>
            }
            <input
              #input
              [id]="inputId"
              class="pm__input"
              [type]="type()"
              [(ngModel)]="value"
              name="value"
              [placeholder]="placeholder() ?? ''"
              [min]="min()"
              [step]="step()"
              autofocus
            />
            @if (hint()) {
              <small class="pm__hint">{{ hint() }}</small>
            }
            <footer class="pm__actions">
              <button type="button" class="pm__btn pm__btn--ghost" (click)="onCancel()">
                {{ cancelLabel() }}
              </button>
              <button type="submit" class="pm__btn pm__btn--primary" [disabled]="!canConfirm()">
                {{ confirmLabel() }}
              </button>
            </footer>
          </form>
        </div>
      </div>
    }
  `,
  styles: `
    .pm__backdrop {
      position: fixed;
      inset: 0;
      background: rgba(3, 7, 18, 0.72);
      backdrop-filter: blur(6px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 2000;
      animation: pm-fade 160ms ease-out both;
      padding: 16px;
    }
    .pm__dialog {
      width: min(420px, 100%);
      background: var(--surface, rgba(17, 24, 39, 0.96));
      border: 1px solid var(--aurora-border, rgba(148, 163, 184, 0.22));
      border-radius: 14px;
      box-shadow: 0 30px 80px rgba(0, 0, 0, 0.45);
      overflow: hidden;
      animation: pm-pop 200ms ease-out both;
    }
    .pm__head {
      padding: 18px 20px 8px;
      border-bottom: 1px solid var(--aurora-border, rgba(148, 163, 184, 0.12));
    }
    .pm__title {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--text, rgba(226, 232, 240, 0.96));
      letter-spacing: -0.01em;
    }
    .pm__subtitle {
      margin: 4px 0 0;
      font-size: 12px;
      color: var(--aurora-dim, rgba(148, 163, 184, 0.72));
    }
    .pm__body { padding: 16px 20px 20px; display: flex; flex-direction: column; gap: 8px; }
    .pm__label {
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.02em;
      color: var(--aurora-dim, rgba(226, 232, 240, 0.72));
      text-transform: uppercase;
    }
    .pm__input {
      padding: 10px 12px;
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--aurora-border, rgba(148, 163, 184, 0.2));
      border-radius: 8px;
      color: var(--text, rgba(226, 232, 240, 0.96));
      font-size: 14px;
      transition: border-color 140ms ease, background 140ms ease;
    }
    .pm__input:focus {
      outline: none;
      border-color: var(--aurora-accent, rgba(129, 140, 248, 0.55));
      background: rgba(255, 255, 255, 0.06);
    }
    .pm__hint {
      font-size: 11px;
      color: var(--aurora-dim, rgba(148, 163, 184, 0.7));
    }
    .pm__actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      margin-top: 10px;
    }
    .pm__btn {
      padding: 9px 14px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 600;
      border: 1px solid transparent;
      cursor: pointer;
      transition: background 140ms ease, border-color 140ms ease;
    }
    .pm__btn--ghost {
      background: transparent;
      color: var(--aurora-dim, rgba(226, 232, 240, 0.78));
      border-color: var(--aurora-border, rgba(148, 163, 184, 0.2));
    }
    .pm__btn--ghost:hover { background: rgba(255, 255, 255, 0.04); }
    .pm__btn--primary {
      background: var(--aurora-accent, rgba(129, 140, 248, 0.9));
      color: white;
    }
    .pm__btn--primary:hover:not([disabled]) { background: rgba(129, 140, 248, 1); }
    .pm__btn[disabled] { opacity: 0.5; cursor: not-allowed; }
    @keyframes pm-fade { from { opacity: 0; } to { opacity: 1; } }
    @keyframes pm-pop {
      from { opacity: 0; transform: translateY(6px) scale(0.98); }
      to   { opacity: 1; transform: translateY(0) scale(1); }
    }
    @media (prefers-reduced-motion: reduce) {
      .pm__backdrop, .pm__dialog { animation: none; }
    }
  `,
})
export class PromptModal {
  private static uid = 0;
  readonly inputId = `pm-input-${++PromptModal.uid}`;

  readonly open = input<boolean>(false);
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
  readonly label = input<string | null>(null);
  readonly hint = input<string | null>(null);
  readonly placeholder = input<string | null>(null);
  readonly initialValue = input<string | number | null>(null);
  readonly type = input<'text' | 'number' | 'password'>('text');
  readonly min = input<number | null>(null);
  readonly step = input<number | string | null>(null);
  readonly confirmLabel = input<string>('Confirmar');
  readonly cancelLabel = input<string>('Cancelar');
  readonly required = input<boolean>(true);

  readonly confirm = output<string>();
  readonly cancel = output<void>();

  readonly value = signal<string>('');

  readonly canConfirm = computed(() => {
    if (!this.required()) return true;
    const raw = this.value();
    if (!raw || !raw.toString().trim()) return false;
    if (this.type() === 'number') {
      const num = Number(raw);
      if (Number.isNaN(num)) return false;
      const min = this.min();
      if (min != null && num < min) return false;
    }
    return true;
  });

  ngOnChanges(): void {
    if (this.open()) {
      const iv = this.initialValue();
      this.value.set(iv != null ? String(iv) : '');
    }
  }

  onConfirm(): void {
    if (!this.canConfirm()) return;
    this.confirm.emit(this.value().trim());
  }

  onCancel(): void {
    this.cancel.emit();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open()) this.onCancel();
  }
}
