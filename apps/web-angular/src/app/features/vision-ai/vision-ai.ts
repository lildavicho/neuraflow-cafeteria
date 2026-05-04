import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, of, timeout } from 'rxjs';
import { UiFeedback } from '../../core/models/ui-feedback';
import { HttpFeedback } from '../../core/services/http-feedback';
import {
  VisionConversionDto,
  VisionFootfallDto,
  VisionInsightsDto,
  VisionPeakHoursDto,
  VisionPredictionsDto,
  ErpApi,
} from '../../core/services/erp-api';
import { RequestFeedback } from '../shared/components/request-feedback';
import { SkeletonCards, SkeletonTable } from '../shared/components/skeleton-loader';

@Component({
  selector: 'app-vision-ai',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, CurrencyPipe, DatePipe, DecimalPipe, RequestFeedback, SkeletonCards, SkeletonTable],
  template: `
    <section class="page">
      <header class="page__header">
        <div>
          <span class="page__eyebrow">Visión comercial</span>
          <h1>Mide afluencia, conversión y comportamiento del local con lectura clara</h1>
          <p>Consulta el flujo de personas, el rendimiento por hora y las predicciones del negocio con foco en decisiones, no en términos técnicos.</p>
        </div>
        <div class="filters">
          <input [(ngModel)]="from" type="datetime-local" class="field" />
          <input [(ngModel)]="to" type="datetime-local" class="field" />
          <button type="button" class="page__button" [disabled]="loading()" (click)="load(true)">Consultar</button>
        </div>
      </header>

      @if (feedback(); as notice) {
        <app-request-feedback
          [tone]="notice.tone"
          [message]="notice.message"
          [traceId]="notice.traceId"
        />
      }

      <div class="stats-grid">
        <article class="stat-card">
          <span>Personas detectadas</span>
          <strong>{{ footfall()?.totalPeople ?? 0 }}</strong>
          <small>{{ footfall()?.dataOrigin || 'Sin datos' }}</small>
        </article>
        <article class="stat-card">
          <span>Conversión</span>
          <strong>{{ conversion()?.conversionRate ?? 0 | number: '1.0-2' }}%</strong>
          <small>{{ conversion()?.closedSales ?? 0 }} ventas cerradas</small>
        </article>
        <article class="stat-card">
          <span>Ticket promedio</span>
          <strong>{{ conversion()?.averageTicket ?? 0 | currency: 'USD' : 'symbol' : '1.2-2' }}</strong>
          <small>{{ conversion()?.dataOrigin || 'Sin datos' }}</small>
        </article>
        <article class="stat-card">
          <span>Fuente</span>
          <strong>{{ hasDegradedMode() ? 'Limitada' : 'Completa' }}</strong>
          <small>{{ degradedSource() }}</small>
        </article>
      </div>

      @if (loading()) {
        <app-skeleton-cards [count]="4" />
        <app-skeleton-table [rows]="5" [columns]="4" />
      } @else if (allTimedOut()) {
        <div class="vision-empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6l3 1.5M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <p>No hay datos disponibles por ahora.</p>
          <small>Los servicios de visión tardaron demasiado en responder. Inténtalo nuevamente.</small>
          <button type="button" class="page__button" (click)="load(true)">Reintentar</button>
        </div>
      } @else {
        <div class="layout">
          <section class="card">
            <header class="card__header">
              <div>
                <span class="page__eyebrow">Horas pico</span>
                <h2>Momentos con más tráfico</h2>
              </div>
            </header>

            @if (peakHours()?.peakHours?.length) {
              <div class="table">
                <div class="table__head">
                  <span>Ranking</span>
                  <span>Hora</span>
                  <span>Personas</span>
                  <span>Conversión</span>
                </div>
                @for (hour of peakHours()?.peakHours ?? []; track hour.rank) {
                  <div class="table__row">
                    <span>#{{ hour.rank }}</span>
                    <span>{{ hour.bucketStart | date: 'short' }}</span>
                    <span>{{ hour.peopleCount }}</span>
                    <span>{{ hour.conversionRate | number: '1.0-2' }}%</span>
                  </div>
                }
              </div>
            } @else {
              <p class="page__state">No hay horas pico disponibles para este rango.</p>
            }
          </section>

          <section class="card">
            <header class="card__header">
              <div>
                <span class="page__eyebrow">Insights</span>
                <h2>Señales accionables</h2>
              </div>
            </header>

            @if (insights()?.insights?.length) {
              <div class="insight-list">
                @for (insight of insights()?.insights ?? []; track insight) {
                  <article class="insight-card">{{ insight }}</article>
                }
              </div>
            } @else {
              <p class="page__state">No hay recomendaciones disponibles para este rango.</p>
            }
          </section>
        </div>

        <section class="card">
          <header class="card__header">
            <div>
              <span class="page__eyebrow">Predicción</span>
              <h2>Comportamiento esperado</h2>
            </div>
          </header>

          @if (predictions()?.predictions?.length) {
            <div class="table">
              <div class="table__head table__head--prediction">
                <span>Fecha</span>
                <span>Visitantes</span>
                <span>Conversión esperada</span>
                <span>Confianza</span>
              </div>
              @for (prediction of predictions()?.predictions ?? []; track prediction.bucketStart) {
                <div class="table__row table__row--prediction">
                  <span>{{ prediction.bucketStart | date: 'mediumDate' }}</span>
                  <span>{{ prediction.expectedVisitors | number: '1.0-0' }}</span>
                  <span>{{ prediction.expectedConversionRate | number: '1.0-2' }}%</span>
                  <span>{{ prediction.confidenceBand || 'Media' }}</span>
                </div>
              }
            </div>
          } @else {
            <p class="page__state">No hay predicciones disponibles para este rango.</p>
          }
        </section>
      }
    </section>
  `,
  styles: `
    :host { display: contents; }

    .page { display: grid; gap: 24px; padding: 32px; }

    .page__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; }

    .page__eyebrow {
      display: block;
      font-size: 10px;
      letter-spacing: 0.22em;
      text-transform: uppercase;
      font-weight: 500;
      color: var(--aurora);
      margin-bottom: 8px;
    }

    .page__header h1 {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 26px;
      font-weight: 400;
      color: var(--text-strong);
      margin: 0 0 6px;
      letter-spacing: -0.01em;
    }

    .page__header p { font-size: 13px; font-weight: 300; color: var(--text-muted); margin: 0; line-height: 1.6; }

    .page__actions { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }

    .page__button {
      padding: 10px 20px;
      background: transparent;
      border: 1px solid var(--aurora-border);
      color: var(--aurora);
      font-size: 11px;
      font-weight: 500;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      border-radius: 2px;
      cursor: pointer;
      transition: background 200ms ease, border-color 200ms ease;
    }

    .page__button:hover:not(:disabled) { background: var(--aurora-ghost); border-color: var(--aurora); }
    .page__button:disabled { opacity: 0.35; cursor: not-allowed; }

    .field { background: transparent; border: none; border-bottom: 1px solid var(--line); color: var(--text); padding: 9px 0; font-size: 13px; font-weight: 300; outline: none; transition: border-color 200ms ease; }
    .field::placeholder { color: var(--text-faint); }
    .field:focus { border-bottom-color: var(--aurora-border); }

    .filters { display: flex; gap: 16px; flex-wrap: wrap; align-items: flex-end; }

    .page__state { margin: 0; padding: 11px 14px; border-radius: 3px; border: 1px solid var(--line-subtle); background: var(--surface-muted); font-size: 12.5px; color: var(--text-muted); }

    .vision-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      padding: 56px 24px;
      border: 1px dashed var(--line-strong);
      border-radius: 12px;
      background: var(--surface-muted);
      text-align: center;
    }
    .vision-empty svg { width: 36px; height: 36px; color: var(--text-faint); }
    .vision-empty p { margin: 0; font-size: 14px; font-weight: 600; color: var(--text-strong); }
    .vision-empty small { font-size: 12px; color: var(--text-muted); }

    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 10px; }
    .stat-card { padding: 18px 20px; background: var(--bg-panel); border: 1px solid var(--line); border-radius: 4px; }
    .stat-card span { display: block; font-size: 10px; letter-spacing: 0.18em; text-transform: uppercase; color: var(--text-faint); margin-bottom: 8px; }
    .stat-card strong { display: block; font-family: 'Cormorant Garamond', Georgia, serif; font-size: 26px; font-weight: 400; color: var(--text-strong); letter-spacing: -0.02em; margin-bottom: 3px; }
    .stat-card small { font-size: 11px; font-weight: 300; color: var(--text-muted); }

    .layout { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(300px, 0.9fr); gap: 12px; }

    .card { padding: 20px; background: var(--bg-panel); border: 1px solid var(--line); border-radius: 4px; display: grid; gap: 16px; }
    .card__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
    .card h2 { font-size: 13px; font-weight: 500; color: var(--text-strong); margin: 0; }
    .card__header > span { font-size: 11px; color: var(--text-faint); flex-shrink: 0; }

    .table { display: grid; gap: 4px; }
    .table__head, .table__row { display: grid; grid-template-columns: 0.7fr 1fr 0.8fr 0.9fr; gap: 12px; align-items: center; padding: 10px 12px; }
    .table__head { font-size: 10px; text-transform: uppercase; letter-spacing: 0.18em; font-weight: 500; color: var(--text-faint); }
    .table__row { background: var(--surface-muted); border-radius: 3px; font-size: 12.5px; color: var(--text-muted); }
    .table__head--prediction, .table__row--prediction { grid-template-columns: 1fr 0.8fr 0.9fr 0.8fr; }

    .insight-list { display: grid; gap: 6px; }
    .insight-card { padding: 12px 14px; background: var(--surface-muted); border: 1px solid var(--line-subtle); border-radius: 3px; font-size: 12.5px; color: var(--text-muted); }

    @media (max-width: 1100px) {
      .page { padding: 20px 16px; }
      .page__header { flex-direction: column; }
      .layout, .filters, .table__head, .table__row, .table__head--prediction, .table__row--prediction { grid-template-columns: 1fr; }
    }
  `,
})
export class VisionAi {
  private readonly erpApi = inject(ErpApi);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly footfall = signal<VisionFootfallDto | null>(null);
  protected readonly conversion = signal<VisionConversionDto | null>(null);
  protected readonly peakHours = signal<VisionPeakHoursDto | null>(null);
  protected readonly insights = signal<VisionInsightsDto | null>(null);
  protected readonly predictions = signal<VisionPredictionsDto | null>(null);
  protected readonly feedback = signal<UiFeedback | null>(null);
  protected readonly visionLoading = signal(false);
  protected readonly allTimedOut = signal(false);

  protected from = '';
  protected to = '';

  constructor() {
    const now = new Date();
    const from = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
    this.from = this.toInputDate(from);
    this.to = this.toInputDate(now);
    this.load();
  }

  protected loading(): boolean {
    return this.visionLoading();
  }

  protected load(forceRefresh = false): void {
    this.feedback.set(null);
    this.visionLoading.set(true);
    this.allTimedOut.set(false);
    const params = {
      from: this.toIsoDate(this.from),
      to: this.toIsoDate(this.to),
    };
    let firstError: unknown | null = null;
    let timeoutCount = 0;
    const TIMEOUT_MS = 8000;
    const timedOut = (error: unknown) => {
      const isTimeout = !!error && typeof error === 'object' && 'name' in error && (error as Error).name === 'TimeoutError';
      if (isTimeout) timeoutCount += 1;
      firstError = firstError ?? error;
      return of(null);
    };

    forkJoin({
      footfall:    this.erpApi.getVisionFootfall(params, forceRefresh).pipe(timeout(TIMEOUT_MS), catchError(timedOut)),
      conversion:  this.erpApi.getVisionConversion(params, forceRefresh).pipe(timeout(TIMEOUT_MS), catchError(timedOut)),
      peakHours:   this.erpApi.getVisionPeakHours(params, forceRefresh).pipe(timeout(TIMEOUT_MS), catchError(timedOut)),
      insights:    this.erpApi.getVisionInsights(params, forceRefresh).pipe(timeout(TIMEOUT_MS), catchError(timedOut)),
      predictions: this.erpApi.getVisionPredictions(params, forceRefresh).pipe(timeout(TIMEOUT_MS), catchError(timedOut)),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ footfall, conversion, peakHours, insights, predictions }) => {
          this.footfall.set(footfall);
          this.conversion.set(conversion);
          this.peakHours.set(peakHours);
          this.insights.set(insights);
          this.predictions.set(predictions);
          this.visionLoading.set(false);
          const allNull = !footfall && !conversion && !peakHours && !insights && !predictions;
          this.allTimedOut.set(allNull && timeoutCount > 0);
          if (allNull) {
            this.feedback.set(this.httpFeedback.warning('No hay datos disponibles por ahora. Intenta nuevamente en unos minutos.'));
          } else if (firstError) {
            this.feedback.set(this.httpFeedback.warning('Algunas señales tardaron demasiado y no se cargaron. Puedes reintentar.'));
          } else if (this.hasDegradedMode()) {
            this.feedback.set(this.httpFeedback.warning(this.degradedSource()));
          }
        },
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudieron cargar las señales comerciales.'));
          this.visionLoading.set(false);
        },
      });
  }

  protected hasDegradedMode(): boolean {
    return Boolean(
      this.footfall()?.degraded ||
        this.conversion()?.degraded ||
        this.peakHours()?.degraded ||
        this.insights()?.degraded ||
        this.predictions()?.degraded,
    );
  }

  protected degradedSource(): string {
    if (!this.hasDegradedMode()) {
      return 'Datos completos';
    }
    return this.conversion()?.warningMessage || this.footfall()?.warningMessage || 'Datos externos temporalmente limitados';
  }

  private toInputDate(date: Date): string {
    const pad = (value: number) => `${value}`.padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
      date.getHours(),
    )}:${pad(date.getMinutes())}`;
  }

  private toIsoDate(value: string): string {
    return value ? new Date(value).toISOString() : '';
  }
}
