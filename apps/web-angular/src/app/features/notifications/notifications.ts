import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ErpApi } from '../../core/services/erp-api';
import { HttpFeedback } from '../../core/services/http-feedback';
import { ToastService } from '../../core/services/toast';
import { Paginator } from '../shared/components/paginator';

type NotifTone = 'danger' | 'warning' | 'info' | 'success';
type NotifCategory = 'stock' | 'payment' | 'sri' | 'sales' | 'system';

type Notif = {
  id: string;
  tone: NotifTone;
  category: NotifCategory;
  title: string;
  detail: string;
  time: string;
  read: boolean;
  link?: string;
};

type FilterTab = 'all' | 'unread' | 'stock' | 'payment' | 'sri';

@Component({
  selector: 'app-notifications',
  imports: [DatePipe, Paginator],
  template: `
    <div class="nt page">

      <!-- Header -->
      <div class="nt__head">
        <div>
          <p class="nt__eyebrow">Centro de notificaciones</p>
          <h2 class="nt__title">
            Bandeja de alertas
            @if (unreadCount() > 0) {
              <span class="nt__badge">{{ unreadCount() }}</span>
            }
          </h2>
          <p class="nt__desc">Todo lo que requiere atención hoy, ordenado por prioridad.</p>
        </div>
        <div class="nt__head-actions">
          @if (unreadCount() > 0) {
            <button
              type="button"
              class="nt__mark-all"
              [disabled]="markingAll()"
              [class.nt__mark-all--busy]="markingAll()"
              (click)="markAllRead()"
            >
              @if (markingAll()) {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" class="spinning" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"/>
                </svg>
                Marcando…
              } @else {
                Marcar todo como leído
              }
            </button>
          }
          <span class="nt__refresh-time">
            Actualizado: {{ lastRefresh() | date:'HH:mm:ss' }}
          </span>
          <button type="button" class="nt__refresh" (click)="load()" [disabled]="loading()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" [class.spinning]="loading()">
              <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"/>
            </svg>
            Actualizar
          </button>
        </div>
      </div>

      <!-- Filter tabs -->
      <div class="nt__tabs">
        @for (tab of tabs; track tab.id) {
          <button
            type="button"
            class="nt__tab"
            [class.nt__tab--active]="activeTab() === tab.id"
            (click)="activeTab.set(tab.id)"
          >
            {{ tab.label }}
            @if (tab.count() > 0) {
              <span class="nt__tab-count">{{ tab.count() }}</span>
            }
          </button>
        }
      </div>

      @if (loading()) {
        <div class="nt__skeletons">
          @for (i of [1,2,3,4]; track i) {
            <div class="nt__skeleton"></div>
          }
        </div>
      } @else if (error()) {
        <div class="nt__empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"/>
          </svg>
          <p>{{ error() }}</p>
          <button type="button" class="nt__retry" (click)="load()">Reintentar</button>
        </div>
      } @else if (filtered().length === 0) {
        <div class="nt__empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"/>
          </svg>
          <p>No hay notificaciones en esta categoría.</p>
        </div>
      } @else {
        <div class="nt__list">
          @for (notif of paged(); track notif.id) {
            <div
              class="nt__item"
              [class.nt__item--unread]="!notif.read"
              [attr.data-tone]="notif.tone"
              (click)="markRead(notif)"
            >
              <div class="nt__item-icon" [attr.data-tone]="notif.tone">
                @switch (notif.tone) {
                  @case ('danger') {
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"/>
                    </svg>
                  }
                  @case ('warning') {
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"/>
                    </svg>
                  }
                  @case ('success') {
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                  }
                  @default {
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z"/>
                    </svg>
                  }
                }
              </div>

              <div class="nt__item-body">
                <div class="nt__item-header">
                  <strong class="nt__item-title">{{ notif.title }}</strong>
                  <span class="nt__item-time">{{ notif.time | date:'dd/MM HH:mm' }}</span>
                </div>
                <p class="nt__item-detail">{{ notif.detail }}</p>
                <div class="nt__item-footer">
                  <span class="nt__category-chip" [attr.data-cat]="notif.category">
                    {{ categoryLabel(notif.category) }}
                  </span>
                  @if (!notif.read) {
                    <span class="nt__unread-dot"></span>
                  }
                </div>
              </div>

              <button
                type="button"
                class="nt__item-dismiss"
                (click)="dismiss(notif, $event)"
                aria-label="Descartar"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>
            </div>
          }
        </div>
        <app-paginator
          [page]="page()"
          [pageSize]="pageSize()"
          [totalElements]="filtered().length"
          (pageChange)="page.set($event)"
        />
      }

    </div>
  `,
  styles: `
    .nt {
      padding: 28px 32px;
      min-height: 100%;
    }

    .nt__head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 16px;
      margin-bottom: 20px;
      flex-wrap: wrap;
    }

    .nt__eyebrow {
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: var(--aurora);
      margin: 0 0 4px;
    }

    .nt__title {
      font-size: 22px;
      font-weight: 700;
      color: var(--text-strong);
      margin: 0 0 4px;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .nt__badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 22px;
      height: 22px;
      padding: 0 6px;
      background: var(--danger);
      color: #fff;
      border-radius: 11px;
      font-size: 12px;
      font-weight: 700;
    }

    .nt__desc {
      font-size: 13px;
      color: var(--text-muted);
      margin: 0;
    }

    .nt__head-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }

    .nt__mark-all {
      padding: 7px 14px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 8px;
      font-size: 12px;
      color: var(--text-muted);
      cursor: pointer;
      transition: all var(--dur-fast) ease;
    }

    .nt__mark-all:hover { background: var(--ok-bg); color: var(--ok); border-color: transparent; }

    .nt__refresh {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 7px 14px;
      background: var(--aurora-ghost);
      border: 1px solid var(--aurora-border);
      border-radius: 8px;
      font-size: 12px;
      color: var(--aurora);
      cursor: pointer;
      font-weight: 500;
      transition: all var(--dur-fast) ease;
    }

    .nt__refresh:hover { background: var(--aurora); color: var(--bg); }
    .nt__refresh:disabled { opacity: 0.5; cursor: not-allowed; }
    .nt__refresh svg { width: 14px; height: 14px; }

    .nt__refresh-time {
      font-size: 10px;
      color: var(--text-faint);
      white-space: nowrap;
    }

    @keyframes spin { to { transform: rotate(360deg); } }
    .spinning { animation: spin 1s linear infinite; }

    /* Tabs */
    .nt__tabs {
      display: flex;
      gap: 8px;
      margin-bottom: 22px;
      border-bottom: 1px solid var(--line, #e4e6eb);
      padding-bottom: 0;
      flex-wrap: wrap;
      background: linear-gradient(180deg, transparent 0%, var(--surface-muted, rgba(0,0,0,0.02)) 100%);
      border-radius: 6px 6px 0 0;
      padding: 4px 4px 0;
    }

    .nt__tab {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 18px;
      background: none;
      border: none;
      border-bottom: 3px solid transparent;
      margin-bottom: -1px;
      font-size: 13px;
      font-weight: 500;
      color: var(--text-muted);
      cursor: pointer;
      transition: all var(--dur-fast) ease;
      white-space: nowrap;
      border-radius: 4px 4px 0 0;
    }

    .nt__tab:hover {
      color: var(--text);
      background: var(--bg-hover, rgba(99, 102, 241, 0.05));
    }
    .nt__tab--active {
      color: var(--aurora);
      border-bottom-color: var(--aurora);
      font-weight: 700;
      background: var(--aurora-ghost, rgba(99, 102, 241, 0.08));
    }

    .nt__tab-count {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 18px;
      height: 18px;
      padding: 0 5px;
      background: var(--danger-bg);
      color: var(--danger);
      border-radius: 9px;
      font-size: 10px;
      font-weight: 700;
    }

    /* Skeleton */
    .nt__skeletons { display: flex; flex-direction: column; gap: 10px; }

    .nt__skeleton {
      height: 76px;
      background: var(--surface);
      border-radius: 10px;
      animation: fadeIn 1.5s ease infinite alternate;
    }

    /* Empty */
    .nt__empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      padding: 64px 24px;
      color: var(--text-faint);
      text-align: center;
    }

    .nt__empty svg { width: 40px; height: 40px; }
    .nt__empty p { font-size: 14px; margin: 0; }

    .nt__retry {
      padding: 8px 20px;
      background: var(--aurora-ghost);
      border: 1px solid var(--aurora-border);
      border-radius: 8px;
      color: var(--aurora);
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
    }

    /* List */
    .nt__list { display: flex; flex-direction: column; gap: 8px; }

    .nt__item {
      display: flex;
      align-items: flex-start;
      gap: 14px;
      padding: 14px 16px;
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 10px;
      cursor: pointer;
      transition: all var(--dur-fast) ease;
      position: relative;
      animation: slideUp var(--dur-normal) var(--ease-out) both;
    }

    .nt__item:hover { background: var(--surface); border-color: var(--line-strong); }

    .nt__item--unread {
      border-left: 3px solid var(--aurora);
    }

    [data-tone="danger"].nt__item--unread  { border-left-color: var(--danger); }
    [data-tone="warning"].nt__item--unread { border-left-color: var(--warn); }
    [data-tone="success"].nt__item--unread { border-left-color: var(--ok); }
    [data-tone="info"].nt__item--unread    { border-left-color: var(--info); }

    .nt__item-icon {
      width: 36px;
      height: 36px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      margin-top: 1px;
    }

    .nt__item-icon svg { width: 18px; height: 18px; }

    [data-tone="danger"]  .nt__item-icon, .nt__item-icon[data-tone="danger"]  { background: var(--danger-bg); color: var(--danger); }
    [data-tone="warning"] .nt__item-icon, .nt__item-icon[data-tone="warning"] { background: var(--warn-bg);   color: var(--warn);   }
    [data-tone="success"] .nt__item-icon, .nt__item-icon[data-tone="success"] { background: var(--ok-bg);     color: var(--ok);     }
    [data-tone="info"]    .nt__item-icon, .nt__item-icon[data-tone="info"]    { background: var(--info-bg);   color: var(--info);   }

    .nt__item-body { flex: 1; min-width: 0; }

    .nt__item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;
    }

    .nt__item-title { font-size: 13px; font-weight: 600; color: var(--text-strong); }
    .nt__item-time  { font-size: 11px; color: var(--text-faint); white-space: nowrap; flex-shrink: 0; }
    .nt__item-detail { font-size: 12px; color: var(--text-muted); margin: 0 0 6px; line-height: 1.4; }

    .nt__item-footer { display: flex; align-items: center; gap: 8px; }

    .nt__category-chip {
      font-size: 10px;
      font-weight: 600;
      padding: 2px 8px;
      border-radius: 10px;
      background: var(--surface-strong);
      color: var(--text-muted);
    }

    [data-cat="stock"]   .nt__category-chip, .nt__category-chip[data-cat="stock"]   { background: var(--warn-bg);   color: var(--warn);   }
    [data-cat="payment"] .nt__category-chip, .nt__category-chip[data-cat="payment"] { background: var(--ok-bg);     color: var(--ok);     }
    [data-cat="sri"]     .nt__category-chip, .nt__category-chip[data-cat="sri"]     { background: var(--info-bg);   color: var(--info);   }
    [data-cat="sales"]   .nt__category-chip, .nt__category-chip[data-cat="sales"]   { background: var(--aurora-ghost); color: var(--aurora); }

    .nt__unread-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--aurora);
      flex-shrink: 0;
    }

    .nt__item-dismiss {
      width: 28px;
      height: 28px;
      flex-shrink: 0;
      background: none;
      border: none;
      cursor: pointer;
      color: var(--text-faint);
      padding: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 6px;
      transition: all var(--dur-fast) ease;
    }

    .nt__item-dismiss:hover { color: var(--text); background: var(--surface-strong); }
    .nt__item-dismiss svg { width: 12px; height: 12px; }
  `,
})
export class Notifications implements OnInit, OnDestroy {
  private readonly api = inject(ErpApi);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly markingAll = signal(false);
  protected readonly error = signal('');
  protected readonly notifs = signal<Notif[]>([]);
  protected readonly activeTab = signal<FilterTab>('all');
  protected readonly lastRefresh = signal<Date>(new Date());

  /** Simulated WebSocket: auto-refresh every 30s */
  private refreshTimer: ReturnType<typeof setInterval> | null = null;

  protected readonly unreadCount = computed(() => this.notifs().filter((n) => !n.read).length);

  protected readonly stockCount   = computed(() => this.notifs().filter((n) => n.category === 'stock' && !n.read).length);
  protected readonly paymentCount = computed(() => this.notifs().filter((n) => n.category === 'payment' && !n.read).length);
  protected readonly sriCount     = computed(() => this.notifs().filter((n) => n.category === 'sri' && !n.read).length);

  protected readonly tabs = [
    { id: 'all'     as FilterTab, label: 'Todas',      count: computed(() => this.notifs().length) },
    { id: 'unread'  as FilterTab, label: 'Sin leer',   count: this.unreadCount },
    { id: 'stock'   as FilterTab, label: 'Inventario', count: this.stockCount },
    { id: 'payment' as FilterTab, label: 'Pagos',      count: this.paymentCount },
    { id: 'sri'     as FilterTab, label: 'SRI',        count: this.sriCount },
  ];

  protected readonly filtered = computed(() => {
    const tab = this.activeTab();
    const list = this.notifs();
    if (tab === 'all')     return list;
    if (tab === 'unread')  return list.filter((n) => !n.read);
    if (tab === 'stock')   return list.filter((n) => n.category === 'stock');
    if (tab === 'payment') return list.filter((n) => n.category === 'payment');
    if (tab === 'sri')     return list.filter((n) => n.category === 'sri');
    return list;
  });

  protected readonly page = signal(0);
  protected readonly pageSize = signal(25);

  protected readonly paged = computed(() => {
    const start = this.page() * this.pageSize();
    return this.filtered().slice(start, start + this.pageSize());
  });

  ngOnInit(): void {
    this.load();
    // Simulate WebSocket: poll every 30s for new alerts
    this.refreshTimer = setInterval(() => this.silentRefresh(), 30_000);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.getDashboardSnapshot().subscribe({
      next: (snap) => {
        this.notifs.set(this.buildNotifs(snap));
        this.lastRefresh.set(new Date());
        this.loading.set(false);
      },
      error: (error) => {
        const feedback = this.httpFeedback.fromError(error, 'No se pudo cargar la bandeja. Intenta de nuevo.');
        this.error.set(feedback.message);
        this.loading.set(false);
      },
    });
  }

  private silentRefresh(): void {
    this.api.getDashboardSnapshot().subscribe({
      next: (snap) => {
        const fresh = this.buildNotifs(snap);
        const existingIds = new Set(this.notifs().map((n) => n.id));
        const newOnes = fresh.filter((n) => !existingIds.has(n.id));
        if (newOnes.length > 0) {
          this.notifs.update((list) => [...newOnes, ...list]);
          this.toast.info(`${newOnes.length} nueva${newOnes.length > 1 ? 's' : ''} alerta${newOnes.length > 1 ? 's' : ''}`, 'Revisa tu bandeja de notificaciones.');
        }
        this.lastRefresh.set(new Date());
      },
      error: () => { /* silent */ },
    });
  }

  protected markRead(notif: Notif): void {
    this.notifs.update((list) =>
      list.map((n) => (n.id === notif.id ? { ...n, read: true } : n)),
    );
  }

  protected markAllRead(): void {
    if (this.markingAll() || this.unreadCount() === 0) return;
    this.markingAll.set(true);
    // No hay endpoint dedicado: aplicamos cambio local con un pequeño delay para feedback visible.
    queueMicrotask(() => {
      try {
        this.notifs.update((list) => list.map((n) => ({ ...n, read: true })));
        this.toast.success('Bandeja actualizada', 'Todas las notificaciones fueron marcadas como leídas.');
      } catch {
        this.toast.info('No se pudieron marcar', 'Inténtalo nuevamente en unos segundos.');
      } finally {
        this.markingAll.set(false);
      }
    });
  }

  protected dismiss(notif: Notif, event: Event): void {
    event.stopPropagation();
    this.notifs.update((list) => list.filter((n) => n.id !== notif.id));
  }

  protected categoryLabel(cat: NotifCategory): string {
    const map: Record<NotifCategory, string> = {
      stock: 'Inventario', payment: 'Pagos', sri: 'SRI', sales: 'Ventas', system: 'Sistema',
    };
    return map[cat] ?? cat;
  }

  private buildNotifs(snap: any): Notif[] {
    const now = new Date().toISOString();
    const list: Notif[] = [];

    // Alertas del sistema
    for (const alerta of snap.alertas ?? []) {
      list.push({
        id: `alerta-${list.length}`,
        tone: alerta.level === 'DANGER' ? 'danger' : alerta.level === 'WARNING' ? 'warning' : 'info',
        category: 'sales',
        title: alerta.title,
        detail: alerta.detail,
        time: now,
        read: false,
      });
    }

    // Productos bajo stock
    for (const p of snap.productosBajoStock ?? []) {
      list.push({
        id: `stock-${p.id}`,
        tone: p.availableStock === 0 ? 'danger' : 'warning',
        category: 'stock',
        title: `Stock ${p.availableStock === 0 ? 'agotado' : 'bajo'}: ${p.name}`,
        detail: `Disponible: ${p.availableStock} — Mínimo recomendado: ${p.minStock}`,
        time: now,
        read: false,
      });
    }

    // Pagos por transferencia pendientes
    for (const t of snap.pagosTransferenciaPendientes ?? []) {
      list.push({
        id: `transfer-${t.id}`,
        tone: 'warning',
        category: 'payment',
        title: 'Transferencia por confirmar',
        detail: `${t.userName ?? t.userEmail ?? 'Cliente'} — $${t.total?.toFixed(2)} · Ref: ${t.paymentReference ?? 'Sin referencia'}`,
        time: t.createdAt ?? now,
        read: false,
      });
    }

    // Documentos SRI pendientes
    if ((snap.documentosSriPendientes ?? 0) > 0) {
      list.push({
        id: 'sri-pending',
        tone: 'info',
        category: 'sri',
        title: `${snap.documentosSriPendientes} documentos SRI pendientes`,
        detail: 'Revisa el módulo SRI para enviar y autorizar los documentos electrónicos pendientes.',
        time: now,
        read: false,
      });
    }

    // Cuentas por cobrar
    if ((snap.cuentasPorCobrarAbiertas ?? 0) > 0) {
      list.push({
        id: 'receivables',
        tone: 'warning',
        category: 'payment',
        title: `${snap.cuentasPorCobrarAbiertas} cuentas por cobrar abiertas`,
        detail: 'Hay cuentas pendientes de cobro. Revisa el módulo de Contabilidad.',
        time: now,
        read: false,
      });
    }

    // Cuentas por pagar
    if ((snap.cuentasPorPagarAbiertas ?? 0) > 0) {
      list.push({
        id: 'payables',
        tone: 'info',
        category: 'payment',
        title: `${snap.cuentasPorPagarAbiertas} cuentas por pagar pendientes`,
        detail: 'Hay pagos a proveedores por registrar. Revisa Compras o Contabilidad.',
        time: now,
        read: false,
      });
    }

    // Si no hay nada, agrega un mensaje positivo
    if (list.length === 0) {
      list.push({
        id: 'all-clear',
        tone: 'success',
        category: 'system',
        title: 'Todo en orden',
        detail: 'No hay alertas activas. El negocio está operando sin incidentes reportados.',
        time: now,
        read: true,
      });
    }

    return list;
  }
}
