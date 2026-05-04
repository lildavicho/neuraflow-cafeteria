import { CurrencyPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

export type ChartPoint = { label: string; value: number };

@Component({
  selector: 'app-line-chart',
  imports: [CurrencyPipe],
  template: `
    <div class="lc">
      @if (points().length < 2) {
        <div class="lc__empty">Sin datos suficientes para trazar la tendencia.</div>
      } @else {
        <div class="lc__y-axis">
          @for (tick of yTicks(); track tick.pos) {
            <span class="lc__y-tick" [style.bottom.%]="tick.pos">
              {{ tick.val | currency: 'USD':'symbol':'1.0-0' }}
            </span>
          }
        </div>

        <div class="lc__canvas-wrap">
          <svg class="lc__svg" viewBox="0 0 600 180" preserveAspectRatio="none">
            @for (tick of yTicks(); track tick.pos) {
              <line
                [attr.x1]="0"
                [attr.y1]="tick.svgY"
                [attr.x2]="600"
                [attr.y2]="tick.svgY"
                class="lc__grid"
              />
            }

            <path [attr.d]="areaPath()" class="lc__area" />
            <polyline [attr.points]="linePts()" class="lc__line" />

            @for (pt of svgPoints(); track pt.key) {
              <g class="lc__point">
                <circle [attr.cx]="pt.x" [attr.cy]="pt.y" r="6.5" class="lc__halo" />
                <circle [attr.cx]="pt.x" [attr.cy]="pt.y" r="3.75" class="lc__dot">
                  <title>{{ pt.label }}: {{ pt.value | currency: 'USD':'symbol':'1.2-2' }}</title>
                </circle>
              </g>
            }
          </svg>
        </div>

        <div class="lc__x-axis">
          @for (tick of xTicks(); track tick.key) {
            <span class="lc__x-tick" [style.left.%]="tick.left">{{ tick.label }}</span>
          }
        </div>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
      min-width: 0;
    }

    .lc {
      display: grid;
      grid-template-columns: 56px minmax(0, 1fr);
      grid-template-rows: minmax(0, 1fr) 34px;
      gap: 0;
      height: clamp(220px, 32vw, 280px);
      width: 100%;
      min-width: 0;
    }

    .lc__y-axis {
      grid-row: 1;
      grid-column: 1;
      position: relative;
      padding: 8px 0 12px;
      min-width: 0;
    }

    .lc__y-tick {
      position: absolute;
      right: 6px;
      font-size: 0.64rem;
      font-weight: 600;
      letter-spacing: 0.01em;
      color: var(--text-faint);
      transform: translateY(50%);
      white-space: nowrap;
      text-align: right;
    }

    .lc__canvas-wrap {
      grid-row: 1;
      grid-column: 2;
      overflow: hidden;
      min-width: 0;
      position: relative;
      padding: 8px 0 10px;
    }

    .lc__canvas-wrap::after {
      content: '';
      position: absolute;
      inset: 8px 0 10px;
      border-radius: 24px;
      pointer-events: none;
      background:
        linear-gradient(180deg, color-mix(in srgb, var(--surface-muted) 60%, transparent), transparent 35%),
        linear-gradient(90deg, transparent, color-mix(in srgb, var(--surface-muted) 30%, transparent), transparent);
      opacity: 0.55;
    }

    .lc__svg {
      width: 100%;
      height: 100%;
      display: block;
      position: relative;
      z-index: 1;
    }

    .lc__grid {
      stroke: var(--line);
      stroke-width: 0.7;
    }

    .lc__area {
      fill: color-mix(in srgb, var(--aurora-ghost) 96%, transparent);
    }

    .lc__line {
      fill: none;
      stroke: var(--aurora);
      stroke-width: 2.25;
      stroke-linejoin: round;
      stroke-linecap: round;
      filter: drop-shadow(0 8px 18px color-mix(in srgb, var(--aurora) 20%, transparent));
    }

    .lc__halo {
      fill: color-mix(in srgb, var(--aurora-ghost) 95%, transparent);
    }

    .lc__dot {
      fill: var(--aurora);
      stroke: var(--bg);
      stroke-width: 2;
    }

    .lc__x-axis {
      grid-row: 2;
      grid-column: 2;
      position: relative;
      padding-top: 6px;
      min-width: 0;
      overflow: visible;
    }

    .lc__x-tick {
      position: absolute;
      bottom: 0;
      transform: translateX(-50%);
      font-size: 0.64rem;
      font-weight: 600;
      letter-spacing: 0.01em;
      color: var(--text-faint);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      text-align: center;
      width: min(72px, 22vw);
    }

    .lc__empty {
      grid-column: 1 / -1;
      grid-row: 1 / -1;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      color: var(--text-faint);
      text-align: center;
      padding: 0 1rem;
    }

    @media (max-width: 720px) {
      .lc {
        grid-template-columns: 48px minmax(0, 1fr);
        grid-template-rows: minmax(0, 1fr) 40px;
        height: 220px;
      }

      .lc__x-tick {
        width: min(54px, 18vw);
        font-size: 0.6rem;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .lc__line {
        filter: none;
      }
    }
  `,
})
export class LineChart {
  readonly points = input.required<ChartPoint[]>();
  readonly currency = input(true);

  protected readonly maxVal = computed(() =>
    Math.max(1, ...this.points().map((point) => point.value)),
  );

  private readonly width = 600;
  private readonly height = 180;
  private readonly padding = 16;

  protected readonly svgPoints = computed(() => {
    const points = this.points();
    if (points.length < 2) return [];
    const max = this.maxVal();
    const usableHeight = this.height - this.padding * 2;
    return points.map((point, index) => ({
      x: this.padding + (index / (points.length - 1)) * (this.width - this.padding * 2),
      y: this.height - this.padding - (point.value / max) * usableHeight,
      label: point.label,
      value: point.value,
      key: `${point.label}-${index}`,
    }));
  });

  protected readonly linePts = computed(() =>
    this.svgPoints()
      .map((point) => `${point.x},${point.y}`)
      .join(' '),
  );

  protected readonly areaPath = computed(() => {
    const points = this.svgPoints();
    if (points.length < 2) return '';
    const base = this.height - this.padding;
    const first = points[0];
    const last = points[points.length - 1];
    return `M${first.x},${base} ${points.map((point) => `L${point.x},${point.y}`).join(' ')} L${last.x},${base} Z`;
  });

  protected readonly yTicks = computed(() => {
    const max = this.maxVal();
    return [0, 25, 50, 75, 100].map((pct) => ({
      pos: pct,
      val: (pct / 100) * max,
      svgY: this.height - this.padding - (pct / 100) * (this.height - this.padding * 2),
    }));
  });

  protected readonly xTicks = computed(() => {
    const points = this.points();
    if (points.length < 2) return [];
    const step = points.length <= 6 ? 1 : Math.ceil(points.length / 6);
    return points
      .map((point, index) => ({
        key: `${point.label}-${index}`,
        label: point.label,
        left: (index / (points.length - 1)) * 100,
        index,
      }))
      .filter((tick) => tick.index % step === 0 || tick.index === points.length - 1);
  });
}
