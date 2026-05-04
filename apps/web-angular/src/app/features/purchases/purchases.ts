import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, HostListener, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UiFeedback } from '../../core/models/ui-feedback';
import { HttpFeedback } from '../../core/services/http-feedback';
import { FileDownloadService } from '../../core/services/file-download';
import { ProductDto, PurchaseDto, PayableDto, ErpApi, ExportFormat } from '../../core/services/erp-api';
import { RequestFeedback } from '../shared/components/request-feedback';
import { ExportActions } from '../shared/components/export-actions';
import { Paginator } from '../shared/components/paginator';

type DraftLine   = { productId: number; quantity: number; unitCost: number };
type StatusFilter = 'ALL' | 'DRAFT' | 'RECEIVED' | 'PAID';
type ActiveModal  = 'detail' | 'pay' | null;
type SortCol      = 'supplier' | 'status' | 'total' | 'due' | 'created' | null;
type SortDir      = 'asc' | 'desc';

/* known supplier record extracted from purchase history */
type SupplierRecord = { name: string; ruc: string; email: string; lastTotal: number; count: number };

@Component({
  selector: 'app-purchases',
  imports: [FormsModule, CurrencyPipe, DatePipe, RequestFeedback, ExportActions, Paginator],
  template: `
    <!-- ══════════════════════ MODAL: DETALLE COMPRA ══════════════════════ -->
    @if (activeModal() === 'detail' && detailPurchase(); as p) {
      <div class="modal-bd" (click)="closeModal()">
        <div class="modal-box modal-box--xl" (click)="$event.stopPropagation()">
          <header class="modal__hd">
            <div>
              <span class="modal__eyebrow">Compra #{{ p.id }}</span>
              <h3>{{ p.supplierName }}</h3>
              @if (p.supplierIdentification) {
                <span class="modal__code">RUC/ID {{ p.supplierIdentification }}</span>
              }
            </div>
            <span class="status-chip" [class]="statusClass(p.status)">{{ statusLabel(p.status) }}</span>
          </header>

          <!-- timeline -->
          <div class="timeline">
            <div class="tl-step tl-step--done">
              <span class="tl-dot tl-dot--done"></span>
              <div><strong>Creada</strong><small>{{ p.createdAt | date:'dd/MM/yyyy HH:mm' }}</small></div>
            </div>
            <div class="tl-step" [class.tl-step--done]="p.status === 'RECEIVED' || p.status === 'PAID'">
              <span class="tl-dot" [class.tl-dot--done]="p.status === 'RECEIVED' || p.status === 'PAID'"></span>
              <div><strong>Recibida</strong><small>{{ p.receivedAt ? (p.receivedAt | date:'dd/MM/yyyy HH:mm') : '—' }}</small></div>
            </div>
            <div class="tl-step" [class.tl-step--done]="p.status === 'PAID'">
              <span class="tl-dot" [class.tl-dot--done]="p.status === 'PAID'"></span>
              <div><strong>Pagada</strong><small>{{ p.paidAt ? (p.paidAt | date:'dd/MM/yyyy HH:mm') : '—' }}</small></div>
            </div>
          </div>

          <!-- meta -->
          <div class="detail-meta">
            @if (p.externalDocumentNumber) {
              <div class="dmeta__item"><span>N° Documento</span><strong>{{ p.externalDocumentNumber }}</strong></div>
            }
            @if (p.dueDate) {
              <div class="dmeta__item">
                <span>Vencimiento</span>
                <strong [class.col--danger]="isOverdue(p)">{{ p.dueDate | date:'dd/MM/yyyy' }}</strong>
              </div>
            }
            @if (p.supplierEmail) {
              <div class="dmeta__item"><span>Correo</span><strong>{{ p.supplierEmail }}</strong></div>
            }
            @if (p.notes) {
              <div class="dmeta__item"><span>Notas</span><strong>{{ p.notes }}</strong></div>
            }
          </div>

          <!-- items -->
          <div class="detail-items">
            <div class="ditems__head">
              <span>Producto</span>
              <span class="num">Cant.</span>
              <span class="num">Costo unit.</span>
              <span class="num">Subtotal</span>
              <span class="num">IVA</span>
              <span class="num">Total línea</span>
            </div>
            @for (item of p.items; track item.id) {
              <div class="ditems__row">
                <span>{{ item.productName }}</span>
                <span class="num">{{ item.quantity }}</span>
                <span class="num">{{ item.unitCost | currency:'USD':'symbol':'1.2-2' }}</span>
                <span class="num">{{ item.lineSubtotal | currency:'USD':'symbol':'1.2-2' }}</span>
                <span class="num">{{ item.taxAmount | currency:'USD':'symbol':'1.2-2' }}</span>
                <span class="num">{{ item.lineTotal | currency:'USD':'symbol':'1.2-2' }}</span>
              </div>
            }
            <div class="ditems__totals">
              <span>Subtotal</span><span class="num">{{ p.subtotal | currency:'USD':'symbol':'1.2-2' }}</span>
              <span>IVA 15%</span><span class="num">{{ p.tax | currency:'USD':'symbol':'1.2-2' }}</span>
              <span class="tot-label">Total</span><span class="num tot-val">{{ p.total | currency:'USD':'symbol':'1.2-2' }}</span>
            </div>
          </div>

          <!-- supplier history strip inside detail -->
          @if (supplierPurchaseHistory(p.supplierName, p.id).length > 0) {
            <div class="hist-strip">
              <span class="hist-strip__label">Compras anteriores de {{ p.supplierName }}</span>
              <div class="hist-strip__items">
                @for (h of supplierPurchaseHistory(p.supplierName, p.id); track h.id) {
                  <div class="hist-chip" (click)="openDetail(h)">
                    <span>#{{ h.id }}</span>
                    <span class="status-chip" [class]="statusClass(h.status)">{{ statusLabel(h.status) }}</span>
                    <strong>{{ h.total | currency:'USD':'symbol':'1.2-2' }}</strong>
                    <small>{{ h.createdAt | date:'dd/MM/yy' }}</small>
                  </div>
                }
              </div>
            </div>
          }

          <div class="modal__foot">
            @if (p.status === 'DRAFT') {
              <button class="btn-ok" [disabled]="submitting()" (click)="receiveFromDetail(p.id)">
                {{ submitting() ? 'Procesando...' : 'Recibir mercadería' }}
              </button>
            }
            @if (p.status === 'RECEIVED') {
              <button class="btn-primary" [disabled]="submitting()" (click)="openPayModal(p)">
                Registrar pago
              </button>
            }
            <button class="ghost" (click)="closeModal()">Cerrar</button>
          </div>
        </div>
      </div>
    }

    <!-- ══════════════════════ MODAL: REGISTRAR PAGO ══════════════════════ -->
    @if (activeModal() === 'pay' && payTarget(); as p) {
      <div class="modal-bd" (click)="closeModal()">
        <div class="modal-box" (click)="$event.stopPropagation()">
          <header class="modal__hd">
            <div>
              <span class="modal__eyebrow">Compra #{{ p.id }}</span>
              <h3>Registrar pago a {{ p.supplierName }}</h3>
            </div>
          </header>

          <div class="pay-summary">
            <div class="pay-summary__row"><span>Subtotal</span><strong>{{ p.subtotal | currency:'USD':'symbol':'1.2-2' }}</strong></div>
            <div class="pay-summary__row"><span>IVA 15%</span><strong>{{ p.tax | currency:'USD':'symbol':'1.2-2' }}</strong></div>
            <div class="pay-summary__row pay-summary__row--total">
              <span>Total a pagar</span><strong>{{ p.total | currency:'USD':'symbol':'1.2-2' }}</strong>
            </div>
          </div>

          <p class="mlabel">Cuenta de pago</p>
          <div class="pay-accounts">
            <button class="pay-acc" [class.pay-acc--on]="payAccount === 'BANK'" (click)="payAccount = 'BANK'">
              <span class="pay-acc__icon">🏦</span>Banco
            </button>
            <button class="pay-acc" [class.pay-acc--on]="payAccount === 'CASH'" (click)="payAccount = 'CASH'">
              <span class="pay-acc__icon">💵</span>Efectivo
            </button>
            <button class="pay-acc" [class.pay-acc--on]="payAccount === 'TRANSFER'" (click)="payAccount = 'TRANSFER'">
              <span class="pay-acc__icon">📲</span>Transferencia
            </button>
          </div>

          <p class="mlabel">Referencia de pago <span class="opt">(opcional)</span></p>
          <input class="field" [(ngModel)]="payReference" placeholder="N° cheque, transferencia, comprobante..." />

          <p class="mlabel">Monto pagado</p>
          <input type="number" class="field" [(ngModel)]="payAmount"
            [placeholder]="p.total.toString()" min="0" step="0.01" />

          @if (payAmount > 0 && payAmount < p.total) {
            <p class="pay-partial-warn">
              Pago parcial — quedarán {{ (p.total - payAmount) | currency:'USD':'symbol':'1.2-2' }} pendientes
            </p>
          }

          <div class="modal__foot">
            <button class="ghost" (click)="closeModal()">Cancelar</button>
            <button class="btn-primary" [disabled]="submitting() || payAmount <= 0" (click)="submitPay()">
              {{ submitting() ? 'Procesando...' : 'Confirmar pago' }}
            </button>
          </div>
        </div>
      </div>
    }

    <!-- ═══════════════════════════ PÁGINA PRINCIPAL ═══════════════════════════ -->
    <section class="page">

      <!-- ── HEADER ── -->
      <header class="page__header">
        <div>
          <span class="eyebrow">Operación diaria</span>
          <h1>Compras</h1>
          <p>Órdenes a proveedores, recepción de mercadería y pagos.</p>
        </div>
        <div class="page__acts">
          <small>{{ filtered().length }} compras visibles</small>
          <button class="btn-outline" [disabled]="loading()" (click)="reload()">
            {{ loading() ? 'Cargando...' : 'Recargar' }}
          </button>
          <app-export-actions
            [disabled]="loading()"
            [exporting]="exporting()"
            (exportRequested)="exportPurchases($event)"
          />
        </div>
      </header>

      @if (feedback(); as n) {
        <app-request-feedback [tone]="n.tone" [message]="n.message" [traceId]="n.traceId" />
      }

      <!-- ── KPI STRIP ── -->
      <div class="kpi-row">
        <article class="kpi" (click)="setFilter('DRAFT')" [class.kpi--active]="statusFilter() === 'DRAFT'">
          <span class="kpi__label">Borradores</span>
          <strong class="kpi__val">{{ statusCount('DRAFT') }}</strong>
          <small class="kpi__sub">pendientes de recepción</small>
        </article>
        <article class="kpi" (click)="setFilter('RECEIVED')" [class.kpi--active]="statusFilter() === 'RECEIVED'">
          <span class="kpi__label">Recibidas</span>
          <strong class="kpi__val">{{ statusCount('RECEIVED') }}</strong>
          <small class="kpi__sub">listas para pago</small>
        </article>
        <article class="kpi" (click)="setFilter('PAID')" [class.kpi--active]="statusFilter() === 'PAID'">
          <span class="kpi__label">Pagadas</span>
          <strong class="kpi__val">{{ statusCount('PAID') }}</strong>
          <small class="kpi__sub">cerradas correctamente</small>
        </article>
        <article class="kpi">
          <span class="kpi__label">Total en compras</span>
          <strong class="kpi__val kpi__val--money">{{ totalVisible() | currency:'USD':'symbol':'1.2-2' }}</strong>
          <small class="kpi__sub">monto visible en esta vista</small>
        </article>
        @if (overdueCount() > 0) {
          <article class="kpi kpi--warn" (click)="setFilter('RECEIVED')">
            <span class="kpi__label">Vencidas</span>
            <strong class="kpi__val">{{ overdueCount() }}</strong>
            <small class="kpi__sub">requieren pago urgente</small>
          </article>
        }
      </div>

      <!-- ── MAIN LAYOUT ── -->
      <div class="main-layout">

        <!-- LEFT: form nueva compra -->
        <section class="card">
          <header class="card__hd">
            <div>
              <span class="eyebrow">Nueva compra</span>
              <h2>Documento del proveedor</h2>
            </div>
            @if (draftTotal() > 0) {
              <span class="draft-total">
                Total estimado: <strong>{{ draftTotal() | currency:'USD':'symbol':'1.2-2' }}</strong>
              </span>
            }
          </header>

          <!-- ── proveedor fields ── -->
          <div class="form-grid">
            <!-- supplier name with autocomplete -->
            <div class="field-wrap">
              <input class="field" [(ngModel)]="supplierName"
                (ngModelChange)="onSupplierChange($event)"
                placeholder="Proveedor *" list="sugg-suppliers" autocomplete="off" />
              <datalist id="sugg-suppliers">
                @for (s of supplierSuggestions(); track s.name) {
                  <option [value]="s.name"></option>
                }
              </datalist>
              @if (knownSupplier()) {
                <span class="field-hint field-hint--ok">
                  Proveedor conocido · {{ knownSupplier()!.count }} compra{{ knownSupplier()!.count !== 1 ? 's' : '' }} ·
                  últ. {{ knownSupplier()!.lastTotal | currency:'USD':'symbol':'1.2-2' }}
                </span>
              }
            </div>

            <!-- RUC -->
            <input class="field" [(ngModel)]="supplierRuc" placeholder="RUC o identificación" />

            <!-- email -->
            <input class="field" [(ngModel)]="supplierEmail" placeholder="Correo del proveedor" />

            <!-- doc number with duplicate warning -->
            <div class="field-wrap">
              <input class="field" [(ngModel)]="docNumber"
                [class.field--warn]="docDuplicate()"
                placeholder="Número de documento" />
              @if (docDuplicate()) {
                <span class="field-hint field-hint--warn">
                  ⚠ Ya existe una compra con este documento ({{ docDuplicate() }})
                </span>
              }
            </div>

            <!-- due date -->
            <div class="field-wrap">
              <input class="field" type="date" [(ngModel)]="dueDate" title="Fecha de vencimiento" />
              @if (dueDate && isDueDateSoon()) {
                <span class="field-hint field-hint--warn">Vence en menos de 7 días</span>
              }
            </div>

            <!-- notes -->
            <input class="field" [(ngModel)]="notes" placeholder="Notas de operación" />
          </div>

          <!-- ── lines ── -->
          <div class="lines-wrap">
            <div class="lines-head">
              <span>Producto</span>
              <span class="num">Cant.</span>
              <span class="num">Costo unit.</span>
              <span class="num">Total línea</span>
              <span></span>
            </div>
            @for (line of draftLines(); track $index; let idx = $index) {
              <div class="line-row">
                <select class="field" [ngModel]="line.productId"
                  (ngModelChange)="updateLine(idx, 'productId', $event)">
                  <option [ngValue]="0">— Seleccionar producto —</option>
                  @for (p of products(); track p.id) {
                    <option [ngValue]="p.id">
                      {{ p.name }}{{ p.code ? ' · ' + p.code : '' }}
                      {{ p.availableStock != null ? ' (' + p.availableStock + ' uds)' : '' }}
                    </option>
                  }
                </select>
                <input type="number" class="field field--num" min="1"
                  [ngModel]="line.quantity"
                  (ngModelChange)="updateLine(idx, 'quantity', $event)"
                  placeholder="1" />
                <div class="cost-wrap">
                  <input type="number" class="field field--num" min="0" step="0.01"
                    [ngModel]="line.unitCost"
                    (ngModelChange)="updateLine(idx, 'unitCost', $event)"
                    placeholder="0.00" />
                  @if (lastCostHint(line.productId); as hint) {
                    <span class="cost-hint"
                      [class.cost-hint--up]="line.unitCost > hint"
                      [class.cost-hint--down]="line.unitCost > 0 && line.unitCost < hint"
                      [class.cost-hint--eq]="line.unitCost === hint">
                      últ. {{ hint | currency:'USD':'symbol':'1.2-2' }}
                    </span>
                  }
                </div>
                <span class="line-total" [class.col--ok]="lineTotal(line) > 0">
                  {{ lineTotal(line) | currency:'USD':'symbol':'1.2-2' }}
                </span>
                <button class="icon-btn icon-btn--del" (click)="removeLine(idx)"
                  [disabled]="draftLines().length <= 1" title="Quitar">✕</button>
              </div>
            }
          </div>

          <!-- ── form footer ── -->
          <div class="form-foot">
            <button class="ghost" (click)="addLine()">+ Agregar línea</button>
            <div class="form-foot__right">
              @if (draftTotal() > 0) {
                <span class="subtotal-tag">
                  {{ draftLines().length }} {{ draftLines().length === 1 ? 'ítem' : 'ítems' }} ·
                  <strong>{{ draftTotal() | currency:'USD':'symbol':'1.2-2' }}</strong>
                </span>
              }
              <button class="ghost" (click)="resetForm()" title="Limpiar formulario">Limpiar</button>
              <button class="btn-primary"
                [disabled]="submitting() || !canSubmitPurchase()"
                [title]="purchaseHint()"
                [attr.aria-label]="purchaseHint() || 'Guardar compra'"
                (click)="submitPurchase()">
                {{ submitting() ? 'Guardando...' : 'Guardar compra' }}
              </button>
            </div>
            @if (!canSubmitPurchase() && !submitting()) {
              <p class="purchase-hint">{{ purchaseHint() }}</p>
            }
          </div>
        </section>

        <!-- RIGHT: aside panel -->
        <aside class="aside">
          <!-- supply status -->
          <section class="card card--aside">
            <header class="card__hd">
              <div><span class="eyebrow">Lectura rápida</span><h2>Estado del abastecimiento</h2></div>
            </header>
            <div class="aside-kpis">
              <div class="aside-kpi">
                <span>Productos activos</span>
                <strong>{{ products().length }}</strong>
              </div>
              <div class="aside-kpi">
                <span>Flujo activo</span>
                <strong>{{ statusCount('DRAFT') + statusCount('RECEIVED') }}</strong>
              </div>
              <div class="aside-kpi">
                <span>Proveedores únicos</span>
                <strong>{{ supplierSuggestions().length }}</strong>
              </div>
              <div class="aside-kpi">
                <span>Próximo paso</span>
                <strong>{{ nextStep() }}</strong>
              </div>
            </div>
          </section>

          <!-- low stock -->
          @if (lowStockProducts().length > 0) {
            <section class="card card--aside card--alert">
              <header class="card__hd">
                <span class="eyebrow eyebrow--warn">Requieren reorden</span>
                <h2>Bajo stock</h2>
              </header>
              <div class="low-list">
                @for (p of lowStockProducts(); track p.id) {
                  <div class="low-item">
                    <div class="low-item__info">
                      <span class="low-item__name">{{ p.name }}</span>
                      <span class="low-item__stock" [class.col--danger]="(p.availableStock ?? 0) === 0">
                        {{ p.availableStock ?? p.stock ?? 0 }} uds.
                        @if (p.minStock) { <em>/ mín {{ p.minStock }}</em> }
                      </span>
                    </div>
                    <button class="mini-btn" (click)="quickAddProduct(p)" title="Añadir a nueva compra">+</button>
                  </div>
                }
              </div>
            </section>
          }

          <!-- payables -->
          @if (openPayables().length > 0) {
            <section class="card card--aside">
              <header class="card__hd">
                <span class="eyebrow">Cuentas por pagar</span>
                <h2>Pendientes</h2>
              </header>
              <div class="payable-list">
                @for (pay of openPayables().slice(0, 5); track pay.id) {
                  <div class="payable-item" [class.payable-item--overdue]="isPayableOverdue(pay)">
                    <div>
                      <span class="payable-item__doc">{{ pay.sourceDocumentType }} #{{ pay.sourceDocumentId }}</span>
                      @if (pay.dueDate) {
                        <span class="payable-item__due">Vence {{ pay.dueDate | date:'dd/MM' }}</span>
                      }
                    </div>
                    <strong class="payable-item__bal">{{ pay.balance | currency:'USD':'symbol':'1.2-2' }}</strong>
                  </div>
                }
                @if (openPayables().length > 5) {
                  <span class="more-hint">+{{ openPayables().length - 5 }} más</span>
                }
              </div>
            </section>
          }
        </aside>
      </div>

      <!-- ── PURCHASES TABLE ── -->
      <section class="card">
        <header class="card__hd">
          <div><span class="eyebrow">Seguimiento</span><h2>Compras registradas</h2></div>
          <div class="tbl-controls">
            <input class="search-field" [(ngModel)]="searchQuery" placeholder="Buscar proveedor, doc..." />
            <input class="field--date" type="date" [(ngModel)]="dateFrom" title="Desde" />
            <input class="field--date" type="date" [(ngModel)]="dateTo"   title="Hasta" />
            @if (dateFrom || dateTo || searchQuery) {
              <button class="ghost ghost--sm" (click)="clearFilters()" title="Limpiar filtros">✕</button>
            }
            <div class="tabs">
              @for (tab of statusTabs; track tab.key) {
                <button class="tab" [class.tab--on]="statusFilter() === tab.key"
                  (click)="setFilter(tab.key)">
                  {{ tab.label }}
                  @if (tab.key !== 'ALL' && statusCount(tab.key) > 0) {
                    <em class="tab-badge">{{ statusCount(tab.key) }}</em>
                  }
                </button>
              }
            </div>
          </div>
        </header>

        @if (loading()) {
          <p class="state-msg">Consultando compras...</p>
        } @else if (!sorted().length) {
          <p class="state-msg">
            @if (purchases().length === 0) {
              Todavía no hay compras registradas para este negocio.
            } @else {
              Sin resultados para los filtros aplicados.
              <button class="link-btn" (click)="clearFilters()">Limpiar filtros</button>
            }
          </p>
        } @else {
          <div class="tbl">
            <!-- sortable header -->
            <div class="tbl__head">
              <span class="th-sort" (click)="toggleSort('supplier')">
                Proveedor {{ sortIcon('supplier') }}
              </span>
              <span>Doc. externo</span>
              <span class="th-sort" (click)="toggleSort('status')">
                Estado {{ sortIcon('status') }}
              </span>
              <span class="num">Subtotal</span>
              <span class="num">IVA</span>
              <span class="num th-sort" (click)="toggleSort('total')">
                Total {{ sortIcon('total') }}
              </span>
              <span class="th-sort" (click)="toggleSort('due')">
                Vencimiento {{ sortIcon('due') }}
              </span>
              <span class="th-sort" (click)="toggleSort('created')">
                Creada {{ sortIcon('created') }}
              </span>
              <span>Acciones</span>
            </div>

            @for (p of paged(); track p.id) {
              <div class="tbl__row" [class.tbl__row--overdue]="isOverdue(p)"
                (click)="openDetail(p)">
                <span class="cell-supplier">
                  {{ p.supplierName }}
                  @if (p.supplierIdentification) {
                    <em>{{ p.supplierIdentification }}</em>
                  }
                </span>
                <span>{{ p.externalDocumentNumber || '—' }}</span>
                <span>
                  <span class="status-chip" [class]="statusClass(p.status)">{{ statusLabel(p.status) }}</span>
                </span>
                <span class="num">{{ p.subtotal | currency:'USD':'symbol':'1.2-2' }}</span>
                <span class="num">{{ p.tax | currency:'USD':'symbol':'1.2-2' }}</span>
                <span class="num tbl__total">{{ p.total | currency:'USD':'symbol':'1.2-2' }}</span>
                <span [class.col--danger]="isOverdue(p)" [class.col--warn]="isDueWithinWeek(p)">
                  {{ p.dueDate ? (p.dueDate | date:'dd/MM/yyyy') : '—' }}
                </span>
                <span>{{ p.createdAt | date:'dd/MM/yy HH:mm' }}</span>
                <div class="tbl__acts" (click)="$event.stopPropagation()">
                  <button class="act-btn" (click)="openDetail(p)">Ver</button>
                  @if (p.status === 'DRAFT') {
                    <button class="act-btn act-btn--ok" [disabled]="submitting()"
                      (click)="receivePurchase(p.id)">Recibir</button>
                  }
                  @if (p.status === 'RECEIVED') {
                    <button class="act-btn act-btn--primary" [disabled]="submitting()"
                      (click)="openPayModal(p)">Pagar</button>
                  }
                </div>
              </div>
            }
          </div>

          <!-- table summary footer -->
          <div class="tbl-footer">
            <span>{{ sorted().length }} compra{{ sorted().length !== 1 ? 's' : '' }}</span>
            <span>Total visible: <strong>{{ totalVisible() | currency:'USD':'symbol':'1.2-2' }}</strong></span>
            @if (statusCount('RECEIVED') > 0) {
              <span class="footer-warn">
                Por pagar: <strong>{{ totalByStatus('RECEIVED') | currency:'USD':'symbol':'1.2-2' }}</strong>
              </span>
            }
            @if (statusFilter() !== 'ALL' || searchQuery || dateFrom || dateTo) {
              <button class="link-btn" (click)="clearFilters()">Ver todas</button>
            }
          </div>
          <app-paginator
            [page]="currentPage()"
            [pageSize]="pageSize"
            [totalElements]="sorted().length"
            (pageChange)="setPage($event)"
          />
        }
      </section>

    </section>
  `,
  styles: `
    :host { display: contents; }

    /* ── page ───────────────────────────────────────────────────── */
    .page { display: grid; gap: 24px; padding: 32px; align-content: start; }

    /* ── header ─────────────────────────────────────────────────── */
    .page__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; }
    .eyebrow {
      display: block;
      font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase;
      font-weight: 500; color: var(--aurora-dim); margin-bottom: 6px;
    }
    .eyebrow--warn { color: var(--warn); }
    .page__header h1 {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 36px; font-weight: 400; color: var(--text-strong);
      margin: 0 0 4px; letter-spacing: -0.02em; line-height: 1.1;
    }
    .page__header p { font-size: 13px; font-weight: 300; color: var(--text-muted); margin: 0; line-height: 1.6; }
    .page__acts { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }
    .page__acts small { font-size: 11px; color: var(--text-faint); }

    /* ── buttons ─────────────────────────────────────────────────── */
    .btn-outline {
      padding: 10px 20px; background: transparent;
      border: 1px solid var(--aurora-border); color: var(--aurora);
      font-size: 11px; font-weight: 500; letter-spacing: 0.12em;
      text-transform: uppercase; border-radius: 2px; cursor: pointer;
      transition: background 180ms ease;
    }
    .btn-outline:hover:not(:disabled) { background: rgba(74,124,94,.07); }
    .btn-outline:disabled { opacity: 0.35; cursor: not-allowed; }
    .btn-primary {
      padding: 10px 18px; background: var(--aurora); border: none;
      color: #fff; font-size: 11px; font-weight: 500;
      letter-spacing: 0.1em; text-transform: uppercase; border-radius: 2px;
      cursor: pointer; transition: opacity 160ms ease; white-space: nowrap;
    }
    .btn-primary:hover:not(:disabled) { opacity: 0.88; }
    .btn-primary:disabled { opacity: 0.35; cursor: not-allowed; }
    .btn-ok {
      padding: 10px 18px; background: var(--ok); border: none;
      color: #fff; font-size: 11px; font-weight: 500;
      letter-spacing: 0.1em; text-transform: uppercase; border-radius: 2px;
      cursor: pointer; transition: opacity 160ms ease;
    }
    .btn-ok:hover:not(:disabled) { opacity: 0.88; }
    .btn-ok:disabled { opacity: 0.35; cursor: not-allowed; }
    .ghost {
      padding: 8px 14px; background: transparent;
      border: 1px solid var(--line-strong); color: var(--text-muted);
      font-size: 11px; border-radius: 2px; cursor: pointer;
      transition: border-color 140ms ease, color 140ms ease; white-space: nowrap;
    }
    .ghost:hover:not(:disabled) { border-color: var(--aurora-border); color: var(--aurora); }
    .ghost:disabled { opacity: 0.35; cursor: not-allowed; }
    .ghost--sm { padding: 6px 10px; font-size: 11px; }
    .link-btn {
      background: none; border: none; color: var(--aurora);
      font-size: 12px; cursor: pointer; text-decoration: underline; padding: 0;
    }

    /* ── KPI strip ───────────────────────────────────────────────── */
    .kpi-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; }
    .kpi {
      padding: 18px 20px; background: var(--bg-panel);
      border: 1px solid var(--line); border-radius: 4px;
      cursor: pointer; transition: border-color 160ms ease, background 160ms ease;
    }
    .kpi:hover { border-color: var(--aurora-border); }
    .kpi--active { border-color: var(--aurora-border); background: var(--aurora-ghost); }
    .kpi--warn { border-color: var(--warn-border); background: var(--warn-bg); }
    .kpi__label { display: block; font-size: 10px; letter-spacing: 0.18em; text-transform: uppercase; color: var(--text-faint); margin-bottom: 8px; }
    .kpi__val { display: block; font-family: 'Cormorant Garamond', Georgia, serif; font-size: 30px; font-weight: 400; color: var(--text-strong); letter-spacing: -0.02em; margin-bottom: 3px; }
    .kpi__val--money { font-size: 22px; }
    .kpi__sub { font-size: 11px; font-weight: 300; color: var(--text-faint); }

    /* ── main layout ─────────────────────────────────────────────── */
    .main-layout { display: grid; grid-template-columns: minmax(0, 1.4fr) 280px; gap: 12px; align-items: start; }

    /* ── card ────────────────────────────────────────────────────── */
    .card { padding: 20px; background: var(--bg-panel); border: 1px solid var(--line); border-radius: 4px; display: grid; gap: 16px; }
    .card--aside { gap: 12px; }
    .card--alert { border-color: var(--warn-border); }
    .aside { display: grid; gap: 12px; align-content: start; }
    .card__hd { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
    .card h2 { font-size: 13px; font-weight: 500; color: var(--text); margin: 0; }

    /* ── form ────────────────────────────────────────────────────── */
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 24px; }
    .field-wrap { position: relative; }
    .field {
      width: 100%; background: transparent; border: none;
      border-bottom: 1px solid var(--line-strong); color: var(--text);
      padding: 10px 0; font-size: 13px; font-weight: 300;
      outline: none; transition: border-color 180ms ease;
    }
    .field:focus { border-bottom-color: var(--aurora-border); }
    .field--warn { border-bottom-color: var(--warn) !important; }
    .field option { background: var(--bg-panel); color: var(--text); }
    .field--num { text-align: right; }
    .form-grid .field { margin-bottom: 2px; }
    .draft-total { font-size: 12px; color: var(--text-muted); }
    .draft-total strong { color: var(--aurora); font-weight: 500; }

    /* field hints */
    .field-hint {
      display: block; font-size: 10.5px; padding: 2px 0;
    }
    .field-hint--ok { color: var(--ok); }
    .field-hint--warn { color: var(--warn); }

    /* ── lines ───────────────────────────────────────────────────── */
    .lines-wrap { display: grid; gap: 4px; }
    .lines-head {
      display: grid; grid-template-columns: 1fr 80px 110px 90px 32px;
      gap: 8px; padding: 0 0 6px;
      font-size: 10px; text-transform: uppercase; letter-spacing: 0.14em;
      color: var(--text-faint); border-bottom: 1px solid var(--line);
    }
    .line-row {
      display: grid; grid-template-columns: 1fr 80px 110px 90px 32px;
      gap: 8px; align-items: start; padding: 6px 0;
      border-bottom: 1px solid var(--line);
    }
    .line-row:last-child { border-bottom: none; }
    .cost-wrap { display: grid; gap: 2px; }
    .cost-hint { font-size: 10px; text-align: right; color: var(--text-faint); }
    .cost-hint--up   { color: var(--danger); }
    .cost-hint--down { color: var(--ok); }
    .cost-hint--eq   { color: var(--text-faint); }
    .line-total { font-size: 12.5px; font-weight: 500; color: var(--text); text-align: right; padding-top: 10px; }
    .icon-btn {
      width: 28px; height: 28px; background: transparent;
      border: 1px solid var(--line-strong); border-radius: 2px;
      cursor: pointer; font-size: 10px; color: var(--text-faint);
      display: flex; align-items: center; justify-content: center;
      transition: border-color 140ms ease; margin-top: 8px;
    }
    .icon-btn--del { border-color: var(--danger-border); color: var(--danger); }
    .icon-btn--del:hover:not(:disabled) { background: var(--danger-bg); }
    .icon-btn:disabled { opacity: 0.3; cursor: not-allowed; }

    /* form footer */
    .form-foot { display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap; }
    .form-foot__right { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .subtotal-tag { font-size: 12px; color: var(--text-muted); }
    .subtotal-tag strong { color: var(--text); }

    /* ── aside ───────────────────────────────────────────────────── */
    .aside-kpis { display: grid; gap: 6px; }
    .aside-kpi {
      display: flex; align-items: center; justify-content: space-between;
      padding: 10px 12px; background: var(--surface); border: 1px solid var(--line); border-radius: 3px;
    }
    .aside-kpi span { font-size: 10px; text-transform: uppercase; letter-spacing: 0.14em; color: var(--text-faint); }
    .aside-kpi strong { font-family: 'Cormorant Garamond', Georgia, serif; font-size: 20px; font-weight: 400; color: var(--text-strong); }

    /* low stock */
    .low-list { display: grid; gap: 5px; }
    .low-item { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 8px 10px; background: var(--warn-bg); border: 1px solid var(--warn-border); border-radius: 3px; }
    .low-item__info { display: grid; gap: 2px; }
    .low-item__name { font-size: 12px; font-weight: 500; color: var(--text); }
    .low-item__stock { font-size: 11px; color: var(--text-muted); }
    .low-item__stock em { color: var(--text-faint); font-style: normal; }
    .mini-btn { width: 26px; height: 26px; background: var(--bg-panel); border: 1px solid var(--aurora-border); color: var(--aurora); border-radius: 2px; cursor: pointer; font-size: 14px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: background 140ms ease; }
    .mini-btn:hover { background: var(--aurora-ghost); }

    /* payables */
    .payable-list { display: grid; gap: 4px; }
    .payable-item { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 8px 10px; background: var(--surface); border: 1px solid var(--line); border-radius: 3px; font-size: 12px; }
    .payable-item--overdue { border-color: var(--danger-border); background: var(--danger-bg); }
    .payable-item__doc { display: block; color: var(--text); font-weight: 500; }
    .payable-item__due { display: block; font-size: 10.5px; color: var(--text-faint); }
    .payable-item__bal { color: var(--text-strong); font-weight: 600; white-space: nowrap; }
    .more-hint { font-size: 11px; color: var(--text-faint); text-align: center; padding: 4px 0 0; }

    /* ── table controls ──────────────────────────────────────────── */
    .tbl-controls {
      display: flex;
      align-items: center;
      gap: 14px;
      flex-wrap: wrap;
      padding-top: 8px;
    }
    .tbl-controls .tabs { padding-left: 12px; border-left: 1px solid var(--line); margin-left: 4px; }
    .tbl-controls .field--date { margin-left: 4px; }
    .purchase-hint {
      margin: 6px 0 0;
      padding: 8px 12px;
      font-size: 12px;
      background: var(--warn-bg, #fff4e0);
      border: 1px solid var(--warn-border, #f4d4a0);
      color: var(--warn, #b56500);
      border-radius: 6px;
    }
    .aside-kpi span { color: var(--text-muted) !important; font-weight: 600; }
    .aside-kpi strong { color: var(--text-strong) !important; }
    .search-field {
      background: transparent; border: 1px solid var(--line-strong); border-radius: 2px;
      color: var(--text); padding: 7px 10px; font-size: 12px; font-weight: 300;
      outline: none; width: 200px; transition: border-color 160ms ease;
    }
    .search-field:focus { border-color: var(--aurora-border); }
    .search-field::placeholder { color: var(--text-faint); }
    .field--date {
      background: transparent; border: 1px solid var(--line-strong); border-radius: 2px;
      color: var(--text); padding: 7px 8px; font-size: 12px; outline: none;
      transition: border-color 160ms ease;
    }
    .field--date:focus { border-color: var(--aurora-border); }
    .tabs { display: flex; gap: 2px; }
    .tab { padding: 6px 12px; background: transparent; border: 1px solid var(--line-strong); border-radius: 2px; color: var(--text-muted); font-size: 11px; cursor: pointer; transition: all 140ms ease; display: flex; align-items: center; gap: 5px; }
    .tab:hover { border-color: var(--aurora-border); color: var(--aurora); }
    .tab--on { border-color: var(--aurora-border); background: var(--aurora-ghost); color: var(--aurora); }
    .tab-badge { display: inline-flex; align-items: center; justify-content: center; background: var(--aurora); color: #fff; font-size: 9px; font-style: normal; font-weight: 600; min-width: 16px; height: 16px; padding: 0 4px; border-radius: 8px; }

    /* ── table ───────────────────────────────────────────────────── */
    .tbl { display: grid; gap: 3px; }
    .tbl__head, .tbl__row {
      display: grid;
      grid-template-columns: 1.4fr 0.9fr 0.75fr 0.65fr 0.55fr 0.8fr 0.7fr 0.85fr 0.85fr;
      gap: 8px; align-items: center; padding: 10px 12px;
    }
    .tbl__head { font-size: 10px; text-transform: uppercase; letter-spacing: 0.16em; font-weight: 500; color: var(--text-faint); border-bottom: 1px solid var(--line); }
    .th-sort { cursor: pointer; user-select: none; transition: color 120ms ease; }
    .th-sort:hover { color: var(--aurora); }
    .tbl__row { background: var(--surface); border-radius: 3px; font-size: 12.5px; color: var(--text-muted); cursor: pointer; transition: background 120ms ease, border-color 120ms ease; border: 1px solid transparent; }
    .tbl__row:hover { background: var(--surface-strong); border-color: var(--line); }
    .tbl__row--overdue { border-color: var(--danger-border); }
    .tbl__total { font-weight: 600; color: var(--text-strong); }
    .cell-supplier { display: grid; gap: 1px; }
    .cell-supplier em { font-size: 10.5px; color: var(--text-faint); font-style: normal; }
    .tbl__acts { display: flex; gap: 5px; }
    .num { text-align: right; }
    .act-btn { padding: 5px 10px; background: transparent; border: 1px solid var(--line-strong); border-radius: 2px; color: var(--text-muted); font-size: 10.5px; cursor: pointer; transition: all 120ms ease; white-space: nowrap; }
    .act-btn:hover { border-color: var(--aurora-border); color: var(--aurora); }
    .act-btn--ok { border-color: var(--ok-border); color: var(--ok); }
    .act-btn--ok:hover { background: var(--ok-bg); }
    .act-btn--primary { border-color: var(--aurora-border); color: var(--aurora); }
    .act-btn--primary:hover { background: var(--aurora-ghost); }
    .act-btn:disabled { opacity: 0.35; cursor: not-allowed; }

    /* table footer */
    .tbl-footer { display: flex; align-items: center; gap: 16px; padding: 10px 12px 0; font-size: 12px; color: var(--text-faint); border-top: 1px solid var(--line); flex-wrap: wrap; }
    .tbl-footer strong { color: var(--text); }
    .footer-warn { color: var(--warn); }
    .footer-warn strong { color: var(--warn); }

    /* state */
    .state-msg { margin: 0; padding: 14px 16px; border-radius: 3px; border: 1px solid var(--line); background: var(--surface); font-size: 13px; font-weight: 300; color: var(--text-muted); display: flex; align-items: center; gap: 12px; }

    /* ── status chips ────────────────────────────────────────────── */
    .status-chip { display: inline-block; padding: 3px 9px; border-radius: 20px; font-size: 10.5px; font-weight: 500; letter-spacing: 0.06em; text-transform: uppercase; }
    .chip--draft     { background: var(--surface-strong); color: var(--text-muted); border: 1px solid var(--line-strong); }
    .chip--received  { background: var(--info-bg); color: var(--info); border: 1px solid var(--info-border); }
    .chip--paid      { background: var(--ok-bg); color: var(--ok); border: 1px solid var(--ok-border); }
    .chip--pending   { background: var(--warn-bg, #fef6e4); color: var(--warn, #a36c00); border: 1px solid var(--warn-border, #f0d69b); }
    .chip--cancelled { background: var(--danger-bg, #fde7e7); color: var(--danger, #a12626); border: 1px solid var(--danger-border, #f4b7b7); }

    /* colors */
    .col--ok    { color: var(--ok); }
    .col--warn  { color: var(--warn); }
    .col--danger { color: var(--danger); }

    /* ═══════════════════════════ MODALS ═══════════════════════════ */
    .modal-bd { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 20px; backdrop-filter: blur(2px); }
    .modal-box { background: var(--bg-panel); border: 1px solid var(--line-strong); border-radius: 6px; width: 100%; max-width: 480px; max-height: 90vh; overflow-y: auto; display: grid; gap: 16px; padding: 24px; }
    .modal-box--xl { max-width: 800px; }
    .modal__hd { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
    .modal__hd > div { display: grid; gap: 4px; }
    .modal__eyebrow { font-size: 10px; letter-spacing: 0.2em; text-transform: uppercase; color: var(--aurora-dim); font-weight: 500; }
    .modal__hd h3 { font-family: 'Cormorant Garamond', Georgia, serif; font-size: 24px; font-weight: 400; color: var(--text-strong); margin: 0; letter-spacing: -0.01em; }
    .modal__code { font-size: 11px; color: var(--text-faint); }
    .modal__foot { display: flex; gap: 8px; justify-content: flex-end; padding-top: 8px; border-top: 1px solid var(--line); }

    /* timeline */
    .timeline { display: flex; align-items: flex-start; padding: 12px 0; border-top: 1px solid var(--line); border-bottom: 1px solid var(--line); }
    .tl-step { flex: 1; display: flex; align-items: flex-start; gap: 8px; opacity: 0.4; position: relative; }
    .tl-step--done { opacity: 1; }
    .tl-step + .tl-step::before { content: ''; position: absolute; left: -50%; top: 6px; width: 50%; height: 1px; background: var(--line-strong); }
    .tl-step--done + .tl-step--done::before { background: var(--ok); }
    .tl-dot { width: 14px; height: 14px; border-radius: 50%; border: 2px solid var(--line-strong); background: var(--bg-panel); flex-shrink: 0; margin-top: 1px; }
    .tl-dot--done { border-color: var(--ok); background: var(--ok); }
    .tl-step strong { display: block; font-size: 11.5px; font-weight: 500; color: var(--text); }
    .tl-step small  { display: block; font-size: 10.5px; color: var(--text-faint); }

    /* detail meta */
    .detail-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
    .dmeta__item { padding: 8px 10px; background: var(--surface); border-radius: 3px; }
    .dmeta__item span { display: block; font-size: 10px; text-transform: uppercase; letter-spacing: 0.12em; color: var(--text-faint); margin-bottom: 4px; }
    .dmeta__item strong { font-size: 13px; color: var(--text); font-weight: 400; }

    /* detail items */
    .detail-items { display: grid; gap: 2px; }
    .ditems__head { display: grid; grid-template-columns: 1.5fr 0.5fr 0.7fr 0.7fr 0.7fr 0.8fr; gap: 8px; padding: 6px 10px; font-size: 10px; text-transform: uppercase; letter-spacing: 0.13em; color: var(--text-faint); border-bottom: 1px solid var(--line); }
    .ditems__row  { display: grid; grid-template-columns: 1.5fr 0.5fr 0.7fr 0.7fr 0.7fr 0.8fr; gap: 8px; padding: 8px 10px; font-size: 12.5px; color: var(--text-muted); background: var(--surface); border-radius: 2px; }
    .ditems__totals { display: grid; grid-template-columns: 1fr auto; gap: 4px 16px; padding: 10px 10px 0; font-size: 12.5px; color: var(--text-muted); border-top: 1px solid var(--line); justify-items: end; }
    .ditems__totals span:nth-child(odd) { justify-self: start; }
    .tot-label { font-weight: 600; color: var(--text-strong); }
    .tot-val   { font-weight: 700; color: var(--text-strong); font-size: 14px; }

    /* supplier history strip inside detail modal */
    .hist-strip { display: grid; gap: 6px; padding: 10px 0 0; border-top: 1px solid var(--line); }
    .hist-strip__label { font-size: 10px; text-transform: uppercase; letter-spacing: 0.14em; color: var(--text-faint); }
    .hist-strip__items { display: flex; gap: 6px; flex-wrap: wrap; }
    .hist-chip { display: flex; align-items: center; gap: 6px; padding: 5px 10px; background: var(--surface); border: 1px solid var(--line); border-radius: 3px; font-size: 11.5px; color: var(--text-muted); cursor: pointer; transition: border-color 120ms ease; }
    .hist-chip:hover { border-color: var(--aurora-border); }
    .hist-chip span:first-child { color: var(--text-faint); }
    .hist-chip strong { color: var(--text); font-weight: 500; }
    .hist-chip small  { color: var(--text-faint); }

    /* pay modal */
    .pay-summary { display: grid; gap: 4px; }
    .pay-summary__row { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: var(--surface); border-radius: 3px; font-size: 13px; }
    .pay-summary__row--total { background: var(--aurora-ghost); border: 1px solid var(--aurora-border); }
    .pay-summary__row--total span,
    .pay-summary__row--total strong { color: var(--aurora); font-weight: 700; }
    .pay-accounts { display: flex; gap: 6px; }
    .pay-acc { flex: 1; padding: 12px 8px; background: var(--surface); border: 1px solid var(--line-strong); border-radius: 3px; cursor: pointer; font-size: 12px; color: var(--text-muted); display: flex; flex-direction: column; align-items: center; gap: 4px; transition: all 140ms ease; }
    .pay-acc:hover { border-color: var(--aurora-border); }
    .pay-acc--on { border-color: var(--aurora-border); background: var(--aurora-ghost); color: var(--aurora); }
    .pay-acc__icon { font-size: 20px; line-height: 1; }
    .pay-partial-warn { margin: 0; padding: 8px 12px; background: var(--warn-bg); border: 1px solid var(--warn-border); border-radius: 3px; font-size: 12px; color: var(--warn); }
    .mlabel { display: block; font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--text-faint); margin-bottom: 6px; }
    .opt { font-size: 9px; text-transform: none; letter-spacing: 0; color: var(--text-faint); }

    /* responsive */
    @media (max-width: 1100px) {
      .page { padding: 20px 16px; }
      .page__header { flex-direction: column; }
      .page__acts { align-items: flex-start; }
      .main-layout { grid-template-columns: 1fr; }
      .aside { grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); }
      .form-grid { grid-template-columns: 1fr; }
      .lines-head, .line-row { grid-template-columns: 1fr 60px 90px 70px 28px; gap: 4px; }
      .tbl__head, .tbl__row { grid-template-columns: 1.2fr 0.7fr 0.7fr 0.7fr 0.9fr; }
      .tbl__head > *:nth-child(4),
      .tbl__head > *:nth-child(5),
      .tbl__head > *:nth-child(7),
      .tbl__head > *:nth-child(8),
      .tbl__row  > *:nth-child(4),
      .tbl__row  > *:nth-child(5),
      .tbl__row  > *:nth-child(7),
      .tbl__row  > *:nth-child(8) { display: none; }
      .kpi-row { grid-template-columns: repeat(2, 1fr); }
      .tbl-controls { gap: 6px; }
      .tabs { flex-wrap: wrap; }
    }
  `,
})
export class Purchases {
  private readonly erpApi       = inject(ErpApi);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly fileDownload = inject(FileDownloadService);
  private readonly destroyRef   = inject(DestroyRef);

  /* ── data signals ──────────────────────────────────────────────── */
  protected readonly products         = signal<ProductDto[]>([]);
  protected readonly purchases        = signal<PurchaseDto[]>([]);
  protected readonly lowStockProducts = signal<ProductDto[]>([]);
  protected readonly openPayables     = signal<PayableDto[]>([]);
  protected readonly feedback         = signal<UiFeedback | null>(null);
  protected readonly submitting       = signal(false);
  protected readonly loadingPurch     = signal(false);
  protected readonly exporting        = signal<ExportFormat | null>(null);
  protected readonly draftLines       = signal<DraftLine[]>([{ productId: 0, quantity: 1, unitCost: 0 }]);

  /* ── filter/sort signals ───────────────────────────────────────── */
  protected readonly statusFilter = signal<StatusFilter>('ALL');
  protected readonly sortCol      = signal<SortCol>(null);
  protected readonly sortDir      = signal<SortDir>('asc');
  protected readonly page         = signal(0);
  protected readonly pageSize      = 10;

  /* ── modal signals ─────────────────────────────────────────────── */
  protected readonly activeModal    = signal<ActiveModal>(null);
  protected readonly detailPurchase = signal<PurchaseDto | null>(null);
  protected readonly payTarget      = signal<PurchaseDto | null>(null);

  /* ── form fields ───────────────────────────────────────────────── */
  protected supplierName  = '';
  protected supplierRuc   = '';
  protected supplierEmail = '';
  protected docNumber     = '';
  protected dueDate       = '';
  protected notes         = '';
  protected searchQuery   = '';
  protected dateFrom      = '';
  protected dateTo        = '';

  /* ── pay modal fields ──────────────────────────────────────────── */
  protected payAccount   = 'BANK';
  protected payReference = '';
  protected payAmount    = 0;

  /* ── static ────────────────────────────────────────────────────── */
  protected readonly statusTabs: { key: StatusFilter; label: string }[] = [
    { key: 'ALL',      label: 'Todas'     },
    { key: 'DRAFT',    label: 'Borrador'  },
    { key: 'RECEIVED', label: 'Recibidas' },
    { key: 'PAID',     label: 'Pagadas'   },
  ];

  /* ── Escape key closes any modal ───────────────────────────────── */
  @HostListener('document:keydown.escape')
  onEsc(): void { if (this.activeModal()) this.closeModal(); }

  /* ═══════════════════════ DERIVED ═══════════════════════════════ */

  /** Supplier history map built from past purchases */
  protected readonly supplierMap = computed<Map<string, SupplierRecord>>(() => {
    const map = new Map<string, SupplierRecord>();
    for (const p of this.purchases()) {
      const key = p.supplierName.toLowerCase().trim();
      const cur = map.get(key);
      if (cur) {
        cur.count++;
        if (Number(p.total) > cur.lastTotal) cur.lastTotal = Number(p.total);
        if (!cur.ruc && p.supplierIdentification) cur.ruc = p.supplierIdentification;
        if (!cur.email && p.supplierEmail)         cur.email = p.supplierEmail;
      } else {
        map.set(key, {
          name:      p.supplierName,
          ruc:       p.supplierIdentification ?? '',
          email:     p.supplierEmail ?? '',
          lastTotal: Number(p.total),
          count:     1,
        });
      }
    }
    return map;
  });

  /** Unique supplier list for datalist autocomplete */
  protected readonly supplierSuggestions = computed<SupplierRecord[]>(() =>
    Array.from(this.supplierMap().values())
  );

  /** Known supplier record that matches current input */
  protected knownSupplier(): SupplierRecord | null {
    if (!this.supplierName.trim()) return null;
    return this.supplierMap().get(this.supplierName.toLowerCase().trim()) ?? null;
  }

  /** Check duplicate doc number — returns the matching purchase label or null */
  protected docDuplicate(): string | null {
    const doc = this.docNumber.trim().toLowerCase();
    if (!doc) return null;
    const match = this.purchases().find(p =>
      (p.externalDocumentNumber ?? '').toLowerCase() === doc
    );
    return match ? `${match.supplierName} #${match.id}` : null;
  }

  /** Filtered list (search + status + date) */
  protected filtered(): PurchaseDto[] {
    let list = this.purchases();
    const sf = this.statusFilter();
    const q  = this.searchQuery.toLowerCase().trim();
    const df = this.dateFrom;
    const dt = this.dateTo;
    if (sf !== 'ALL') list = list.filter(p => p.status === sf);
    if (q)  list = list.filter(p =>
      p.supplierName.toLowerCase().includes(q) ||
      (p.supplierIdentification ?? '').toLowerCase().includes(q) ||
      (p.externalDocumentNumber ?? '').toLowerCase().includes(q)
    );
    if (df) list = list.filter(p => p.createdAt >= df);
    if (dt) list = list.filter(p => p.createdAt <= dt + 'T23:59:59');
    return list;
  }

  /** Sorted list applied on top of filtered */
  protected sorted(): PurchaseDto[] {
    const col = this.sortCol();
    const dir = this.sortDir();
    if (!col) return this.filtered();
    return [...this.filtered()].sort((a, b) => {
      let va: string | number = 0;
      let vb: string | number = 0;
      if (col === 'supplier') { va = a.supplierName.toLowerCase(); vb = b.supplierName.toLowerCase(); }
      if (col === 'status')   { va = a.status; vb = b.status; }
      if (col === 'total')    { va = Number(a.total); vb = Number(b.total); }
      if (col === 'due')      { va = a.dueDate ?? ''; vb = b.dueDate ?? ''; }
      if (col === 'created')  { va = a.createdAt; vb = b.createdAt; }
      if (va < vb) return dir === 'asc' ? -1 : 1;
      if (va > vb) return dir === 'asc' ?  1 : -1;
      return 0;
    });
  }

  protected paged(): PurchaseDto[] {
    const pageSize = this.pageSize;
    const page = Math.min(this.page(), Math.max(Math.ceil(this.sorted().length / pageSize) - 1, 0));
    return this.sorted().slice(page * pageSize, page * pageSize + pageSize);
  }

  protected totalPages(): number {
    return Math.max(1, Math.ceil(this.sorted().length / this.pageSize));
  }

  protected currentPage(): number {
    return Math.min(this.page(), this.totalPages() - 1);
  }

  protected totalVisible(): number {
    return this.filtered().reduce((s, p) => s + Number(p.total || 0), 0);
  }

  protected overdueCount(): number {
    return this.purchases().filter(p => this.isOverdue(p)).length;
  }

  protected readonly draftTotal = computed(() =>
    this.draftLines().reduce((s, l) => s + this.lineTotal(l), 0)
  );

  /* ═══════════════════════ HELPERS ════════════════════════════════ */

  protected loading(): boolean { return this.loadingPurch(); }

  protected lineTotal(line: DraftLine): number {
    return Number(line.quantity || 0) * Number(line.unitCost || 0);
  }

  protected statusCount(status: string): number {
    return this.purchases().filter(p => p.status === status).length;
  }

  protected totalByStatus(status: string): number {
    return this.purchases().filter(p => p.status === status).reduce((s, p) => s + Number(p.total || 0), 0);
  }

  protected statusLabel(status: string): string {
    if (status === 'PAID') return 'Pagada';
    if (status === 'RECEIVED') return 'Recibida';
    if (status === 'PENDING_APPROVAL') return 'Pendiente aprobación';
    if (status === 'CANCELLED') return 'Cancelada';
    return 'Borrador';
  }

  protected statusClass(status: string): string {
    if (status === 'PAID') return 'chip--paid';
    if (status === 'RECEIVED') return 'chip--received';
    if (status === 'PENDING_APPROVAL') return 'chip--pending';
    if (status === 'CANCELLED') return 'chip--cancelled';
    return 'chip--draft';
  }

  protected isOverdue(p: PurchaseDto): boolean {
    if (!p.dueDate || p.status === 'PAID') return false;
    return new Date(p.dueDate) < new Date();
  }

  protected isDueWithinWeek(p: PurchaseDto): boolean {
    if (!p.dueDate || p.status === 'PAID' || this.isOverdue(p)) return false;
    const diff = new Date(p.dueDate).getTime() - Date.now();
    return diff < 7 * 24 * 60 * 60 * 1000;
  }

  protected isDueDateSoon(): boolean {
    if (!this.dueDate) return false;
    const diff = new Date(this.dueDate).getTime() - Date.now();
    return diff > 0 && diff < 7 * 24 * 60 * 60 * 1000;
  }

  protected isPayableOverdue(pay: PayableDto): boolean {
    if (!pay.dueDate) return false;
    return new Date(pay.dueDate) < new Date();
  }

  protected nextStep(): string {
    if (this.statusCount('DRAFT')    > 0) return 'Recibir';
    if (this.statusCount('RECEIVED') > 0) return 'Pagar';
    return 'Ordenado';
  }

  /** Last purchase unit cost for a given product from history (for hint display) */
  protected lastCostHint(productId: number): number | null {
    if (!productId) return null;
    const past = this.purchases()
      .flatMap(p => p.items.filter(i => i.productId === productId).map(i => ({ cost: i.unitCost, date: p.createdAt })))
      .sort((a, b) => b.date.localeCompare(a.date));
    return past.length ? past[0].cost : null;
  }

  /** Recent purchases from same supplier, excluding current */
  protected supplierPurchaseHistory(name: string, excludeId: number): PurchaseDto[] {
    return this.purchases()
      .filter(p => p.supplierName === name && p.id !== excludeId)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .slice(0, 4);
  }

  /* ── sort ────────────────────────────────────────────────────────*/
  protected toggleSort(col: SortCol): void {
    if (this.sortCol() === col) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortCol.set(col);
      this.sortDir.set('asc');
    }
    this.page.set(0);
  }

  protected sortIcon(col: SortCol): string {
    if (this.sortCol() !== col) return '↕';
    return this.sortDir() === 'asc' ? '↑' : '↓';
  }

  /* ── filter helpers ──────────────────────────────────────────────*/
  protected setFilter(f: StatusFilter): void {
    this.statusFilter.set(this.statusFilter() === f ? 'ALL' : f);
    this.page.set(0);
  }

  protected clearFilters(): void {
    this.statusFilter.set('ALL');
    this.searchQuery = '';
    this.dateFrom    = '';
    this.dateTo      = '';
    this.sortCol.set(null);
    this.page.set(0);
  }

  protected setPage(page: number): void {
    this.page.set(page);
  }

  /* ── modals ──────────────────────────────────────────────────────*/
  protected openDetail(p: PurchaseDto): void {
    this.detailPurchase.set(p);
    this.activeModal.set('detail');
  }

  protected openPayModal(p: PurchaseDto): void {
    this.payTarget.set(p);
    this.payAmount    = p.total;
    this.payReference = '';
    this.payAccount   = 'BANK';
    this.activeModal.set('pay');
  }

  protected closeModal(): void {
    this.activeModal.set(null);
    this.detailPurchase.set(null);
    this.payTarget.set(null);
  }

  /* ── supplier auto-fill ──────────────────────────────────────────*/
  protected onSupplierChange(name: string): void {
    this.supplierName = name;
    const rec = this.supplierMap().get(name.toLowerCase().trim());
    if (rec) {
      if (!this.supplierRuc   && rec.ruc)   this.supplierRuc   = rec.ruc;
      if (!this.supplierEmail && rec.email) this.supplierEmail = rec.email;
    }
  }

  /* ── quick-add low-stock product ─────────────────────────────────*/
  protected quickAddProduct(p: ProductDto): void {
    const existingIdx = this.draftLines().findIndex(l => l.productId === p.id);
    if (existingIdx >= 0) {
      this.updateLine(existingIdx, 'quantity', this.draftLines()[existingIdx].quantity + 1);
    } else {
      const cost = p.purchasePrice ?? 0;
      const emptyIdx = this.draftLines().findIndex(l => l.productId === 0);
      if (emptyIdx >= 0) {
        const lines = [...this.draftLines()];
        lines[emptyIdx] = { productId: p.id, quantity: 1, unitCost: cost };
        this.draftLines.set(lines);
      } else {
        this.draftLines.set([...this.draftLines(), { productId: p.id, quantity: 1, unitCost: cost }]);
      }
    }
    this.feedback.set({ tone: 'info', message: `${p.name} añadido al formulario.` });
    setTimeout(() => this.feedback.set(null), 2500);
  }

  /* ── draft line management ───────────────────────────────────────*/
  protected addLine(): void {
    this.draftLines.set([...this.draftLines(), { productId: 0, quantity: 1, unitCost: 0 }]);
  }

  protected removeLine(index: number): void {
    if (this.draftLines().length <= 1) return;
    this.draftLines.set(this.draftLines().filter((_, i) => i !== index));
  }

  protected updateLine(index: number, key: keyof DraftLine, value: number): void {
    const lines = this.draftLines().map((l, i) =>
      i === index ? { ...l, [key]: Number(value) } : l
    );
    this.draftLines.set(lines);

    // auto-fill purchase price when product is selected
    if (key === 'productId') {
      const prod = this.products().find(p => p.id === Number(value));
      if (prod?.purchasePrice && prod.purchasePrice > 0) {
        this.draftLines.set(this.draftLines().map((l, i) =>
          i === index ? { ...l, unitCost: prod.purchasePrice! } : l
        ));
      }
    }
  }

  /* ── data load ───────────────────────────────────────────────────*/
  protected reload(): void {
    this.loadingPurch.set(true);
    this.feedback.set(null);

    this.erpApi.getProducts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  page => this.products.set(page.content ?? []),
        error: err  => this.feedback.set(this.httpFeedback.fromError(err, 'No se pudieron cargar los productos.')),
      });

    this.erpApi.getLowStockProducts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: list => this.lowStockProducts.set(list ?? []), error: () => {} });

    this.erpApi.getPurchases()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  list => { this.purchases.set(list ?? []); this.loadingPurch.set(false); },
        error: err  => { this.feedback.set(this.httpFeedback.fromError(err, 'No se pudieron cargar las compras.')); this.loadingPurch.set(false); },
      });

    this.erpApi.getPayables()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: list => this.openPayables.set((list ?? []).filter(p => p.status !== 'PAID' && p.balance > 0)),
        error: () => {},
    });
    this.page.set(0);
  }

  protected exportPurchases(format: ExportFormat): void {
    if (this.exporting()) {
      return;
    }
    this.exporting.set(format);
    this.erpApi.exportPurchases(format, {
      status: this.statusFilter() === 'ALL' ? undefined : this.statusFilter(),
      search: this.searchQuery.trim() || undefined,
      from: this.dateFrom || undefined,
      to: this.dateTo || undefined,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: response => {
          this.fileDownload.download(response, `compras.${format}`);
          this.exporting.set(null);
        },
        error: err => {
          this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo exportar las compras.'));
          this.exporting.set(null);
        },
      });
  }

  /* ── submit new purchase ─────────────────────────────────────────*/
  protected canSubmitPurchase(): boolean {
    if (!this.supplierName.trim()) return false;
    if (this.docDuplicate()) return false;
    const validLines = this.draftLines().filter(l => l.productId > 0 && l.quantity > 0 && l.unitCost > 0);
    return validLines.length > 0;
  }

  protected purchaseHint(): string {
    if (!this.supplierName.trim()) return 'Ingresa el nombre del proveedor para continuar.';
    if (this.docDuplicate()) return `El número de documento ya existe en ${this.docDuplicate()}.`;
    const validLines = this.draftLines().filter(l => l.productId > 0 && l.quantity > 0 && l.unitCost > 0);
    if (!validLines.length) return 'Agrega al menos una línea con producto, cantidad y costo.';
    return '';
  }

  protected submitPurchase(): void {
    this.feedback.set(null);
    if (!this.supplierName.trim()) {
      this.feedback.set(this.httpFeedback.warning('El nombre del proveedor es obligatorio.'));
      return;
    }
    if (this.docDuplicate()) {
      this.feedback.set(this.httpFeedback.warning(`El número de documento ya existe en ${this.docDuplicate()}.`));
      return;
    }
    const items = this.draftLines().filter(l => l.productId > 0 && l.quantity > 0 && l.unitCost > 0);
    if (!items.length) {
      this.feedback.set(this.httpFeedback.warning('Agrega al menos una línea con producto, cantidad y costo.'));
      return;
    }
    this.submitting.set(true);
    this.erpApi.createPurchase({
      supplierName:           this.supplierName.trim(),
      supplierIdentification: this.supplierRuc.trim()   || undefined,
      supplierEmail:          this.supplierEmail.trim()  || undefined,
      externalDocumentNumber: this.docNumber.trim()      || undefined,
      dueDate:                this.dueDate               || undefined,
      notes:                  this.notes.trim()          || undefined,
      items: items.map(l => ({ productId: l.productId, quantity: l.quantity, unitCost: l.unitCost })),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  p   => { this.feedback.set(this.httpFeedback.success(`Compra #${p.id} creada — ${p.supplierName}.`)); this.submitting.set(false); this.resetForm(); this.reload(); },
        error: err => { this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo crear la compra.')); this.submitting.set(false); },
      });
  }

  /* ── receive ─────────────────────────────────────────────────────*/
  protected receivePurchase(id: number): void {
    this.submitting.set(true);
    this.erpApi.receivePurchase(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  () => { this.feedback.set(this.httpFeedback.success(`Compra #${id} recibida. Inventario actualizado.`)); this.submitting.set(false); this.reload(); },
        error: err => { this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo recibir la compra.')); this.submitting.set(false); },
      });
  }

  protected receiveFromDetail(id: number): void {
    this.closeModal();
    this.receivePurchase(id);
  }

  /* ── pay ─────────────────────────────────────────────────────────*/
  protected submitPay(): void {
    const p = this.payTarget();
    if (!p || this.payAmount <= 0) return;
    this.submitting.set(true);
    this.erpApi.payPurchase(p.id, {
      amount:      this.payAmount,
      reference:   this.payReference.trim() || undefined,
      accountCode: this.payAccount,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  () => { this.feedback.set(this.httpFeedback.success(`Pago de ${this.payAmount.toFixed(2)} USD registrado para compra #${p.id}.`)); this.submitting.set(false); this.closeModal(); this.reload(); },
        error: err => { this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo registrar el pago.')); this.submitting.set(false); },
      });
  }

  /* ── reset ───────────────────────────────────────────────────────*/
  protected resetForm(): void {
    this.supplierName  = '';
    this.supplierRuc   = '';
    this.supplierEmail = '';
    this.docNumber     = '';
    this.dueDate       = '';
    this.notes         = '';
    this.draftLines.set([{ productId: 0, quantity: 1, unitCost: 0 }]);
  }
}
