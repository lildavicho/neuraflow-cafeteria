import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

/**
 * Paginador reutilizable con estética aurora.
 * Entradas: total de elementos, tamaño de página, página actual (0-based).
 * Emite `pageChange` cuando el usuario navega.
 */
@Component({
  selector: 'app-paginator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (totalElements() > 0 && totalPages() > 0) {
      <nav class="pg" aria-label="Paginación">
        <span class="pg__info">
          {{ rangeStart() }}–{{ rangeEnd() }} de {{ totalElements() }}
        </span>
        <div class="pg__controls">
          <button
            type="button"
            class="pg__btn"
            [disabled]="page() === 0"
            (click)="go(0)"
            aria-label="Primera página"
          >«</button>
          <button
            type="button"
            class="pg__btn"
            [disabled]="page() === 0"
            (click)="go(page() - 1)"
            aria-label="Página anterior"
          >‹</button>
          <span class="pg__page">
            Página {{ page() + 1 }} de {{ totalPages() }}
          </span>
          <button
            type="button"
            class="pg__btn"
            [disabled]="page() >= totalPages() - 1"
            (click)="go(page() + 1)"
            aria-label="Página siguiente"
          >›</button>
          <button
            type="button"
            class="pg__btn"
            [disabled]="page() >= totalPages() - 1"
            (click)="go(totalPages() - 1)"
            aria-label="Última página"
          >»</button>
        </div>
      </nav>
    }
  `,
  styles: `
    .pg {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 12px 16px;
      margin-top: 8px;
      border-top: 1px solid var(--line, #e4e6eb);
      font-size: 12.5px;
      color: var(--text-muted, #5e6976);
      flex-wrap: wrap;
    }
    .pg__info {
      letter-spacing: 0.01em;
      font-weight: 500;
      font-variant-numeric: tabular-nums;
    }
    .pg__controls {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
    .pg__btn {
      min-width: 32px;
      height: 32px;
      padding: 0 10px;
      border-radius: 6px;
      border: 1px solid var(--line-strong, #d4d8df);
      background: var(--surface, #fff);
      color: var(--text, #1d2430);
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      transition: background 140ms ease, border-color 140ms ease, color 140ms ease;
    }
    .pg__btn:hover:not([disabled]) {
      background: var(--aurora-ghost, rgba(99, 102, 241, 0.08));
      border-color: var(--aurora-border);
      color: var(--aurora);
    }
    .pg__btn[disabled] { opacity: 0.4; cursor: not-allowed; }
    .pg__page {
      padding: 0 8px;
      font-variant-numeric: tabular-nums;
      font-weight: 500;
      color: var(--text);
    }
  `,
})
export class Paginator {
  readonly page = input.required<number>();
  readonly pageSize = input.required<number>();
  readonly totalElements = input.required<number>();

  readonly pageChange = output<number>();

  readonly totalPages = computed(() => {
    const size = Math.max(1, this.pageSize());
    return Math.max(1, Math.ceil(this.totalElements() / size));
  });

  readonly rangeStart = computed(() => {
    if (this.totalElements() === 0) return 0;
    return this.page() * this.pageSize() + 1;
  });

  readonly rangeEnd = computed(() =>
    Math.min(this.totalElements(), (this.page() + 1) * this.pageSize()),
  );

  go(target: number): void {
    const clamped = Math.max(0, Math.min(target, this.totalPages() - 1));
    if (clamped !== this.page()) {
      this.pageChange.emit(clamped);
    }
  }
}
