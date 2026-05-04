import {
  Component,
  computed,
  DestroyRef,
  HostListener,
  inject,
  signal,
} from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ErpApi, SriDocumentDto } from '../../core/services/erp-api';
import { HttpFeedback } from '../../core/services/http-feedback';
import { ToastService } from '../../core/services/toast';
import { Paginator } from '../shared/components/paginator';

// ── Types ──────────────────────────────────────────────────────────────────

type CustomerTier   = 'VIP' | 'Frecuente' | 'Nuevo';
type SegmentFilter  = 'ALL' | 'VIP' | 'Frecuente' | 'Nuevo';
type SortCol        = 'name' | 'spent' | 'docs' | 'last' | 'avg';
type SortDir        = 'asc' | 'desc';

type Customer = {
  id:            string;
  name:          string;
  identification: string;
  totalSpent:    number;
  totalDocs:     number;
  lastPurchase:  string;
  avgTicket:     number;
  tier:          CustomerTier;
  docs:          SriDocumentDto[];
};

// ── Component ──────────────────────────────────────────────────────────────

@Component({
  selector: 'app-customers',
  imports: [CurrencyPipe, DatePipe, FormsModule, Paginator],
  template: `
    <section class="page">

      <!-- ══ HEADER ══════════════════════════════════════════════════════ -->
      <header class="page__header">
        <div>
          <span class="eyebrow">Relación comercial</span>
          <h1>Clientes</h1>
          <p>Historial de compras, documentos emitidos y análisis por cliente.</p>
        </div>
        <div class="page__acts">
          <button class="btn-outline" (click)="exportCsv()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3"/>
            </svg>
            Exportar CSV
          </button>
        </div>
      </header>

      <!-- ══ KPI STRIP ═══════════════════════════════════════════════════ -->
      <div class="kpi-row">
        <div class="kpi" [class.kpi--active]="segment() === 'ALL'" (click)="segment.set('ALL')">
          <span class="kpi__label">Clientes únicos</span>
          <span class="kpi__val">{{ totalCustomers() }}</span>
        </div>
        <div class="kpi">
          <span class="kpi__label">Documentos emitidos</span>
          <span class="kpi__val">{{ totalDocs() }}</span>
        </div>
        <div class="kpi">
          <span class="kpi__label">Facturación total</span>
          <span class="kpi__val kpi__val--money">{{ totalRevenue() | currency:'USD':'symbol':'1.0-0' }}</span>
        </div>
        <div class="kpi">
          <span class="kpi__label">Ticket promedio</span>
          <span class="kpi__val kpi__val--money">{{ avgTicketGlobal() | currency:'USD':'symbol':'1.0-0' }}</span>
        </div>
        <div class="kpi kpi--highlight" [class.kpi--empty]="!hasTopCustomer()">
          <span class="kpi__label">Top cliente</span>
          @if (hasTopCustomer()) {
            <span class="kpi__val kpi__val--sm">{{ topCustomerName() }}</span>
            <span class="kpi__sub">{{ topCustomerSpent() | currency:'USD':'symbol':'1.2-2' }}</span>
          } @else {
            <span class="kpi__val kpi__val--sm kpi__val--empty">Sin datos suficientes</span>
            <span class="kpi__sub">Registra ventas para identificar al cliente líder.</span>
          }
        </div>
      </div>

      <!-- ══ MAIN: FORM + TABLE ══════════════════════════════════════════ -->
      <section class="card">
        <header class="card__hd">
          <div><span class="eyebrow">Directorio</span><h2>Clientes registrados</h2></div>
          <div class="tbl-controls">
            <input
              class="search-field"
              [(ngModel)]="searchQuery"
              placeholder="Buscar nombre o identificación..."
            />
            @if (searchQuery() || segment() !== 'ALL') {
              <button class="ghost ghost--sm" (click)="clearFilters()" title="Limpiar filtros">✕</button>
            }
            <div class="tabs">
              @for (seg of segmentOptions; track seg.value) {
                <button
                  class="tab"
                  [class.tab--on]="segment() === seg.value"
                  (click)="segment.set(seg.value)"
                >
                  {{ seg.label }}
                  @if (seg.value !== 'ALL' && seg.count() > 0) {
                    <em class="tab-badge">{{ seg.count() }}</em>
                  }
                </button>
              }
            </div>
          </div>
        </header>

        @if (loading()) {
          <p class="state-msg">Consultando clientes...</p>
        } @else if (errorMsg()) {
          <p class="state-msg state-msg--error">
            {{ errorMsg() }}
            <button class="link-btn" (click)="load()">Reintentar</button>
          </p>
        } @else if (!sorted().length) {
          <p class="state-msg">
            @if (customers().length === 0) {
              Todavía no hay clientes con documentos emitidos.
            } @else {
              Sin resultados para los filtros aplicados.
              <button class="link-btn" (click)="clearFilters()">Limpiar filtros</button>
            }
          </p>
        } @else {
          <div class="tbl">
            <!-- sortable header -->
            <div class="tbl__head">
              <span class="th-sort" (click)="toggleSort('name')">Cliente {{ sortIcon('name') }}</span>
              <span>Identificación</span>
              <span>Segmento</span>
              <span class="num th-sort" (click)="toggleSort('docs')">Documentos {{ sortIcon('docs') }}</span>
              <span class="num th-sort" (click)="toggleSort('avg')">Ticket prom. {{ sortIcon('avg') }}</span>
              <span class="num th-sort" (click)="toggleSort('spent')">Total facturado {{ sortIcon('spent') }}</span>
              <span class="th-sort" (click)="toggleSort('last')">Última compra {{ sortIcon('last') }}</span>
              <span>Acciones</span>
            </div>

            @for (c of paged(); track c.id) {
              <div
                class="tbl__row"
                [class.tbl__row--selected]="selected()?.id === c.id"
                (click)="openPanel(c)"
              >
                <div class="cell-customer">
                  <div class="avatar" [attr.data-tier]="c.tier">{{ initial(c.name) }}</div>
                  <span class="cell-customer__name">{{ c.name }}</span>
                </div>
                <span class="mono">{{ c.identification }}</span>
                <span>
                  <span class="tier-chip" [attr.data-tier]="c.tier">{{ c.tier }}</span>
                </span>
                <span class="num">
                  <em class="doc-badge">{{ c.totalDocs }}</em>
                </span>
                <span class="num muted">{{ c.avgTicket | currency:'USD':'symbol':'1.2-2' }}</span>
                <span class="num tbl__total">{{ c.totalSpent | currency:'USD':'symbol':'1.2-2' }}</span>
                <span class="muted">{{ c.lastPurchase | date:'dd/MM/yyyy' }}</span>
                <div class="tbl__acts" (click)="$event.stopPropagation()">
                  <button class="act-btn" (click)="openPanel(c)">Ver</button>
                  <button class="act-btn" (click)="sendPromo(c, $event)" title="Enviar promoción">Promo</button>
                </div>
              </div>
            }
          </div>

          <app-paginator
            [page]="page()"
            [pageSize]="pageSize()"
            [totalElements]="sorted().length"
            (pageChange)="page.set($event)"
          />

          <!-- footer -->
          <div class="tbl-footer">
            <span>{{ sorted().length }} cliente{{ sorted().length !== 1 ? 's' : '' }}</span>
            <span>Total visible: <strong>{{ footerTotal() | currency:'USD':'symbol':'1.2-2' }}</strong></span>
            <span>Documentos: <strong>{{ footerDocs() }}</strong></span>
            @if (searchQuery() || segment() !== 'ALL') {
              <button class="link-btn" (click)="clearFilters()">Ver todos</button>
            }
          </div>
        }
      </section>

      <!-- ══ MODAL: DETALLE CLIENTE ═════════════════════════════════════ -->
      @if (selected()) {
        <div class="modal-bd" (click)="closePanel()">
          <div class="modal-box" (click)="$event.stopPropagation()">

            <!-- Modal header -->
            <header class="modal__hd">
              <div class="modal__identity">
                <div class="modal-avatar" [attr.data-tier]="selected()!.tier">
                  {{ initial(selected()!.name) }}
                </div>
                <div class="modal__identity-info">
                  <div class="modal__identity-top">
                    <h3 class="modal__name">{{ selected()!.name }}</h3>
                    <span class="tier-chip tier-chip--lg" [attr.data-tier]="selected()!.tier">
                      {{ selected()!.tier }}
                    </span>
                  </div>
                  <span class="mono modal__id">{{ selected()!.identification }}</span>
                </div>
              </div>
              <button class="icon-btn" (click)="closePanel()" aria-label="Cerrar">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>
            </header>

            <!-- KPI stats row -->
            <div class="modal-kpis">
              <div class="modal-kpi">
                <span class="modal-kpi__label">Documentos</span>
                <strong class="modal-kpi__val">{{ selected()!.totalDocs }}</strong>
              </div>
              <div class="modal-kpi">
                <span class="modal-kpi__label">Facturado</span>
                <strong class="modal-kpi__val">{{ selected()!.totalSpent | currency:'USD':'symbol':'1.0-0' }}</strong>
              </div>
              <div class="modal-kpi">
                <span class="modal-kpi__label">Ticket prom.</span>
                <strong class="modal-kpi__val">{{ selected()!.avgTicket | currency:'USD':'symbol':'1.0-0' }}</strong>
              </div>
              <div class="modal-kpi">
                <span class="modal-kpi__label">Días sin comprar</span>
                <strong class="modal-kpi__val">{{ daysSinceLast(selected()!.lastPurchase) }}</strong>
              </div>
            </div>

            <!-- Recurrence insight -->
            @if (selected()!.totalDocs > 1) {
              <div class="modal-insight">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 3v11.25A2.25 2.25 0 006 16.5h2.25M3.75 3h-1.5m1.5 0h16.5m0 0h1.5m-1.5 0v11.25A2.25 2.25 0 0118 16.5h-2.25m-7.5 0h7.5m-7.5 0l-1 3m8.5-3l1 3m0 0l.5 1.5m-.5-1.5h-9.5m0 0l-.5 1.5"/>
                </svg>
                Compra cada <strong>{{ avgDaysBetween(selected()!.docs) }} días</strong> en promedio
              </div>
            }

            <!-- Actions -->
            <div class="modal__foot modal__foot--top">
              <button class="btn-primary" (click)="registerSale(selected()!)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.25L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z"/>
                </svg>
                Registrar venta
              </button>
              <button class="btn-outline" (click)="sendPromo(selected()!)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75"/>
                </svg>
                Enviar promo
              </button>
              <button class="ghost" (click)="closePanel()">Cerrar</button>
            </div>

            <!-- Document history -->
            <div class="modal__section">
              <span class="eyebrow">Historial de documentos</span>
              <div class="hist-list">
                @for (doc of panelDocs(); track doc.id) {
                  <div class="hist-item">
                    <div class="hist-item__left">
                      <span class="hist-item__code">{{ doc.documentCode }}</span>
                      <span class="hist-item__seq mono">{{ doc.sequentialNumber || '—' }}</span>
                    </div>
                    <div class="hist-item__right">
                      <span class="hist-item__amt">{{ doc.total | currency:'USD':'symbol':'1.2-2' }}</span>
                      <span class="hist-item__date">{{ doc.issueDate | date:'dd/MM/yy' }}</span>
                      <span class="status-chip" [attr.data-status]="doc.status">{{ statusLabel(doc.status) }}</span>
                    </div>
                  </div>
                }
                @if (selected()!.docs.length > panelDocLimit()) {
                  <button class="more-btn" (click)="panelDocLimit.set(panelDocLimit() + 10)">
                    Ver {{ selected()!.docs.length - panelDocLimit() }} más…
                  </button>
                }
              </div>
            </div>

          </div>
        </div>
      }

    </section>
  `,
  styles: `
    :host { display: contents; }

    /* ── page ─────────────────────────────────────────────────────────── */
    .page { display: grid; gap: 24px; padding: 32px; align-content: start; min-height: 100%; }

    /* ── header ───────────────────────────────────────────────────────── */
    .page__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; }
    .eyebrow {
      display: block;
      font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase;
      font-weight: 500; color: var(--aurora-dim); margin-bottom: 6px;
    }
    .page__header h1 {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 36px; font-weight: 400; color: var(--text-strong);
      margin: 0 0 4px; letter-spacing: -0.02em; line-height: 1.1;
    }
    .page__header p { font-size: 13px; font-weight: 300; color: var(--text-muted); margin: 0; line-height: 1.6; }
    .page__acts { display: flex; align-items: flex-start; gap: 8px; flex-shrink: 0; }

    /* ── buttons ──────────────────────────────────────────────────────── */
    .btn-outline {
      display: flex; align-items: center; gap: 6px;
      padding: 10px 20px; background: transparent;
      border: 1px solid var(--aurora-border); color: var(--aurora);
      font-size: 11px; font-weight: 500; letter-spacing: 0.12em;
      text-transform: uppercase; border-radius: 2px; cursor: pointer;
      transition: background 180ms ease; white-space: nowrap;
    }
    .btn-outline svg { width: 13px; height: 13px; }
    .btn-outline:hover { background: var(--aurora-ghost); }
    .btn-primary {
      display: flex; align-items: center; gap: 6px;
      padding: 10px 18px; background: var(--aurora); border: none;
      color: #fff; font-size: 11px; font-weight: 500;
      letter-spacing: 0.1em; text-transform: uppercase; border-radius: 2px;
      cursor: pointer; transition: opacity 160ms ease; white-space: nowrap;
    }
    .btn-primary svg { width: 13px; height: 13px; }
    .btn-primary:hover { opacity: 0.88; }
    .ghost {
      padding: 8px 14px; background: transparent;
      border: 1px solid var(--line-strong); color: var(--text-muted);
      font-size: 11px; border-radius: 2px; cursor: pointer;
      transition: border-color 140ms ease, color 140ms ease;
    }
    .ghost:hover { border-color: var(--aurora-border); color: var(--aurora); }
    .ghost--sm { padding: 6px 10px; }
    .link-btn {
      background: none; border: none; color: var(--aurora);
      font-size: 12px; cursor: pointer; text-decoration: underline; padding: 0;
    }

    /* ── KPI strip ────────────────────────────────────────────────────── */
    .kpi-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; }
    .kpi {
      padding: 18px 20px; background: var(--bg-panel);
      border: 1px solid var(--line); border-radius: 4px;
      cursor: pointer; transition: border-color 160ms ease, background 160ms ease;
    }
    .kpi:hover { border-color: var(--aurora-border); }
    .kpi--active { border-color: var(--aurora-border); background: var(--aurora-ghost); }
    .kpi--highlight { border-color: var(--aurora-dim); }
    .kpi__label { display: block; font-size: 10px; letter-spacing: 0.18em; text-transform: uppercase; color: var(--text-faint); margin-bottom: 8px; }
    .kpi__val {
      display: block;
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 30px; font-weight: 400; color: var(--text-strong);
      letter-spacing: -0.02em; margin-bottom: 3px;
    }
    .kpi__val--money { font-size: 22px; }
    .kpi__val--sm { font-size: 16px; line-height: 1.3; }
    .kpi__sub { font-size: 11px; font-weight: 300; color: var(--aurora); }

    /* ── card ─────────────────────────────────────────────────────────── */
    .card {
      padding: 20px; background: var(--bg-panel);
      border: 1px solid var(--line); border-radius: 4px;
      display: grid; gap: 16px; align-self: start;
    }
    .card__hd { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
    .card h2 { font-size: 13px; font-weight: 500; color: var(--text); margin: 0; }

    /* ── table controls ───────────────────────────────────────────────── */
    .tbl-controls { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .search-field {
      background: transparent; border: 1px solid var(--line-strong); border-radius: 2px;
      color: var(--text); padding: 7px 10px; font-size: 12px; font-weight: 300;
      outline: none; width: 230px; transition: border-color 160ms ease;
    }
    .search-field:focus { border-color: var(--aurora-border); }
    .search-field::placeholder { color: var(--text-faint); }

    .tabs { display: flex; gap: 2px; }
    .tab {
      padding: 6px 12px; background: transparent;
      border: 1px solid var(--line-strong); border-radius: 2px;
      color: var(--text-muted); font-size: 11px; cursor: pointer;
      transition: all 140ms ease; display: flex; align-items: center; gap: 5px;
    }
    .tab:hover { border-color: var(--aurora-border); color: var(--aurora); }
    .tab--on { border-color: var(--aurora-border); background: var(--aurora-ghost); color: var(--aurora); }
    .tab-badge {
      display: inline-flex; align-items: center; justify-content: center;
      background: var(--aurora); color: #fff; font-size: 9px;
      font-style: normal; font-weight: 600; min-width: 16px; height: 16px;
      padding: 0 4px; border-radius: 8px;
    }

    /* ── states ───────────────────────────────────────────────────────── */
    .state-msg {
      font-size: 13px; font-weight: 300; color: var(--text-faint);
      text-align: center; padding: 32px 0; display: flex;
      align-items: center; justify-content: center; gap: 8px;
    }
    .state-msg--error { color: var(--danger); }

    /* ── table ────────────────────────────────────────────────────────── */
    .tbl { display: grid; gap: 3px; }
    .tbl__head, .tbl__row {
      display: grid;
      grid-template-columns: 1.6fr 0.9fr 0.7fr 0.5fr 0.8fr 1fr 0.8fr 0.8fr;
      gap: 8px; align-items: center; padding: 10px 12px;
    }
    .tbl__head {
      font-size: 10px; text-transform: uppercase; letter-spacing: 0.16em;
      font-weight: 500; color: var(--text-faint);
      border-bottom: 1px solid var(--line);
    }
    .th-sort { cursor: pointer; user-select: none; transition: color 120ms ease; }
    .th-sort:hover { color: var(--aurora); }
    .tbl__row {
      background: var(--surface); border-radius: 3px;
      font-size: 12.5px; color: var(--text-muted); cursor: pointer;
      transition: background 120ms ease, border-color 120ms ease;
      border: 1px solid transparent;
    }
    .tbl__row:hover { background: var(--surface-strong); border-color: var(--line); }
    .tbl__row--selected { border-color: var(--aurora-border); background: var(--aurora-ghost); }
    .tbl__total { font-weight: 600; color: var(--text-strong); }
    .tbl__acts { display: flex; gap: 5px; }
    .num { text-align: right; }
    .muted { color: var(--text-muted); }
    .mono { font-family: monospace; font-size: 11.5px; }

    /* ── act buttons ──────────────────────────────────────────────────── */
    .act-btn {
      padding: 5px 10px; background: transparent;
      border: 1px solid var(--line-strong); border-radius: 2px;
      color: var(--text-muted); font-size: 10.5px; cursor: pointer;
      transition: all 120ms ease; white-space: nowrap;
    }
    .act-btn:hover { border-color: var(--aurora-border); color: var(--aurora); }

    /* ── customer cell ────────────────────────────────────────────────── */
    .cell-customer { display: flex; align-items: center; gap: 9px; }
    .cell-customer__name { font-weight: 500; color: var(--text-strong); }

    .avatar {
      width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0;
      background: var(--aurora-ghost); border: 1px solid var(--aurora-border);
      color: var(--aurora); font-size: 11px; font-weight: 600;
      display: flex; align-items: center; justify-content: center;
    }
    .avatar[data-tier="VIP"]       { background: var(--warn-bg);   border-color: var(--warn);   color: var(--warn); }
    .avatar[data-tier="Frecuente"] { background: var(--ok-bg);     border-color: var(--ok);     color: var(--ok);   }

    /* ── tier chips ───────────────────────────────────────────────────── */
    .tier-chip {
      display: inline-flex; align-items: center;
      padding: 1px 7px; border-radius: 2px;
      font-size: 9.5px; font-weight: 600;
      letter-spacing: 0.08em; text-transform: uppercase;
      background: var(--surface-strong); color: var(--text-faint);
      border: 1px solid var(--line);
    }
    .tier-chip[data-tier="VIP"]       { background: var(--warn-bg);   color: var(--warn);   border-color: var(--warn);   }
    .tier-chip[data-tier="Frecuente"] { background: var(--ok-bg);     color: var(--ok);     border-color: var(--ok);     }
    .tier-chip--lg { font-size: 10px; padding: 2px 9px; }

    /* ── doc badge ────────────────────────────────────────────────────── */
    .doc-badge {
      display: inline-flex; align-items: center; justify-content: center;
      min-width: 22px; height: 18px; padding: 0 5px;
      background: var(--surface-strong); border-radius: 9px;
      font-size: 10.5px; font-weight: 600; font-style: normal;
      color: var(--text);
    }

    /* ── table footer ─────────────────────────────────────────────────── */
    .tbl-footer {
      display: flex; align-items: center; gap: 16px;
      padding: 10px 12px 0; font-size: 12px; color: var(--text-faint);
      border-top: 1px solid var(--line); flex-wrap: wrap;
    }
    .tbl-footer strong { color: var(--text); }

    /* ── modal ────────────────────────────────────────────────────────── */
    .modal-bd {
      position: fixed; inset: 0; z-index: 1000;
      background: transparent;
      display: flex; align-items: center; justify-content: center;
      padding: 20px;
    }
    .modal-box {
      background: var(--bg-panel); border: 1px solid var(--line-strong);
      border-radius: 6px; width: 100%; max-width: 620px;
      max-height: 90vh; overflow-y: auto;
      display: grid; gap: 0;
      box-shadow: 0 24px 64px rgba(0,0,0,.22), 0 4px 16px rgba(0,0,0,.12);
      animation: slideUp var(--dur-normal) var(--ease-out) both;
    }

    /* ── modal header ─────────────────────────────────────────────────── */
    .modal__hd {
      display: flex; align-items: center; justify-content: space-between;
      gap: 16px; padding: 20px 24px 16px;
      border-bottom: 1px solid var(--line);
    }
    .modal__identity { display: flex; align-items: center; gap: 14px; flex: 1; min-width: 0; }
    .modal-avatar {
      width: 48px; height: 48px; border-radius: 50%; flex-shrink: 0;
      background: var(--aurora-ghost); border: 1.5px solid var(--aurora-border);
      color: var(--aurora); font-size: 18px; font-weight: 600;
      display: flex; align-items: center; justify-content: center;
    }
    .modal-avatar[data-tier="VIP"]       { background: var(--warn-bg);   border-color: var(--warn);   color: var(--warn); }
    .modal-avatar[data-tier="Frecuente"] { background: var(--ok-bg);     border-color: var(--ok);     color: var(--ok);   }
    .modal__identity-info { display: grid; gap: 3px; min-width: 0; }
    .modal__identity-top  { display: flex; align-items: center; gap: 8px; }
    .modal__name {
      font-size: 15px; font-weight: 600; color: var(--text-strong);
      margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    .modal__id { font-size: 11px; color: var(--text-faint); }

    .icon-btn {
      width: 28px; height: 28px; background: transparent;
      border: 1px solid var(--line-strong); border-radius: 2px;
      cursor: pointer; color: var(--text-faint); flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
      transition: border-color 140ms ease;
    }
    .icon-btn:hover { border-color: var(--aurora-border); color: var(--aurora); }
    .icon-btn svg { width: 12px; height: 12px; }

    /* ── modal KPI strip ──────────────────────────────────────────────── */
    .modal-kpis {
      display: grid; grid-template-columns: repeat(4, 1fr);
      border-bottom: 1px solid var(--line);
    }
    .modal-kpi {
      display: flex; flex-direction: column; align-items: center;
      padding: 16px 12px; gap: 4px; text-align: center;
    }
    .modal-kpi + .modal-kpi { border-left: 1px solid var(--line); }
    .modal-kpi__label {
      font-size: 9.5px; letter-spacing: 0.14em; text-transform: uppercase;
      color: var(--text-faint); font-weight: 500;
    }
    .modal-kpi__val {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 22px; font-weight: 400; color: var(--text-strong);
      letter-spacing: -0.01em; line-height: 1;
    }

    /* ── modal insight ────────────────────────────────────────────────── */
    .modal-insight {
      display: flex; align-items: center; gap: 8px;
      padding: 10px 24px; background: var(--info-bg);
      border-bottom: 1px solid var(--line);
      font-size: 12px; font-weight: 300; color: var(--info);
    }
    .modal-insight svg { width: 13px; height: 13px; flex-shrink: 0; }
    .modal-insight strong { font-weight: 600; }

    /* ── modal actions row ────────────────────────────────────────────── */
    .modal__foot {
      display: flex; align-items: center; gap: 8px;
      padding: 14px 24px; border-bottom: 1px solid var(--line); flex-wrap: wrap;
    }
    .modal__foot--top .btn-primary,
    .modal__foot--top .btn-outline { flex: 1; justify-content: center; }

    /* ── modal history section ────────────────────────────────────────── */
    .modal__section { padding: 16px 24px 24px; display: grid; gap: 10px; }

    .hist-list { display: grid; gap: 4px; }

    .hist-item {
      display: flex; justify-content: space-between; align-items: center;
      padding: 9px 11px; background: var(--surface);
      border: 1px solid var(--line); border-radius: 3px;
      gap: 8px; transition: border-color 120ms ease;
    }
    .hist-item:hover { border-color: var(--aurora-border); }

    .hist-item__left { display: flex; flex-direction: column; gap: 2px; }
    .hist-item__code { font-size: 12px; font-weight: 500; color: var(--text-strong); }
    .hist-item__seq  { font-size: 10.5px; color: var(--text-faint); }

    .hist-item__right {
      display: flex; flex-direction: column; align-items: flex-end; gap: 2px;
    }
    .hist-item__amt  { font-size: 12.5px; font-weight: 600; color: var(--text-strong); }
    .hist-item__date { font-size: 10.5px; color: var(--text-faint); }

    /* ── status chips ─────────────────────────────────────────────────── */
    .status-chip {
      display: inline-flex; align-items: center;
      padding: 1px 7px; border-radius: 2px;
      font-size: 9.5px; font-weight: 600;
      background: var(--surface-strong); color: var(--text-muted);
    }
    [data-status="AUTHORIZED"]         { background: var(--ok-bg);     color: var(--ok);     }
    [data-status="PENDING"]            { background: var(--warn-bg);   color: var(--warn);   }
    [data-status="PENDING_VALIDATION"] { background: var(--warn-bg);   color: var(--warn);   }
    [data-status="SENT"]               { background: var(--info-bg);   color: var(--info);   }
    [data-status="REJECTED"]           { background: var(--danger-bg); color: var(--danger); }
    [data-status="CANCELLED"],
    [data-status="VOID"],
    [data-status="ERROR"]              { background: var(--danger-bg); color: var(--danger); }
    [data-status="DRAFT"]              { background: var(--surface-strong); color: var(--text-muted); }

    /* ── show more ────────────────────────────────────────────────────── */
    .more-btn {
      width: 100%; padding: 7px; background: none;
      border: 1px dashed var(--line-strong); border-radius: 2px;
      font-size: 11px; color: var(--text-faint); cursor: pointer;
      text-align: center; transition: all 120ms ease;
    }
    .more-btn:hover { border-color: var(--aurora-border); color: var(--aurora); }
  `,
})
export class Customers {
  private readonly api        = inject(ErpApi);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly toast      = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  // ── Data signals ───────────────────────────────────────────────────────
  protected readonly loading      = signal(false);
  protected readonly errorMsg     = signal('');
  protected readonly customers    = signal<Customer[]>([]);
  protected readonly selected     = signal<Customer | null>(null);
  protected readonly panelDocLimit = signal(10);

  // ── Filter signals ─────────────────────────────────────────────────────
  protected readonly searchQuery  = signal('');
  protected readonly segment      = signal<SegmentFilter>('ALL');
  protected readonly sortCol      = signal<SortCol>('spent');
  protected readonly sortDir      = signal<SortDir>('desc');

  // ── Segment options ────────────────────────────────────────────────────
  protected readonly segmentOptions: Array<{ value: SegmentFilter; label: string; count: () => number }> = [
    { value: 'ALL',       label: 'Todos',     count: () => this.customers().length },
    { value: 'VIP',       label: 'VIP',       count: () => this.customers().filter(c => c.tier === 'VIP').length },
    { value: 'Frecuente', label: 'Frecuente', count: () => this.customers().filter(c => c.tier === 'Frecuente').length },
    { value: 'Nuevo',     label: 'Nuevo',     count: () => this.customers().filter(c => c.tier === 'Nuevo').length },
  ];

  // ── KPI computed ───────────────────────────────────────────────────────
  protected readonly totalCustomers  = computed(() => this.customers().length);
  protected readonly totalDocs       = computed(() => this.customers().reduce((s, c) => s + c.totalDocs, 0));
  protected readonly totalRevenue    = computed(() => this.customers().reduce((s, c) => s + c.totalSpent, 0));
  protected readonly avgTicketGlobal = computed(() => {
    const docs = this.totalDocs();
    return docs > 0 ? this.totalRevenue() / docs : 0;
  });
  protected readonly topCustomer     = computed(() => {
    const list = this.customers();
    return list.length ? list.reduce((top, c) => c.totalSpent > top.totalSpent ? c : top, list[0]) : null;
  });
  protected readonly topCustomerName  = computed(() => this.topCustomer()?.name ?? '—');
  protected readonly topCustomerSpent = computed(() => this.topCustomer()?.totalSpent ?? 0);
  protected readonly hasTopCustomer   = computed(() => {
    const top = this.topCustomer();
    return !!top && (top.totalSpent ?? 0) > 0;
  });

  // ── Filtered + Sorted ─────────────────────────────────────────────────
  protected readonly filtered = computed(() => {
    const q   = this.searchQuery().toLowerCase().trim();
    const seg = this.segment();
    let list  = this.customers();
    if (seg !== 'ALL') list = list.filter(c => c.tier === seg);
    if (q) list = list.filter(c =>
      c.name.toLowerCase().includes(q) || c.identification.toLowerCase().includes(q),
    );
    return list;
  });

  protected readonly page = signal(0);
  protected readonly pageSize = signal(25);

  protected readonly sorted = computed(() => {
    const list = [...this.filtered()];
    const col  = this.sortCol();
    const dir  = this.sortDir();
    const sign = dir === 'asc' ? 1 : -1;
    list.sort((a, b) => {
      if (col === 'name')  return sign * a.name.localeCompare(b.name);
      if (col === 'spent') return sign * (a.totalSpent - b.totalSpent);
      if (col === 'docs')  return sign * (a.totalDocs - b.totalDocs);
      if (col === 'avg')   return sign * (a.avgTicket - b.avgTicket);
      if (col === 'last')  return sign * a.lastPurchase.localeCompare(b.lastPurchase);
      return 0;
    });
    return list;
  });

  protected readonly paged = computed(() => {
    const start = this.page() * this.pageSize();
    return this.sorted().slice(start, start + this.pageSize());
  });

  // ── Footer totals ──────────────────────────────────────────────────────
  protected readonly footerTotal = computed(() => this.filtered().reduce((s, c) => s + c.totalSpent, 0));
  protected readonly footerDocs  = computed(() => this.filtered().reduce((s, c) => s + c.totalDocs, 0));

  // ── Panel docs (paginated) ─────────────────────────────────────────────
  protected readonly panelDocs = computed(() =>
    (this.selected()?.docs ?? [])
      .slice()
      .sort((a, b) => (b.issueDate ?? '').localeCompare(a.issueDate ?? ''))
      .slice(0, this.panelDocLimit()),
  );

  // ── Constructor ────────────────────────────────────────────────────────
  constructor() { this.load(); }

  // ── Escape ─────────────────────────────────────────────────────────────
  @HostListener('document:keydown.escape')
  onEsc(): void { if (this.selected()) this.closePanel(); }

  // ── Data loading ───────────────────────────────────────────────────────
  protected load(): void {
    this.loading.set(true);
    this.errorMsg.set('');
    this.api
      .getCustomerDocuments()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (docs) => {
          this.customers.set(this.buildCustomers(docs));
          this.loading.set(false);
        },
        error: (error) => {
          const feedback = this.httpFeedback.fromError(error, 'No se pudieron cargar los clientes. Intenta de nuevo.');
          this.errorMsg.set(feedback.message);
          this.loading.set(false);
        },
      });
  }

  // ── Panel ──────────────────────────────────────────────────────────────
  protected openPanel(c: Customer): void {
    this.selected.set(c);
    this.panelDocLimit.set(10);
  }

  protected closePanel(): void { this.selected.set(null); }

  // ── Sorting ────────────────────────────────────────────────────────────
  protected toggleSort(col: SortCol): void {
    if (this.sortCol() === col) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortCol.set(col);
      this.sortDir.set('desc');
    }
  }

  protected sortIcon(col: SortCol): string {
    if (this.sortCol() !== col) return '↕';
    return this.sortDir() === 'asc' ? '↑' : '↓';
  }

  // ── Filters ────────────────────────────────────────────────────────────
  protected clearFilters(): void {
    this.searchQuery.set('');
    this.segment.set('ALL');
  }

  // ── Actions ────────────────────────────────────────────────────────────
  protected registerSale(c: Customer, event?: Event): void {
    event?.stopPropagation();
    this.toast.info('Registrar venta', `Abre POS para vincular una venta a ${c.name}.`);
  }

  protected sendPromo(c: Customer, event?: Event): void {
    event?.stopPropagation();
    this.toast.success('Promoción programada', `Se preparará una comunicación para ${c.name}.`);
  }

  // ── CSV Export ─────────────────────────────────────────────────────────
  protected exportCsv(): void {
    const rows = [
      ['Cliente', 'Identificación', 'Segmento', 'Documentos', 'Total Facturado', 'Ticket Promedio', 'Última Compra'],
      ...this.sorted().map((c) => [
        `"${c.name}"`, c.identification, c.tier,
        c.totalDocs, c.totalSpent.toFixed(2), c.avgTicket.toFixed(2), c.lastPurchase,
      ]),
    ];
    const csv  = rows.map((r) => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href = url;
    a.download = `clientes_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    this.toast.success('CSV exportado', `${this.sorted().length} clientes exportados.`);
  }

  // ── Helpers ────────────────────────────────────────────────────────────
  protected initial(name: string): string {
    return (name || '?').charAt(0).toUpperCase();
  }

  protected statusLabel(status: string): string {
    const map: Record<string, string> = {
      AUTHORIZED:          'Autorizado',
      PENDING:             'Pendiente',
      PENDING_VALIDATION:  'Pend. validación',
      DRAFT:               'Borrador',
      REJECTED:            'Rechazado',
      CANCELLED:           'Anulado',
      VOID:                'Anulado',
      SENT:                'Enviado',
      ERROR:               'Error',
    };
    return map[status] ?? status;
  }

  protected daysSinceLast(lastPurchase: string): number {
    if (!lastPurchase) return 0;
    return Math.floor((Date.now() - new Date(lastPurchase).getTime()) / 86_400_000);
  }

  protected avgDaysBetween(docs: SriDocumentDto[]): number {
    const sorted = [...docs]
      .filter((d) => d.issueDate)
      .sort((a, b) => a.issueDate!.localeCompare(b.issueDate!));
    if (sorted.length < 2) return 0;
    const span = new Date(sorted.at(-1)!.issueDate!).getTime() - new Date(sorted[0].issueDate!).getTime();
    return Math.round(span / 86_400_000 / (sorted.length - 1));
  }

  // ── Data builder ───────────────────────────────────────────────────────
  private buildCustomers(docs: SriDocumentDto[]): Customer[] {
    const map = new Map<string, Customer>();
    for (const doc of docs) {
      const id   = doc.buyerIdentification ?? 'CONSUMIDOR_FINAL';
      const name = doc.buyerName ?? 'Consumidor Final';
      if (!map.has(id)) {
        map.set(id, { id, name, identification: id, totalSpent: 0, totalDocs: 0, lastPurchase: '', avgTicket: 0, tier: 'Nuevo', docs: [] });
      }
      const entry = map.get(id)!;
      entry.totalSpent += doc.total ?? 0;
      entry.totalDocs  += 1;
      entry.docs.push(doc);
      if (!entry.lastPurchase || (doc.issueDate ?? '') > entry.lastPurchase) {
        entry.lastPurchase = doc.issueDate ?? '';
      }
    }
    const list = Array.from(map.values()).map((c) => ({ ...c, avgTicket: c.totalDocs > 0 ? c.totalSpent / c.totalDocs : 0 }));
    return this.assignTiers(list);
  }

  private assignTiers(customers: Customer[]): Customer[] {
    if (!customers.length) return customers;
    const sorted   = [...customers].sort((a, b) => b.totalSpent - a.totalSpent);
    const vipCount = Math.max(1, Math.ceil(sorted.length * 0.2));
    const vipIds   = new Set(sorted.slice(0, vipCount).map((c) => c.id));
    return customers.map((c) => ({
      ...c,
      tier: vipIds.has(c.id) ? 'VIP' : c.totalDocs >= 3 ? 'Frecuente' : 'Nuevo',
    }));
  }
}
