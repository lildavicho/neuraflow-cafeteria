import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UiFeedback } from '../../core/models/ui-feedback';
import { ErpApi, DashboardSnapshot } from '../../core/services/erp-api';
import { HttpFeedback } from '../../core/services/http-feedback';
import { ThemeService } from '../../core/services/theme';
import { RequestFeedback } from '../shared/components/request-feedback';
import { LineChart, ChartPoint } from '../shared/components/line-chart';
import { SkeletonCards } from '../shared/components/skeleton-loader';

type ChartMode = 'sales' | 'profit';

type KpiCard = {
  label: string;
  value: number;
  kind: 'currency' | 'number';
  digits: string;
  helper: string;
  delta?: number | null;
  icon: 'revenue' | 'profit' | 'orders' | 'ticket' | 'attention';
  accent: 'revenue' | 'profit' | 'orders' | 'ticket' | 'attention';
};

type MixItem = {
  name: string;
  revenue: number;
  profit: number;
  quantity: number;
  share: number;
};

type HealthSegment = {
  label: string;
  count: number;
  share: number;
  tone: 'danger' | 'warning' | 'info';
};

type AgendaItem = {
  title: string;
  detail: string;
  meta: string;
  tone: 'danger' | 'warning' | 'info' | 'ok';
};

type TimelineDay = {
  date: string;
  weekday: string;
  day: string;
  active: boolean;
};

type LeaderBar = {
  name: string;
  revenue: number;
  profit: number;
  quantity: number;
  width: number;
};

@Component({
  selector: 'app-executive-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, RequestFeedback, LineChart, SkeletonCards],
  templateUrl: './executive-dashboard.html',
  styleUrls: ['./executive-dashboard.css'],
})
export class ExecutiveDashboard {
  private readonly erpApi = inject(ErpApi);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly themeService = inject(ThemeService);

  private readonly moneyFormatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2,
  });
  private readonly shortDayFormatter = new Intl.DateTimeFormat('es-EC', { weekday: 'short' });
  private readonly dayNumberFormatter = new Intl.DateTimeFormat('es-EC', { day: 'numeric' });
  private readonly shortDateFormatter = new Intl.DateTimeFormat('es-EC', {
    day: 'numeric',
    month: 'short',
  });

  protected readonly loading = signal(false);
  protected readonly feedback = signal<UiFeedback | null>(null);
  protected readonly snapshot = signal<DashboardSnapshot | null>(null);
  protected readonly chartMode = signal<ChartMode>('sales');

  protected readonly pendingLoadCount = computed(() => {
    const view = this.snapshot();
    if (!view) {
      return 0;
    }
    return view.stockCriticoCount + view.documentosSriPendientes + view.pagosTransferenciaPendientes.length;
  });

  protected readonly kpiCards = computed((): KpiCard[] => {
    const view = this.snapshot();
    if (!view) {
      return [];
    }

    return [
      {
        label: 'Ingresos del dia',
        value: view.ventasHoy,
        kind: 'currency',
        digits: '1.2-2',
        helper: 'Cobros del dia consolidados',
        delta: view.comparacionVentas.deltaPorcentaje,
        icon: 'revenue',
        accent: 'revenue',
      },
      {
        label: 'Utilidad del dia',
        value: view.gananciasHoy,
        kind: 'currency',
        digits: '1.2-2',
        helper: 'Margen operativo generado',
        delta: view.comparacionGanancias.deltaPorcentaje,
        icon: 'profit',
        accent: 'profit',
      },
      {
        label: 'Ordenes cobradas',
        value: view.ordenesHoy,
        kind: 'number',
        digits: '1.0-0',
        helper: 'Ventas cerradas hoy',
        delta: null,
        icon: 'orders',
        accent: 'orders',
      },
      {
        label: 'Ticket promedio',
        value: view.ticketPromedio,
        kind: 'currency',
        digits: '1.2-2',
        helper: 'Promedio por orden cobrada',
        delta: null,
        icon: 'ticket',
        accent: 'ticket',
      },
      {
        label: 'Frentes activos',
        value: this.pendingLoadCount(),
        kind: 'number',
        digits: '1.0-0',
        helper: 'Stock, SRI y tesorería',
        delta: null,
        icon: 'attention',
        accent: 'attention',
      },
    ];
  });

  protected readonly salesChartPoints = computed((): ChartPoint[] =>
    (this.snapshot()?.historicoVentas ?? []).map((item) => ({ label: item.date, value: item.value })),
  );

  protected readonly profitChartPoints = computed((): ChartPoint[] =>
    (this.snapshot()?.historicoGanancias ?? []).map((item) => ({ label: item.date, value: item.value })),
  );

  protected readonly activeTrendPoints = computed((): ChartPoint[] =>
    this.chartMode() === 'sales' ? this.salesChartPoints() : this.profitChartPoints(),
  );

  protected readonly chartHeadlineValue = computed(() => {
    const view = this.snapshot();
    if (!view) {
      return 0;
    }
    return this.chartMode() === 'sales' ? view.ventasHoy : view.gananciasHoy;
  });

  protected readonly chartHeadlineDelta = computed(() => {
    const view = this.snapshot();
    if (!view) {
      return 0;
    }
    return this.chartMode() === 'sales'
      ? view.comparacionVentas.deltaPorcentaje
      : view.comparacionGanancias.deltaPorcentaje;
  });

  protected readonly productMix = computed((): MixItem[] => {
    const topProducts = this.snapshot()?.productosTop ?? [];
    const totalRevenue = topProducts.reduce((sum, item) => sum + item.revenue, 0);
    if (!topProducts.length || totalRevenue <= 0) {
      return [];
    }

    return topProducts.slice(0, 4).map((item) => ({
      name: item.name,
      revenue: item.revenue,
      profit: item.profit,
      quantity: item.quantity,
      share: Math.max(8, Math.round((item.revenue / totalRevenue) * 100)),
    }));
  });

  protected readonly topRevenueTotal = computed(() =>
    (this.snapshot()?.productosTop ?? []).reduce((sum, item) => sum + item.revenue, 0),
  );

  protected readonly healthSegments = computed((): HealthSegment[] => {
    const view = this.snapshot();
    if (!view) {
      return [];
    }

    const raw = [
      { label: 'Stock crítico', count: view.stockCriticoCount, tone: 'danger' as const },
      { label: 'SRI pendiente', count: view.documentosSriPendientes, tone: 'warning' as const },
      { label: 'Transferencias', count: view.pagosTransferenciaPendientes.length, tone: 'info' as const },
      { label: 'CxC abiertas', count: view.cuentasPorCobrarAbiertas, tone: 'warning' as const },
      { label: 'CxP abiertas', count: view.cuentasPorPagarAbiertas, tone: 'info' as const },
    ].filter((item) => item.count > 0);

    const total = raw.reduce((sum, item) => sum + item.count, 0);
    if (!total) {
      return [];
    }

    return raw.map((item) => ({
      ...item,
      share: item.count / total,
    }));
  });

  protected readonly healthIssueTotal = computed(() =>
    this.healthSegments().reduce((sum, item) => sum + item.count, 0),
  );

  protected readonly healthRingStyle = computed(() => {
    const segments = this.healthSegments();
    if (!segments.length) {
      return 'conic-gradient(var(--ok) 0 360deg)';
    }

    let offset = 0;
    const stops = segments.map((segment) => {
      const start = offset;
      const end = offset + segment.share * 360;
      offset = end;
      return `${this.toneColor(segment.tone)} ${start}deg ${end}deg`;
    });

    return `conic-gradient(${stops.join(', ')})`;
  });

  protected readonly agendaItems = computed((): AgendaItem[] => {
    const view = this.snapshot();
    if (!view) {
      return [];
    }

    const items: AgendaItem[] = [];
    const transferTotal = view.pagosTransferenciaPendientes.reduce((sum, payment) => sum + payment.total, 0);

    if (view.pagosTransferenciaPendientes.length) {
      items.push({
        title: 'Confirmar transferencias',
        detail: `${view.pagosTransferenciaPendientes.length} pagos siguen en revisión.`,
        meta: `${this.moneyFormatter.format(transferTotal)} por validar`,
        tone: 'warning',
      });
    }

    if (view.documentosSriPendientes || view.documentosSriRecientes.length) {
      const latestDocument = view.documentosSriRecientes[0];
      items.push({
        title: 'Revisar documentos SRI',
        detail: `${view.documentosSriPendientes} documentos esperan envío o seguimiento.`,
        meta: latestDocument ? `Último ${this.formatDateLabel(latestDocument.issueDate)}` : 'Sin actividad reciente',
        tone: view.documentosSriPendientes > 0 ? 'danger' : 'info',
      });
    }

    if (view.stockCriticoCount) {
      const firstRisk = view.productosBajoStock[0];
      items.push({
        title: 'Reponer inventario',
        detail: `${view.stockCriticoCount} productos estan bajo el minimo.`,
        meta: firstRisk ? `${firstRisk.name} ${firstRisk.availableStock}/${firstRisk.minStock}` : 'Prioridad alta',
        tone: 'danger',
      });
    }

    if (view.alertas.length) {
      items.push({
        title: view.alertas[0].title,
        detail: view.alertas[0].detail,
        meta: `${view.alertas.length} alertas visibles`,
        tone: this.alertTone(view.alertas[0].level),
      });
    }

    if (!items.length) {
      items.push({
        title: 'Operacion estable',
        detail: 'No hay pendientes urgentes abiertos en este momento.',
        meta: 'Tablero en calma',
        tone: 'ok',
      });
    }

    return items.slice(0, 4);
  });

  protected readonly timelineDays = computed((): TimelineDay[] => {
    const history = this.snapshot()?.historicoVentas ?? [];
    const source = history.length ? history.slice(-5) : [{ date: new Date().toISOString(), value: 0 }];

    return source.map((item, index) => {
      const parsedDate = new Date(item.date);
      return {
        date: item.date,
        weekday: this.shortDayFormatter.format(parsedDate).replace('.', ''),
        day: this.dayNumberFormatter.format(parsedDate),
        active: index === source.length - 1,
      };
    });
  });

  protected readonly topRevenueBars = computed((): LeaderBar[] => {
    const products = this.snapshot()?.productosTop ?? [];
    if (!products.length) {
      return [];
    }

    const maxRevenue = Math.max(...products.map((item) => item.revenue), 1);
    return products.slice(0, 4).map((item) => ({
      name: item.name,
      revenue: item.revenue,
      profit: item.profit,
      quantity: item.quantity,
      width: Math.max(10, Math.round((item.revenue / maxRevenue) * 100)),
    }));
  });

  constructor() {
    this.load();
  }

  protected setChartMode(mode: ChartMode): void {
    this.chartMode.set(mode);
  }

  protected load(forceRefresh = false): void {
    this.loading.set(true);
    this.feedback.set(null);

    this.erpApi
      .getDashboardSnapshot(forceRefresh)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (snapshot) => {
          this.snapshot.set(snapshot);
          if (forceRefresh) {
            this.feedback.set(this.httpFeedback.success('El dashboard ejecutivo fue actualizado.'));
          }
          this.loading.set(false);
        },
        error: (error) => {
          this.feedback.set(
            this.httpFeedback.fromError(error, 'No fue posible cargar el dashboard ejecutivo del ERP.'),
          );
          this.loading.set(false);
        },
      });
  }

  protected metricTone(delta: number | null | undefined): 'positive' | 'negative' | 'neutral' {
    if (delta === null || delta === undefined || delta === 0) {
      return 'neutral';
    }
    return delta > 0 ? 'positive' : 'negative';
  }

  protected trendCaption(): string {
    const points = this.activeTrendPoints();
    if (!points.length) {
      return 'Sin serie historica disponible.';
    }
    return `${points.length} cortes historicos para leer el ritmo comercial.`;
  }

  protected alertTone(level: string): 'danger' | 'warning' | 'info' | 'ok' {
    switch (level) {
      case 'danger':
      case 'error':
        return 'danger';
      case 'warning':
        return 'warning';
      case 'success':
        return 'ok';
      default:
        return 'info';
    }
  }

  private toneColor(tone: HealthSegment['tone']): string {
    switch (tone) {
      case 'danger':
        return 'var(--danger)';
      case 'warning':
        return 'var(--warn)';
      default:
        return 'var(--info)';
    }
  }

  private formatDateLabel(value: string): string {
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return 'sin fecha';
    }
    return this.shortDateFormatter.format(parsed);
  }
}
