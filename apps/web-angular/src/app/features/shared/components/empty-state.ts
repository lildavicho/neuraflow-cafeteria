import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Empty state reutilizable para listados vacíos.
 * Uso:
 *   <app-empty-state title="Sin resultados" message="Ajusta los filtros" icon="📭" />
 */
@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="es">
      @if (icon()) {
        <div class="es__icon" aria-hidden="true">{{ icon() }}</div>
      }
      <h3 class="es__title">{{ title() }}</h3>
      @if (message()) {
        <p class="es__message">{{ message() }}</p>
      }
      <ng-content />
    </div>
  `,
  styles: `
    .es {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 10px;
      padding: 48px 24px;
      border: 1px dashed var(--aurora-border, rgba(148, 163, 184, 0.18));
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.02);
      text-align: center;
    }
    .es__icon {
      font-size: 32px;
      line-height: 1;
      opacity: 0.75;
    }
    .es__title {
      margin: 0;
      font-size: 15px;
      font-weight: 600;
      color: var(--text, rgba(226, 232, 240, 0.92));
      letter-spacing: -0.01em;
    }
    .es__message {
      margin: 0;
      font-size: 13px;
      color: var(--aurora-dim, rgba(148, 163, 184, 0.72));
      max-width: 52ch;
    }
  `,
})
export class EmptyState {
  readonly title = input.required<string>();
  readonly message = input<string | null>(null);
  readonly icon = input<string | null>(null);
}
