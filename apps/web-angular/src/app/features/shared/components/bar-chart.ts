import { CurrencyPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

export type BarPoint = { label: string; value: number; value2?: number };

@Component({
  selector: 'app-bar-chart',
  imports: [CurrencyPipe],
  template: `
    <div class="bc">
      @if (!points().length) {
        <div class="bc__empty">Sin datos.</div>
      } @else {
        <div class="bc__scroll">
          <div class="bc__bars" [style.minWidth.px]="chartWidth()">
            @for (bar of bars(); track bar.label) {
              <div class="bc__bar-wrap" [title]="bar.label + ': ' + bar.value">
                <div class="bc__bar-group">
                  <div
                    class="bc__bar"
                    [style.height.%]="bar.pct"
                    [class.bc__bar--highlight]="bar.isMax"
                  >
                    @if (bar.pct > 18) {
                      <span class="bc__bar-val">{{ bar.value | currency: 'USD':'symbol':'1.0-0' }}</span>
                    }
                  </div>
                  @if (bar.pct2 !== undefined) {
                    <div
                      class="bc__bar bc__bar--secondary"
                      [style.height.%]="bar.pct2"
                    ></div>
                  }
                </div>
                <span class="bc__label">{{ bar.label }}</span>
              </div>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
      min-width: 0;
    }

    .bc {
      height: clamp(220px, 30vw, 260px);
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    .bc__scroll {
      flex: 1;
      min-width: 0;
      overflow-x: auto;
      overflow-y: hidden;
      padding-bottom: 2px;
    }

    .bc__scroll::-webkit-scrollbar {
      height: 5px;
    }

    .bc__scroll::-webkit-scrollbar-thumb {
      background: var(--line-strong);
      border-radius: 999px;
    }

    .bc__bars {
      position: relative;
      height: 100%;
      display: flex;
      align-items: flex-end;
      gap: 0.8rem;
      padding: 0.4rem 0 2rem;
      background:
        linear-gradient(180deg, color-mix(in srgb, var(--surface-muted) 58%, transparent), transparent 38%),
        repeating-linear-gradient(
          to top,
          transparent 0,
          transparent calc(25% - 1px),
          color-mix(in srgb, var(--line) 84%, transparent) calc(25% - 1px),
          color-mix(in srgb, var(--line) 84%, transparent) 25%
        );
      border-radius: 24px;
    }

    .bc__bar-wrap {
      flex: 1 1 0;
      min-width: 54px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: flex-end;
      height: 100%;
      position: relative;
    }

    .bc__bar-group {
      flex: 1;
      width: 100%;
      display: flex;
      align-items: flex-end;
      justify-content: center;
      gap: 0.25rem;
      min-height: 0;
    }

    .bc__bar {
      flex: 1 1 0;
      min-width: 0;
      min-height: 8px;
      background: color-mix(in srgb, var(--surface-strong) 88%, transparent);
      border: 1px solid color-mix(in srgb, var(--line-strong) 90%, transparent);
      border-radius: 18px 18px 8px 8px;
      position: relative;
      transition:
        height 500ms var(--ease-out),
        background-color var(--dur-fast) ease,
        border-color var(--dur-fast) ease,
        transform var(--dur-fast) var(--ease-out);
      display: flex;
      align-items: flex-start;
      justify-content: center;
      overflow: hidden;
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
    }

    .bc__bar:hover {
      transform: translateY(-2px);
    }

    .bc__bar--highlight {
      background: linear-gradient(
        180deg,
        color-mix(in srgb, var(--aurora) 34%, white),
        color-mix(in srgb, var(--aurora) 88%, transparent)
      );
      border-color: var(--aurora-border);
      box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.14),
        0 16px 28px color-mix(in srgb, var(--aurora) 18%, transparent);
    }

    .bc__bar--secondary {
      background: linear-gradient(
        180deg,
        color-mix(in srgb, var(--aurora-light) 28%, white),
        color-mix(in srgb, var(--aurora-light) 72%, transparent)
      );
      border-color: color-mix(in srgb, var(--aurora-light) 38%, var(--line));
    }

    .bc__bar-val {
      font-size: 0.58rem;
      font-weight: 700;
      color: var(--text-strong);
      padding-top: 0.45rem;
      white-space: nowrap;
      writing-mode: vertical-rl;
      transform: rotate(180deg);
      letter-spacing: 0.02em;
      opacity: 0.82;
    }

    .bc__label {
      position: absolute;
      bottom: -1.55rem;
      font-size: 0.64rem;
      font-weight: 600;
      color: var(--text-faint);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 100%;
      text-align: center;
    }

    .bc__empty {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      font-size: 12px;
      color: var(--text-faint);
    }

    @media (max-width: 720px) {
      .bc {
        height: 220px;
      }

      .bc__bars {
        gap: 0.65rem;
      }

      .bc__bar-wrap {
        min-width: 48px;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .bc__bar {
        transition: none;
      }
    }
  `,
})
export class BarChart {
  readonly points = input.required<BarPoint[]>();

  protected readonly maxVal = computed(() =>
    Math.max(1, ...this.points().map((point) => Math.max(point.value, point.value2 ?? 0))),
  );

  protected readonly bars = computed(() => {
    const max = this.maxVal();
    const points = this.points();
    const maxPoint = points.reduce((current, next) => (current.value > next.value ? current : next), points[0]);
    return points.map((point) => ({
      label: point.label,
      value: point.value,
      pct: Math.max(3, (point.value / max) * 100),
      pct2: point.value2 !== undefined ? Math.max(3, (point.value2 / max) * 100) : undefined,
      isMax: point === maxPoint,
    }));
  });

  protected readonly chartWidth = computed(() =>
    Math.max(320, this.points().length * (this.points().some((point) => point.value2 !== undefined) ? 68 : 58)),
  );
}
