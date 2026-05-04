import { Component, input, output } from '@angular/core';
import { ExportFormat } from '../../../core/services/erp-api';

@Component({
  selector: 'app-export-actions',
  template: `
    <div class="exp" role="group" aria-label="Exportar datos">
      @for (item of options; track item.format) {
        <button
          type="button"
          class="exp__btn"
          [class.exp__btn--active]="exporting() === item.format"
          [class.exp__btn--busy]="exporting() === item.format"
          [disabled]="disabled() || exporting() !== null"
          [title]="item.tooltip"
          [attr.aria-label]="item.tooltip"
          (click)="request(item.format)"
        >
          <svg class="exp__icon" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3v12m0 0l-4-4m4 4l4-4M5 21h14" />
          </svg>
          <span>{{ exporting() === item.format ? 'Exportando…' : item.label }}</span>
        </button>
      }
    </div>
  `,
  styles: `
    :host { display: block; }

    .exp {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      justify-content: flex-end;
    }

    .exp__btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 7px 12px;
      background: var(--surface, #fff);
      border: 1px solid var(--line-strong, #d4d8df);
      color: var(--text, #1d2430);
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      border-radius: 6px;
      cursor: pointer;
      transition: border-color 150ms ease, color 150ms ease, background 150ms ease, box-shadow 150ms ease;
      box-shadow: var(--shadow-xs, 0 1px 2px rgba(15, 23, 42, 0.04));
    }

    .exp__btn:hover:not(:disabled) {
      border-color: var(--aurora-border);
      color: var(--aurora);
      background: var(--aurora-ghost);
    }

    .exp__btn--active,
    .exp__btn--busy {
      border-color: var(--aurora-border);
      color: var(--aurora);
      background: var(--aurora-ghost);
    }

    .exp__btn:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }

    .exp__icon {
      width: 14px;
      height: 14px;
      flex-shrink: 0;
    }

    .exp__btn--busy .exp__icon {
      animation: exp-spin 1s linear infinite;
    }

    @keyframes exp-spin {
      from { transform: rotate(0deg); }
      to   { transform: rotate(360deg); }
    }
  `,
})
export class ExportActions {
  readonly disabled = input(false);
  readonly exporting = input<ExportFormat | null>(null);
  readonly exportRequested = output<ExportFormat>();

  protected readonly options: ReadonlyArray<{ format: ExportFormat; label: string; tooltip: string }> = [
    { format: 'xlsx', label: 'XLSX', tooltip: 'Descargar Excel (XLSX)' },
    { format: 'csv',  label: 'CSV',  tooltip: 'Descargar CSV' },
    { format: 'pdf',  label: 'PDF',  tooltip: 'Descargar PDF' },
  ];

  protected request(format: ExportFormat): void {
    if (this.disabled()) {
      return;
    }
    this.exportRequested.emit(format);
  }
}
