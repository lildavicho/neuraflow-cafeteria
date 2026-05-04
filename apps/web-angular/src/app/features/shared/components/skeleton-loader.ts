import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-skeleton-cards',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="skeleton-grid" role="status" aria-live="polite" aria-busy="true">
      <span class="skeleton__sr">Cargando contenido</span>
      @for (item of items(count); track item) {
        <article class="skeleton-card-panel" aria-hidden="true">
          <span class="skeleton skeleton-line skeleton-line--short"></span>
          <span class="skeleton skeleton-value"></span>
          <span class="skeleton skeleton-line"></span>
        </article>
      }
    </div>
  `,
  styles: `
    :host { display: contents; }

    .skeleton-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(var(--skeleton-min, 180px), 1fr));
      gap: 12px;
    }

    .skeleton-card-panel {
      display: grid;
      gap: 10px;
      padding: 20px;
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 4px;
      min-height: var(--skeleton-card-height, 112px);
    }

    .skeleton-line { height: 11px; width: 100%; }
    .skeleton-line--short { width: 46%; }
    .skeleton-value { height: 28px; width: 70%; }

    .skeleton__sr {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }
  `,
})
export class SkeletonCards {
  @Input() count = 4;

  protected items(count: number): number[] {
    return Array.from({ length: Math.max(1, count) }, (_, index) => index);
  }
}

@Component({
  selector: 'app-skeleton-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="skeleton-table" role="status" aria-live="polite" aria-busy="true" [style.--skeleton-columns]="columns">
      <span class="skeleton__sr">Cargando tabla</span>
      @for (row of items(rows); track row) {
        <div class="skeleton-table__row" aria-hidden="true">
          @for (column of items(columns); track column) {
            <span class="skeleton skeleton-line" [style.width.%]="columnWidth(column)"></span>
          }
        </div>
      }
    </div>
  `,
  styles: `
    :host { display: contents; }

    .skeleton-table {
      display: grid;
      gap: 4px;
      padding: 20px;
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 4px;
    }

    .skeleton-table__row {
      display: grid;
      grid-template-columns: repeat(var(--skeleton-columns, 4), minmax(0, 1fr));
      gap: 12px;
      align-items: center;
      min-height: 38px;
      padding: 9px 12px;
      border-radius: 3px;
      background: var(--surface-muted);
    }

    .skeleton-line { height: 11px; }

    .skeleton__sr {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }
  `,
})
export class SkeletonTable {
  @Input() rows = 8;
  @Input() columns = 4;

  protected items(count: number): number[] {
    return Array.from({ length: Math.max(1, count) }, (_, index) => index);
  }

  protected columnWidth(column: number): number {
    return column % 3 === 0 ? 72 : column % 2 === 0 ? 86 : 58;
  }
}
