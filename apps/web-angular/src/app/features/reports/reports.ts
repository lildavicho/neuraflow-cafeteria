import { CurrencyPipe, DatePipe, DecimalPipe, LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { UiFeedback } from '../../core/models/ui-feedback';
import { HttpFeedback } from '../../core/services/http-feedback';
import { FileDownloadService } from '../../core/services/file-download';
import { ErpApi, ExportFormat, SalesReportDto } from '../../core/services/erp-api';
import { RequestFeedback } from '../shared/components/request-feedback';
import { ExportActions } from '../shared/components/export-actions';
import { LineChart, ChartPoint } from '../shared/components/line-chart';
import { BarChart, BarPoint } from '../shared/components/bar-chart';
import { SkeletonCards, SkeletonTable } from '../shared/components/skeleton-loader';

type Range = '7d' | '30d' | '90d' | 'custom';

@Component({
  selector: 'app-reports',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, LowerCasePipe, FormsModule, RequestFeedback, LineChart, BarChart, ExportActions, SkeletonCards, SkeletonTable],
  template: `
    <section class="rp">
      <!-- Header -->
      <header class="rp__header">
        <div>
          <span class="rp__eyebrow">Reportes</span>
          <h1>Lectura del negocio</h1>
          <p>Ventas, utilidad, ticket promedio y productos que mueven el resultado.</p>
        </div>
        <div class="rp__header-actions">
          <div class="rp__ranges">
            @for (r of ranges; track r.key) {
              <button
                type="button"
                class="rp__range-btn"
                [class.rp__range-btn--active]="activeRange() === r.key"
                (click)="setRange(r.key)"
              >{{ r.label }}</button>
            }
          </div>
          <div class="rp__date-row">
            <input type="date" class="rp__date-input" [(ngModel)]="fromDate" />
            <span class="rp__date-sep">—</span>
            <input type="date" class="rp__date-input" [(ngModel)]="toDate" />
            <button type="button" class="rp__apply-btn" [disabled]="loading()" (click)="load(true)">
              Aplicar
            </button>
          </div>
          <app-export-actions
            [disabled]="loading() || !report()"
            [exporting]="exporting()"
            (exportRequested)="exportReport($event)"
          />
        </div>
      </header>

      @if (feedback(); as notice) {
        <app-request-feedback [tone]="notice.tone" [message]="notice.message" [traceId]="notice.traceId" />
      }

      @if (loading()) {
        <app-skeleton-cards [count]="5" />
        <app-skeleton-table [rows]="8" [columns]="5" />
        @if (loadingTakingTooLong()) {
          <p class="rp__hint">La consulta está tardando más de lo habitual. Si no responde en unos segundos, podrás reintentar.</p>
        }
      } @else if (!report() && feedback()?.tone === 'error') {
        <div class="rp__error">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"/>
          </svg>
          <p>No pudimos cargar los reportes en este momento.</p>
          <button type="button" class="rp__retry" (click)="load(true)">Reintentar</button>
        </div>
      } @else if (report(); as r) {

        <!-- KPIs -->
        <div class="rp__kpis">
          <article class="rp__kpi">
            <span>Ventas totales</span>
            <strong>{{ r.totalSales | currency: 'USD':'symbol':'1.2-2' }}</strong>
            <small [class.rp__delta--up]="r.comparisonPreviousPeriod.salesDelta >= 0"
                   [class.rp__delta--down]="r.comparisonPreviousPeriod.salesDelta < 0">
              {{ r.comparisonPreviousPeriod.salesDelta >= 0 ? '+' : '' }}{{ r.comparisonPreviousPeriod.salesDelta | number: '1.1-1' }}% vs periodo anterior
            </small>
          </article>
          <article class="rp__kpi">
            <span>Utilidad</span>
            <strong>{{ r.totalProfit | currency: 'USD':'symbol':'1.2-2' }}</strong>
            <small [class.rp__delta--up]="r.comparisonPreviousPeriod.profitDelta >= 0"
                   [class.rp__delta--down]="r.comparisonPreviousPeriod.profitDelta < 0">
              {{ r.comparisonPreviousPeriod.profitDelta >= 0 ? '+' : '' }}{{ r.comparisonPreviousPeriod.profitDelta | number: '1.1-1' }}% vs periodo anterior
            </small>
          </article>
          <article class="rp__kpi">
            <span>Órdenes</span>
            <strong>{{ r.totalOrders }}</strong>
            <small [class.rp__delta--up]="r.comparisonPreviousPeriod.ordersDelta >= 0"
                   [class.rp__delta--down]="r.comparisonPreviousPeriod.ordersDelta < 0">
              {{ r.comparisonPreviousPeriod.ordersDelta >= 0 ? '+' : '' }}{{ r.comparisonPreviousPeriod.ordersDelta | number: '1.1-1' }}% vs periodo anterior
            </small>
          </article>
          <article class="rp__kpi">
            <span>Ticket promedio</span>
            <strong>{{ r.avgTicket | currency: 'USD':'symbol':'1.2-2' }}</strong>
            <small>por transacción</small>
          </article>
          <article class="rp__kpi rp__kpi--highlight">
            <span>Margen</span>
            <strong>{{ margin() | number: '1.1-1' }}%</strong>
            <small>utilidad / ventas</small>
          </article>
        </div>

        <!-- Charts row -->
        <div class="rp__charts">
          <article class="rp__card">
            <header class="rp__card-header">
              <h2>Tendencia de ventas</h2>
              <span>{{ r.from | date: 'mediumDate' }} – {{ r.to | date: 'mediumDate' }}</span>
            </header>
            @if (salesChartPoints().length === 0) {
              <p class="rp__chart-empty">Sin ventas en este rango.</p>
            } @else {
              <app-line-chart [points]="salesChartPoints()" />
            }
          </article>

          <article class="rp__card">
            <header class="rp__card-header">
              <h2>Tendencia de utilidad</h2>
              <span>{{ r.dailySales.length === 0 ? 'Sin actividad en el rango' : (r.dailySales.length + ' días') }}</span>
            </header>
            @if (profitChartPoints().length === 0) {
              <p class="rp__chart-empty">Sin utilidad registrada en este rango.</p>
            } @else {
              <app-line-chart [points]="profitChartPoints()" />
            }
          </article>
        </div>

        <!-- Top products chart -->
        @if (r.topProducts.length) {
          <article class="rp__card">
            <header class="rp__card-header">
              <h2>Productos top por ingresos</h2>
              <span>ingresos vs utilidad</span>
            </header>
            <app-bar-chart [points]="topProductsBarPoints()" />
          </article>
        }

        <!-- Tables row -->
        <div class="rp__tables">
          <!-- Payment methods -->
          @if (r.salesByPaymentMethod.length) {
            <article class="rp__card">
              <header class="rp__card-header">
                <h2>Por método de pago</h2>
                <span>{{ r.salesByPaymentMethod.length }}</span>
              </header>
              <div class="rp__table">
                <div class="rp__thead rp__thead--pm">
                  <span>Método</span><span>Transacciones</span><span>Total</span>
                </div>
                @for (pm of r.salesByPaymentMethod; track pm.method) {
                  <div class="rp__trow rp__trow--pm">
                    <span class="rp__badge rp__badge--{{ pm.method | lowercase }}">{{ pm.method }}</span>
                    <span>{{ pm.count }}</span>
                    <span>{{ pm.total | currency: 'USD':'symbol':'1.2-2' }}</span>
                  </div>
                }
              </div>
            </article>
          }

          <!-- Top products table -->
          @if (r.topProducts.length) {
            <article class="rp__card">
              <header class="rp__card-header">
                <h2>Productos líderes</h2>
                <span>{{ r.topProducts.length }}</span>
              </header>
              <div class="rp__table">
                <div class="rp__thead rp__thead--prod">
                  <span>Producto</span><span>Uds</span><span>Ingresos</span><span>Utilidad</span>
                </div>
                @for (p of r.topProducts; track p.id) {
                  <div class="rp__trow rp__trow--prod">
                    <span>{{ p.name }}</span>
                    <span>{{ p.quantity }}</span>
                    <span>{{ p.revenue | currency: 'USD':'symbol':'1.2-2' }}</span>
                    <span>{{ p.profit | currency: 'USD':'symbol':'1.2-2' }}</span>
                  </div>
                }
              </div>
            </article>
          }
        </div>

        <!-- Daily breakdown -->
        @if (r.dailySales.length) {
          <article class="rp__card">
            <header class="rp__card-header">
              <h2>Desglose diario</h2>
              <span>{{ r.dailySales.length }} registros</span>
            </header>
            <div class="rp__table">
              <div class="rp__thead rp__thead--daily">
                <span>Fecha</span><span>Ventas</span><span>Utilidad</span><span>Órdenes</span><span>Ticket</span>
              </div>
              @for (day of r.dailySales; track day.date) {
                <div class="rp__trow rp__trow--daily">
                  <span>{{ day.date | date: 'EEE d MMM' }}</span>
                  <span>{{ day.sales | currency: 'USD':'symbol':'1.2-2' }}</span>
                  <span>{{ day.profit | currency: 'USD':'symbol':'1.2-2' }}</span>
                  <span>{{ day.orders }}</span>
                  <span>{{ day.orders > 0 ? (day.sales / day.orders | currency: 'USD':'symbol':'1.2-2') : '—' }}</span>
                </div>
              }
            </div>
          </article>
        }

      } @else {
        <p class="rp__empty">Selecciona un periodo y presiona Aplicar para ver los reportes.</p>
      }
    </section>
  `,
  styles: `
    :host { display: contents; }

    .rp {
      display: grid;
      gap: 24px;
      padding: 32px;
      animation: pageEnter var(--dur-normal, 260ms) var(--ease-out, ease) both;
    }

    /* Header */
    .rp__header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 24px;
      flex-wrap: wrap;
    }

    .rp__eyebrow {
      display: block;
      font-size: 10px;
      letter-spacing: 0.22em;
      text-transform: uppercase;
      font-weight: 500;
      color: var(--aurora-dim);
      margin-bottom: 8px;
    }

    .rp__header h1 {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 26px;
      font-weight: 400;
      color: var(--text-strong);
      margin: 0 0 6px;
      letter-spacing: -0.01em;
    }

    .rp__header p {
      font-size: 13px;
      color: var(--text-faint);
      margin: 0;
      line-height: 1.6;
    }

    .rp__header-actions {
      display: flex;
      flex-direction: column;
      gap: 10px;
      align-items: flex-end;
      flex-shrink: 0;
    }

    .rp__ranges {
      display: flex;
      gap: 4px;
    }

    .rp__range-btn {
      padding: 6px 12px;
      background: transparent;
      border: 1px solid var(--line);
      color: var(--text-faint);
      font-size: 11px;
      border-radius: 2px;
      cursor: pointer;
      transition: all 150ms ease;
    }

    .rp__range-btn:hover { border-color: var(--aurora-border); color: var(--aurora-dim); }

    .rp__range-btn--active {
      background: var(--aurora-ghost);
      border-color: var(--aurora-border);
      color: var(--aurora);
    }

    .rp__date-row {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .rp__date-input {
      padding: 6px 10px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 2px;
      font-size: 12px;
      color: var(--text);
      cursor: pointer;
    }

    .rp__date-sep { color: var(--text-faint); font-size: 12px; }

    .rp__apply-btn {
      padding: 7px 16px;
      background: var(--aurora-ghost);
      border: 1px solid var(--aurora-border);
      color: var(--aurora);
      font-size: 11px;
      font-weight: 500;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      border-radius: 2px;
      cursor: pointer;
      transition: all 150ms ease;
    }

    .rp__apply-btn:hover:not(:disabled) { background: rgba(74,124,94,0.14); }
    .rp__apply-btn:disabled { opacity: 0.35; cursor: not-allowed; }

    /* KPIs */
    .rp__kpis {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 10px;
    }

    .rp__kpi {
      padding: 18px 20px;
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 4px;
      display: grid;
      gap: 6px;
    }

    .rp__kpi--highlight {
      border-color: var(--aurora-border);
      background: var(--aurora-ghost);
    }

    .rp__kpi > span {
      font-size: 10px;
      letter-spacing: 0.18em;
      text-transform: uppercase;
      color: var(--text-faint);
    }

    .rp__kpi > strong {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 26px;
      font-weight: 400;
      color: var(--text-strong);
      letter-spacing: -0.02em;
    }

    .rp__kpi > small { font-size: 11px; color: var(--text-muted); }

    .rp__delta--up { color: rgba(74, 124, 94, 0.85) !important; }
    .rp__delta--down { color: rgba(180, 60, 50, 0.8) !important; }

    /* Charts */
    .rp__charts {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 12px;
    }

    /* Card */
    .rp__card {
      padding: 20px;
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 4px;
      display: grid;
      gap: 16px;
    }

    .rp__card-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
    }

    .rp__card-header h2 {
      font-size: 13px;
      font-weight: 500;
      color: var(--text);
      margin: 0;
    }

    .rp__card-header > span {
      font-size: 11px;
      color: var(--text-faint);
      flex-shrink: 0;
    }

    /* Tables */
    .rp__tables {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 12px;
    }

    .rp__table { display: grid; gap: 3px; }

    .rp__thead, .rp__trow {
      display: grid;
      gap: 10px;
      align-items: center;
      padding: 9px 12px;
      border-radius: 3px;
    }

    .rp__thead {
      font-size: 10px;
      text-transform: uppercase;
      letter-spacing: 0.16em;
      color: var(--text-faint);
    }

    .rp__trow {
      background: var(--surface);
      font-size: 12.5px;
      color: var(--text);
    }

    .rp__thead--pm, .rp__trow--pm { grid-template-columns: 1.2fr 0.8fr 1fr; }
    .rp__thead--prod, .rp__trow--prod { grid-template-columns: 1.8fr 0.5fr 1fr 1fr; }
    .rp__thead--daily, .rp__trow--daily { grid-template-columns: 1.4fr 1fr 1fr 0.5fr 1fr; }

    /* Badge */
    .rp__badge {
      padding: 2px 8px;
      border-radius: 2px;
      font-size: 10px;
      font-weight: 500;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      background: var(--surface-strong);
      color: var(--text-muted);
      display: inline-block;
    }

    .rp__badge--cash { background: rgba(74,124,94,0.1); color: rgba(74,124,94,0.85); }
    .rp__badge--card { background: rgba(60,120,196,0.1); color: rgba(60,120,196,0.85); }
    .rp__badge--transfer { background: rgba(196,150,60,0.1); color: rgba(196,150,60,0.85); }

    /* Empty / loading */
    .rp__empty {
      margin: 0;
      padding: 16px;
      font-size: 13px;
      color: var(--text-faint);
      border: 1px solid var(--line);
      border-radius: 4px;
      text-align: center;
    }

    .rp__hint {
      margin: 8px 0 0;
      padding: 10px 14px;
      font-size: 12px;
      color: var(--text-muted);
      background: var(--info-bg);
      border: 1px solid var(--info-border);
      border-radius: 6px;
      text-align: center;
    }

    .rp__error {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      padding: 56px 24px;
      border: 1px dashed var(--danger-border);
      border-radius: 12px;
      background: var(--danger-bg);
      color: var(--danger);
      text-align: center;
    }
    .rp__error svg { width: 36px; height: 36px; }
    .rp__error p { margin: 0; font-size: 14px; color: var(--text-strong); }
    .rp__retry {
      padding: 9px 20px;
      background: var(--aurora);
      border: none;
      color: #fff;
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      border-radius: 6px;
      cursor: pointer;
      transition: opacity 150ms ease;
    }
    .rp__retry:hover { opacity: 0.85; }

    .rp__chart-empty {
      margin: 0;
      padding: 36px 16px;
      text-align: center;
      font-size: 13px;
      color: var(--text-faint);
      background: var(--surface-muted);
      border: 1px dashed var(--line-strong);
      border-radius: 8px;
    }

    @media (max-width: 900px) {
      .rp { padding: 20px 16px; }
      .rp__header { flex-direction: column; }
      .rp__header-actions { align-items: flex-start; }
      .rp__thead--daily, .rp__trow--daily { grid-template-columns: 1fr 1fr 1fr; }
      .rp__thead--daily span:nth-child(4),
      .rp__thead--daily span:nth-child(5),
      .rp__trow--daily span:nth-child(4),
      .rp__trow--daily span:nth-child(5) { display: none; }
    }
  `,
})
export class Reports {
  private readonly erpApi = inject(ErpApi);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly fileDownload = inject(FileDownloadService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly loadingTakingTooLong = signal(false);
  private slowTimer: ReturnType<typeof setTimeout> | null = null;
  protected readonly feedback = signal<UiFeedback | null>(null);
  protected readonly report = signal<SalesReportDto | null>(null);
  protected readonly activeRange = signal<Range>('30d');
  protected readonly exporting = signal<ExportFormat | null>(null);

  protected fromDate = this.isoDate(-30);
  protected toDate = this.isoDate(0);

  protected readonly ranges: { key: Range; label: string }[] = [
    { key: '7d', label: '7 días' },
    { key: '30d', label: '30 días' },
    { key: '90d', label: '90 días' },
  ];

  protected readonly margin = computed(() => {
    const r = this.report();
    if (!r || !r.totalSales) return 0;
    return (r.totalProfit / r.totalSales) * 100;
  });

  protected readonly salesChartPoints = computed((): ChartPoint[] =>
    (this.report()?.dailySales ?? []).map((d) => ({ label: d.date, value: d.sales })),
  );

  protected readonly profitChartPoints = computed((): ChartPoint[] =>
    (this.report()?.dailySales ?? []).map((d) => ({ label: d.date, value: d.profit })),
  );

  protected readonly topProductsBarPoints = computed((): BarPoint[] =>
    (this.report()?.topProducts ?? []).slice(0, 8).map((p) => ({
      label: p.name.length > 14 ? p.name.slice(0, 13) + '…' : p.name,
      value: p.revenue,
      value2: p.profit,
    })),
  );

  constructor() {
    this.load();
  }

  protected setRange(range: Range): void {
    this.activeRange.set(range);
    const days = range === '7d' ? 7 : range === '30d' ? 30 : 90;
    this.fromDate = this.isoDate(-days);
    this.toDate = this.isoDate(0);
    this.load();
  }

  protected exportReport(format: ExportFormat): void {
    this.exporting.set(format);
    this.feedback.set(null);

    this.erpApi
      .exportSalesReport(format, this.fromDate, this.toDate)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.fileDownload.download(response, `reporte-ventas.${format}`);
          this.feedback.set(this.httpFeedback.success(`Reporte exportado en ${format.toUpperCase()}.`));
          this.exporting.set(null);
        },
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo exportar el reporte.'));
          this.exporting.set(null);
        },
      });
  }

  protected load(forceRefresh = false): void {
    this.loading.set(true);
    this.feedback.set(null);
    this.loadingTakingTooLong.set(false);
    if (this.slowTimer) {
      clearTimeout(this.slowTimer);
    }
    this.slowTimer = setTimeout(() => this.loadingTakingTooLong.set(true), 6000);

    this.erpApi
      .getSalesReport(this.fromDate, this.toDate, forceRefresh)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() =>
          // Fallback: build a minimal SalesReportDto from the dashboard snapshot
          this.erpApi.getDashboardSnapshot(forceRefresh).pipe(
            catchError(() => of(null)),
          ),
        ),
      )
      .subscribe({
        next: (data) => {
          if (!data) {
            this.feedback.set(this.httpFeedback.fromError(null as any, 'No fue posible cargar los reportes.'));
            this.loading.set(false);
            this.loadingTakingTooLong.set(false);
            if (this.slowTimer) { clearTimeout(this.slowTimer); this.slowTimer = null; }
            return;
          }
          // If it came from the fallback (DashboardSnapshot), map it
          if ('ventasHoy' in data) {
            const snap = data as any;
            this.report.set({
              from: this.fromDate,
              to: this.toDate,
              totalSales: snap.ventasHoy,
              totalProfit: snap.gananciasHoy,
              totalOrders: snap.ordenesHoy,
              avgTicket: snap.ticketPromedio,
              dailySales: (snap.historicoVentas ?? []).map((h: any) => ({
                date: h.date,
                sales: h.value,
                profit: (snap.historicoGanancias ?? []).find((g: any) => g.date === h.date)?.value ?? 0,
                orders: 0,
              })),
              topProducts: snap.productosTop ?? [],
              salesByPaymentMethod: [],
              comparisonPreviousPeriod: {
                salesDelta: snap.comparacionVentas?.deltaPorcentaje ?? 0,
                profitDelta: snap.comparacionGanancias?.deltaPorcentaje ?? 0,
                ordersDelta: 0,
              },
            });
          } else {
            this.report.set(data as SalesReportDto);
          }
          this.loading.set(false);
          this.loadingTakingTooLong.set(false);
          if (this.slowTimer) { clearTimeout(this.slowTimer); this.slowTimer = null; }
        },
        error: (err) => {
          this.feedback.set(this.httpFeedback.fromError(err, 'Error cargando reportes.'));
          this.loading.set(false);
          this.loadingTakingTooLong.set(false);
          if (this.slowTimer) { clearTimeout(this.slowTimer); this.slowTimer = null; }
        },
      });
  }

  private isoDate(offsetDays: number): string {
    const d = new Date();
    d.setDate(d.getDate() + offsetDays);
    return d.toISOString().slice(0, 10);
  }
}



