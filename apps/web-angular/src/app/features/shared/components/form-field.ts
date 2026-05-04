import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { ValidationMessages } from '../validators/validators';

/**
 * Envoltorio de control con label, hint opcional y render de errores inline.
 * Uso:
 *   <app-form-field label="Nombre" [control]="form.controls.name">
 *     <input ucFormControl formControlName="name" />
 *   </app-form-field>
 */
@Component({
  selector: 'app-form-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="ff" [class.ff--invalid]="showError()">
      @if (label(); as lbl) {
        <label class="ff__label">
          {{ lbl }}
          @if (required()) {
            <span class="ff__req" aria-hidden="true">*</span>
          }
        </label>
      }
      <div class="ff__control">
        <ng-content />
      </div>
      @if (hint() && !showError()) {
        <small class="ff__hint">{{ hint() }}</small>
      }
      @if (showError()) {
        <small class="ff__error" role="alert">{{ errorMessage() }}</small>
      }
    </div>
  `,
  styles: `
    .ff {
      display: flex;
      flex-direction: column;
      gap: 6px;
      min-width: 0;
    }
    .ff__label {
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.02em;
      color: var(--aurora-dim, rgba(226, 232, 240, 0.72));
      text-transform: uppercase;
    }
    .ff__req { color: var(--danger, #f87171); margin-left: 2px; }
    .ff__control { display: flex; flex-direction: column; }
    .ff__control ::ng-deep input,
    .ff__control ::ng-deep select,
    .ff__control ::ng-deep textarea {
      width: 100%;
      padding: 10px 12px;
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--aurora-border, rgba(148, 163, 184, 0.16));
      border-radius: 8px;
      color: var(--text, rgba(226, 232, 240, 0.96));
      font-size: 13px;
      transition: border-color 140ms ease, background 140ms ease;
    }
    .ff__control ::ng-deep input:focus,
    .ff__control ::ng-deep select:focus,
    .ff__control ::ng-deep textarea:focus {
      outline: none;
      border-color: var(--aurora-accent, rgba(129, 140, 248, 0.55));
      background: rgba(255, 255, 255, 0.06);
    }
    .ff--invalid ::ng-deep input,
    .ff--invalid ::ng-deep select,
    .ff--invalid ::ng-deep textarea {
      border-color: var(--danger, #f87171);
    }
    .ff__hint {
      font-size: 11px;
      color: var(--aurora-dim, rgba(148, 163, 184, 0.7));
    }
    .ff__error {
      font-size: 11px;
      color: var(--danger, #f87171);
      font-weight: 500;
    }
  `,
})
export class FormField {
  readonly label = input<string | null>(null);
  readonly hint = input<string | null>(null);
  readonly required = input<boolean>(false);
  readonly control = input<AbstractControl | null>(null);

  readonly showError = computed(() => {
    const ctrl = this.control();
    if (!ctrl) return false;
    return ctrl.invalid && (ctrl.touched || ctrl.dirty);
  });

  readonly errorMessage = computed(() => {
    const ctrl = this.control();
    return ctrl ? ValidationMessages.describe(ctrl.errors) : null;
  });
}
