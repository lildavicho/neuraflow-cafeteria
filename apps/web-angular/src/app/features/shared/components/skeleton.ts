import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Placeholder animado para estados de carga.
 * Uso: <app-skeleton [lines]="5" width="100%" height="18px" />
 * O como tabla: <app-skeleton variant="table" [rows]="5" [cols]="4" />
 */
@Component({
  selector: 'app-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (variant() === 'table') {
      <div class="sk-table" role="status" aria-busy="true" aria-label="Cargando datos">
        <div class="sk-row sk-row--header">
          @for (c of colsArray(); track c) {
            <span class="sk-cell sk-cell--head"></span>
          }
        </div>
        @for (r of rowsArray(); track r) {
          <div class="sk-row">
            @for (c of colsArray(); track c) {
              <span class="sk-cell"></span>
            }
          </div>
        }
      </div>
    } @else {
      <div class="sk-stack" role="status" aria-busy="true" aria-label="Cargando">
        @for (l of linesArray(); track l) {
          <span class="sk-line" [style.width]="width()" [style.height]="height()"></span>
        }
      </div>
    }
  `,
  styles: `
    :host { display: block; width: 100%; }
    .sk-line, .sk-cell {
      display: block;
      border-radius: 6px;
      background: linear-gradient(
        90deg,
        rgba(148, 163, 184, 0.08) 0%,
        rgba(148, 163, 184, 0.22) 50%,
        rgba(148, 163, 184, 0.08) 100%
      );
      background-size: 200% 100%;
      animation: sk-shimmer 1.4s ease-in-out infinite;
    }
    .sk-stack { display: flex; flex-direction: column; gap: 10px; }
    .sk-table { display: flex; flex-direction: column; gap: 8px; }
    .sk-row { display: grid; grid-auto-flow: column; grid-auto-columns: 1fr; gap: 12px; }
    .sk-cell { height: 16px; }
    .sk-cell--head { height: 12px; opacity: 0.75; }
    @keyframes sk-shimmer {
      0% { background-position: 100% 0; }
      100% { background-position: -100% 0; }
    }
    @media (prefers-reduced-motion: reduce) {
      .sk-line, .sk-cell { animation: none; }
    }
  `,
})
export class Skeleton {
  readonly variant = input<'stack' | 'table'>('stack');
  readonly lines  = input(3);
  readonly rows   = input(5);
  readonly cols   = input(4);
  readonly width  = input('100%');
  readonly height = input('16px');

  protected linesArray() { return Array.from({ length: this.lines() }, (_, i) => i); }
  protected rowsArray()  { return Array.from({ length: this.rows()  }, (_, i) => i); }
  protected colsArray()  { return Array.from({ length: this.cols()  }, (_, i) => i); }
}
