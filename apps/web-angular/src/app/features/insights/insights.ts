import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UiFeedback } from '../../core/models/ui-feedback';
import { HttpFeedback } from '../../core/services/http-feedback';
import { ErpApi, DashboardSnapshot } from '../../core/services/erp-api';
import { RequestFeedback } from '../shared/components/request-feedback';
import { SkeletonCards } from '../shared/components/skeleton-loader';

type Tone = 'success' | 'warning' | 'danger' | 'info';

interface InsightCard {
  id: string;
  tone: Tone;
  category: string;
  title: string;
  explanation: string;
  impact: string;
  actionRecommended: string;
}

@Component({
  selector: 'app-insights',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RequestFeedback, SkeletonCards],
  template: `
    <section class="ins">
      <!-- Header -->
      <header class="ins__header">
        <div>
          <span class="ins__eyebrow">Inteligencia</span>
          <h1>Insights del negocio</h1>
          <p>Señales concretas para actuar hoy: productos, ventas, stock y oportunidades detectadas.</p>
        </div>
        <div class="ins__header-actions">
          @if (generatedAt()) {
            <small class="ins__ts">Generado {{ generatedAt() | date: 'short' }}</small>
          }
          <button type="button" class="ins__refresh" [disabled]="loading()" (click)="load(true)">
            Actualizar
          </button>
        </div>
      </header>

      @if (feedback(); as notice) {
        <app-request-feedback [tone]="notice.tone" [message]="notice.message" [traceId]="notice.traceId" />
      }

      @if (loading()) {
        <app-skeleton-cards [count]="6" />
      } @else {
        <!-- Summary chips -->
        @if (insights().length) {
          <div class="ins__summary">
            @for (cat of categoryCounts(); track cat.label) {
              <span class="ins__chip ins__chip--{{ cat.tone }}" [attr.aria-label]="cat.label + ' · ' + cat.count + ' (resumen informativo)'">
                {{ cat.label }} · {{ cat.count }}
              </span>
            }
          </div>
        }

        <!-- Alerts first -->
        @if (alerts().length) {
          <div class="ins__section">
            <h2 class="ins__section-title">Alertas activas</h2>
            <div class="ins__grid">
              @for (a of alerts(); track a.id) {
                <article class="ins__card ins__card--{{ a.tone }}">
                  <div class="ins__card-top">
                    <span class="ins__badge ins__badge--{{ a.tone }}">{{ a.category }}</span>
                    <div class="ins__dot ins__dot--{{ a.tone }}"></div>
                  </div>
                  <h3>{{ a.title }}</h3>
                  <p class="ins__explanation">{{ a.explanation }}</p>
                  <div class="ins__card-footer">
                    <div class="ins__impact">
                      <span class="ins__impact-label">Impacto</span>
                      <span>{{ a.impact }}</span>
                    </div>
                    <div class="ins__action">
                      <span class="ins__action-label">Acción recomendada</span>
                      <span>{{ a.actionRecommended }}</span>
                    </div>
                  </div>
                </article>
              }
            </div>
          </div>
        }

        <!-- Opportunities -->
        @if (opportunities().length) {
          <div class="ins__section">
            <h2 class="ins__section-title">Oportunidades</h2>
            <div class="ins__grid">
              @for (i of opportunities(); track i.id) {
                <article class="ins__card ins__card--{{ i.tone }}">
                  <div class="ins__card-top">
                    <span class="ins__badge ins__badge--{{ i.tone }}">{{ i.category }}</span>
                    <div class="ins__dot ins__dot--{{ i.tone }}"></div>
                  </div>
                  <h3>{{ i.title }}</h3>
                  <p class="ins__explanation">{{ i.explanation }}</p>
                  <div class="ins__card-footer">
                    <div class="ins__impact">
                      <span class="ins__impact-label">Impacto</span>
                      <span>{{ i.impact }}</span>
                    </div>
                    <div class="ins__action">
                      <span class="ins__action-label">Acción recomendada</span>
                      <span>{{ i.actionRecommended }}</span>
                    </div>
                  </div>
                </article>
              }
            </div>
          </div>
        }

        @if (!insights().length) {
          <div class="ins__none">
            <span class="ins__none-icon">✓</span>
            <strong>Todo bajo control</strong>
            <p>No hay alertas ni oportunidades detectadas en este momento. Vuelve después de registrar más ventas y movimientos.</p>
          </div>
        }
      }
    </section>
  `,
  styles: `
    :host { display: contents; }

    .ins {
      display: grid;
      gap: 28px;
      padding: 32px;
      animation: pageEnter var(--dur-normal, 260ms) var(--ease-out, ease) both;
    }

    /* Header */
    .ins__header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 24px;
      flex-wrap: wrap;
    }

    .ins__eyebrow {
      display: block;
      font-size: 10px;
      letter-spacing: 0.22em;
      text-transform: uppercase;
      font-weight: 500;
      color: var(--aurora-dim);
      margin-bottom: 8px;
    }

    .ins__header h1 {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 26px;
      font-weight: 400;
      color: var(--text-strong);
      margin: 0 0 6px;
      letter-spacing: -0.01em;
    }

    .ins__header p {
      font-size: 13px;
      color: var(--text-faint);
      margin: 0;
      line-height: 1.6;
    }

    .ins__header-actions {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 8px;
      flex-shrink: 0;
    }

    .ins__ts {
      font-size: 11px;
      color: var(--text-faint);
    }

    .ins__refresh {
      padding: 9px 20px;
      background: transparent;
      border: 1px solid var(--aurora-border);
      color: var(--aurora-dim);
      font-size: 11px;
      font-weight: 500;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      border-radius: 2px;
      cursor: pointer;
      transition: all 150ms ease;
    }

    .ins__refresh:hover:not(:disabled) { background: var(--aurora-ghost); }
    .ins__refresh:disabled { opacity: 0.35; cursor: not-allowed; }

    /* Summary chips */
    .ins__summary {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .ins__chip {
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 11px;
      font-weight: 500;
      cursor: default;
      user-select: none;
      pointer-events: none;
    }

    .ins__chip--danger  { background: rgba(180,60,50,0.1); color: rgba(200,80,70,0.9); }
    .ins__chip--warning { background: rgba(196,150,60,0.1); color: rgba(196,150,60,0.9); }
    .ins__chip--success { background: var(--aurora-ghost); color: var(--aurora-dim); }
    .ins__chip--info    { background: rgba(60,120,196,0.1); color: rgba(100,160,220,0.9); }

    /* Section */
    .ins__section { display: grid; gap: 14px; }

    .ins__section-title {
      font-size: 11px;
      letter-spacing: 0.2em;
      text-transform: uppercase;
      color: var(--text-faint);
      font-weight: 500;
      margin: 0;
    }

    /* Grid */
    .ins__grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 12px;
    }

    /* Card */
    .ins__card {
      padding: 20px;
      border-radius: 4px;
      display: grid;
      gap: 12px;
      border: 1px solid var(--line);
      background: var(--bg-panel);
      transition: border-color 200ms ease;
    }

    .ins__card:hover { border-color: var(--line-strong); }

    .ins__card--danger  { border-left: 3px solid rgba(180,60,50,0.5); }
    .ins__card--warning { border-left: 3px solid rgba(196,150,60,0.5); }
    .ins__card--success { border-left: 3px solid var(--aurora-border); }
    .ins__card--info    { border-left: 3px solid rgba(60,120,196,0.4); }

    .ins__card-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .ins__badge {
      padding: 3px 9px;
      border-radius: 2px;
      font-size: 9px;
      font-weight: 600;
      letter-spacing: 0.14em;
      text-transform: uppercase;
    }

    .ins__badge--danger  { background: rgba(180,60,50,0.12); color: rgba(200,80,70,0.9); }
    .ins__badge--warning { background: rgba(196,150,60,0.12); color: rgba(196,150,60,0.9); }
    .ins__badge--success { background: var(--aurora-ghost); color: var(--aurora-dim); }
    .ins__badge--info    { background: rgba(60,120,196,0.12); color: rgba(100,160,220,0.9); }

    .ins__dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
    }

    .ins__dot--danger  { background: rgba(200,80,70,0.8); }
    .ins__dot--warning { background: rgba(196,150,60,0.8); }
    .ins__dot--success { background: var(--aurora); }
    .ins__dot--info    { background: rgba(100,160,220,0.8); }

    .ins__card h3 {
      font-size: 14px;
      font-weight: 500;
      color: var(--text);
      margin: 0;
      line-height: 1.4;
    }

    .ins__explanation {
      font-size: 12.5px;
      color: var(--text-muted);
      margin: 0;
      line-height: 1.6;
    }

    .ins__card-footer { display: grid; gap: 8px; }

    .ins__impact, .ins__action { display: grid; gap: 2px; }

    .ins__impact-label, .ins__action-label {
      font-size: 9px;
      letter-spacing: 0.16em;
      text-transform: uppercase;
      color: var(--text-faint);
      font-weight: 500;
    }

    .ins__impact > span:last-child,
    .ins__action > span:last-child {
      font-size: 12px;
      color: var(--text-muted);
      line-height: 1.5;
    }

    .ins__action > span:last-child { color: var(--text); }

    /* Empty state */
    .ins__none {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 10px;
      padding: 48px 24px;
      border: 1px solid var(--line);
      border-radius: 4px;
      text-align: center;
    }

    .ins__none-icon {
      font-size: 28px;
      color: var(--aurora);
    }

    .ins__none strong {
      font-size: 15px;
      font-weight: 500;
      color: var(--text);
    }

    .ins__none p {
      font-size: 13px;
      color: var(--text-faint);
      margin: 0;
      max-width: 380px;
      line-height: 1.6;
    }

    .ins__empty {
      margin: 0;
      padding: 16px;
      font-size: 13px;
      color: var(--text-faint);
      border: 1px solid var(--line);
      border-radius: 4px;
      text-align: center;
    }

    @media (max-width: 900px) {
      .ins { padding: 20px 16px; }
      .ins__header { flex-direction: column; }
      .ins__header-actions { align-items: flex-start; }
      .ins__grid { grid-template-columns: 1fr; }
    }
  `,
})
export class Insights {
  private readonly erpApi = inject(ErpApi);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly feedback = signal<UiFeedback | null>(null);
  protected readonly insights = signal<InsightCard[]>([]);
  protected readonly generatedAt = signal<string | null>(null);

  protected readonly alerts = computed(() =>
    this.insights().filter((i) => i.tone === 'danger' || i.tone === 'warning'),
  );

  protected readonly opportunities = computed(() =>
    this.insights().filter((i) => i.tone === 'success' || i.tone === 'info'),
  );

  protected readonly categoryCounts = computed(() => {
    const counts: Record<string, { count: number; tone: Tone }> = {};
    for (const i of this.insights()) {
      if (!counts[i.tone]) counts[i.tone] = { count: 0, tone: i.tone };
      counts[i.tone].count++;
    }
    const labels: Record<Tone, string> = {
      danger: 'Crítico',
      warning: 'Atención',
      success: 'Positivo',
      info: 'Info',
    };
    return Object.entries(counts).map(([tone, v]) => ({
      label: labels[tone as Tone] ?? tone,
      tone: tone as Tone,
      count: v.count,
    }));
  });

  constructor() {
    this.load();
  }

  protected load(forceRefresh = false): void {
    this.loading.set(true);
    this.feedback.set(null);

    this.erpApi
      .getDashboardSnapshot(forceRefresh)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (snap) => {
          this.generatedAt.set(snap.generatedAt);
          const cards = this.mapInsights(snap);
          this.insights.set(cards);
          this.loading.set(false);
        },
        error: (err) => {
          this.feedback.set(this.httpFeedback.fromError(err, 'No fue posible cargar los insights del negocio.'));
          this.loading.set(false);
        },
      });
  }

  private mapInsights(snap: DashboardSnapshot): InsightCard[] {
    const cards: InsightCard[] = [];

    // From snapshot.insights
    for (let i = 0; i < snap.insights.length; i++) {
      const ins = snap.insights[i];
      cards.push({
        id: `insight-${i}`,
        tone: this.toTone(ins.tone),
        category: ins.category ?? 'Análisis',
        title: ins.title,
        explanation: ins.explanation,
        impact: ins.impact,
        actionRecommended: ins.actionRecommended,
      });
    }

    // From snapshot.alertas — map to InsightCard
    for (let i = 0; i < snap.alertas.length; i++) {
      const a = snap.alertas[i];
      cards.push({
        id: `alerta-${i}`,
        tone: this.toTone(a.level),
        category: 'Alerta',
        title: a.title,
        explanation: a.detail,
        impact: 'Requiere atención inmediata.',
        actionRecommended: 'Revisa y toma acción.',
      });
    }

    // From low-stock products
    if (snap.productosBajoStock?.length) {
      cards.push({
        id: 'low-stock',
        tone: 'warning',
        category: 'Inventario',
        title: `${snap.productosBajoStock.length} producto${snap.productosBajoStock.length > 1 ? 's' : ''} bajo mínimo`,
        explanation: snap.productosBajoStock.slice(0, 3).map((p) => p.name).join(', ') + (snap.productosBajoStock.length > 3 ? '...' : ''),
        impact: 'Riesgo de quiebre de stock y pérdida de ventas.',
        actionRecommended: 'Genera una orden de compra o ajusta el nivel mínimo según la demanda real.',
      });
    }

    // Pending SRI
    if (snap.documentosSriPendientes > 0) {
      cards.push({
        id: 'sri-pending',
        tone: 'warning',
        category: 'SRI',
        title: `${snap.documentosSriPendientes} documento${snap.documentosSriPendientes > 1 ? 's' : ''} SRI pendiente${snap.documentosSriPendientes > 1 ? 's' : ''}`,
        explanation: 'Hay comprobantes que aún no han sido enviados o autorizados por el SRI.',
        impact: 'Incumplimiento tributario y riesgo de multa.',
        actionRecommended: 'Ingresa al módulo SRI, revisa los documentos y transmite los pendientes.',
      });
    }

    // Pending transfers
    if (snap.pagosTransferenciaPendientes?.length) {
      cards.push({
        id: 'transfers-pending',
        tone: 'info',
        category: 'Cobros',
        title: `${snap.pagosTransferenciaPendientes.length} transferencia${snap.pagosTransferenciaPendientes.length > 1 ? 's' : ''} por confirmar`,
        explanation: 'Hay pagos por transferencia bancaria que esperan confirmación.',
        impact: 'Ingresos no registrados que afectan el flujo de caja real.',
        actionRecommended: 'Verifica el comprobante bancario y confirma el pago en el sistema.',
      });
    }

    // Sales trend insight
    const delta = snap.comparacionVentas?.deltaPorcentaje ?? 0;
    if (delta <= -10) {
      cards.push({
        id: 'sales-drop',
        tone: 'danger',
        category: 'Ventas',
        title: `Caída de ventas del ${Math.abs(delta).toFixed(1)}% vs ayer`,
        explanation: `Las ventas de hoy son significativamente menores al periodo anterior.`,
        impact: 'Impacto directo en caja y cobertura de costos fijos.',
        actionRecommended: 'Revisa si hubo problemas operativos o de demanda. Considera promociones.',
      });
    } else if (delta >= 15) {
      cards.push({
        id: 'sales-up',
        tone: 'success',
        category: 'Ventas',
        title: `Ventas creciendo +${delta.toFixed(1)}% vs ayer`,
        explanation: `El negocio está teniendo un rendimiento superior al periodo anterior.`,
        impact: 'Oportunidad de mejorar márgenes y fortalecer el momentum.',
        actionRecommended: 'Asegura stock suficiente para sostener la demanda. Evalúa qué está funcionando.',
      });
    }

    return cards;
  }

  private toTone(raw: string): Tone {
    const map: Record<string, Tone> = {
      danger: 'danger',
      warning: 'warning',
      warn: 'warning',
      error: 'danger',
      success: 'success',
      ok: 'success',
      info: 'info',
      information: 'info',
    };
    return map[raw?.toLowerCase()] ?? 'info';
  }
}
