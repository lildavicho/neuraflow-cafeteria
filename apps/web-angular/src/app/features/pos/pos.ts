import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UiFeedback } from '../../core/models/ui-feedback';
import { HttpFeedback } from '../../core/services/http-feedback';
import { OrderDto, ErpApi, ProductDto, CashRegisterDto } from '../../core/services/erp-api';
import { RequestFeedback } from '../shared/components/request-feedback';

type CartLine = ProductDto & { quantity: number; discountPercent: number };
type ActiveModal = 'cancel' | 'refund' | 'confirm-transfer' | 'receipt' | 'order-detail' | 'open-register' | 'close-register' | 'z-report' | null;
type DocumentType = 'NOTA_VENTA' | 'FACTURA';
type SplitEntry = { method: 'CASH' | 'CARD' | 'TRANSFER'; amount: number; reference: string };
type CardNetwork = 'VISA' | 'MC' | 'AMEX' | 'DINERS' | 'DEBIT';
type InvoiceMeta = { identification: string; name: string; email?: string; address?: string };

const ECUADOR_BANKS = [
  'Banco Pichincha', 'Produbanco', 'Banco Guayaquil', 'Banco Internacional',
  'Banco del Pacífico', 'Banco Bolivariano', 'Diners Club del Ecuador',
  'Banco de Loja', 'Banco del Austro', 'Banco Solidario',
  'Cooperativa JEP', 'Cooperativa Jardín Azuayo', 'BanEcuador',
] as const;

const CARD_TYPES: { value: CardNetwork; label: string }[] = [
  { value: 'VISA', label: 'Visa' },
  { value: 'MC', label: 'Mastercard' },
  { value: 'AMEX', label: 'Amex' },
  { value: 'DINERS', label: 'Diners' },
  { value: 'DEBIT', label: 'Débito' },
];

const QUICK_BILLS = [1, 5, 10, 20, 50, 100];

@Component({
  selector: 'app-pos',
  imports: [FormsModule, CurrencyPipe, DatePipe, RequestFeedback],
  template: `
    <!-- ══════════════════════ MODAL LAYER ══════════════════════ -->
    @if (activeModal()) {
      <div class="modal-bd" (click)="closeModal()">
        <div class="modal-box" (click)="$event.stopPropagation()">

          <!-- CANCELAR -->
          @if (activeModal() === 'cancel') {
            <header class="modal__hd">
              <span class="eyebrow">Orden #{{ modalOrderId() }}</span>
              <h3>Cancelar venta</h3>
            </header>
            <p class="modal__hint">Libera el stock reservado. Esta acción no puede deshacerse.</p>
            <label class="mlabel">Motivo de cancelación</label>
              <textarea class="field field--ta" [(ngModel)]="modalReason"
                placeholder="ej. Cliente desistió, error en pedido..." rows="3"></textarea>
            <div class="modal__foot">
              <button class="ghost" (click)="closeModal()">Volver</button>
              <button class="btn-danger" [disabled]="submitting()" (click)="submitCancelOrder()">
                {{ submitting() ? 'Cancelando...' : 'Confirmar cancelación' }}
              </button>
            </div>
          }

          <!-- REEMBOLSAR -->
          @if (activeModal() === 'refund') {
            <header class="modal__hd">
              <span class="eyebrow">Orden #{{ modalOrderId() }}</span>
              <h3>Reembolsar venta</h3>
            </header>
            <p class="modal__hint">Se revierte el cobro y se libera el inventario comprometido.</p>
            <label class="mlabel">Motivo del reembolso</label>
              <textarea class="field field--ta" [(ngModel)]="modalReason"
                placeholder="ej. Producto defectuoso, insatisfacción..." rows="3"></textarea>
            <div class="modal__foot">
              <button class="ghost" (click)="closeModal()">Volver</button>
              <button class="btn-danger" [disabled]="submitting()" (click)="submitRefundOrder()">
                {{ submitting() ? 'Reembolsando...' : 'Confirmar reembolso' }}
              </button>
            </div>
          }

          <!-- CONFIRMAR TRANSFERENCIA -->
          @if (activeModal() === 'confirm-transfer') {
            <header class="modal__hd">
              <span class="eyebrow">Orden #{{ modalOrderId() }}</span>
              <h3>Confirmar transferencia</h3>
            </header>
            <p class="modal__hint">Confirma la recepción del pago bancario para cerrar la venta.</p>
            <label class="mlabel">Número de comprobante</label>
            <input class="field" [(ngModel)]="modalTransferRef"
              placeholder="Comprobante o referencia bancaria" />
            <div class="modal__foot">
              <button class="ghost" (click)="closeModal()">Volver</button>
              <button class="btn-primary" [disabled]="submitting()" (click)="submitConfirmTransfer()">
                {{ submitting() ? 'Confirmando...' : 'Confirmar pago recibido' }}
              </button>
            </div>
          }

          <!-- TICKET / COMPROBANTE -->
          @if (activeModal() === 'receipt' && lastReceipt(); as r) {
            <header class="modal__hd modal__hd--c">
              <span class="eyebrow">{{ receiptDocumentLabel(r) }}</span>
              <h3>Orden #{{ r.id }}</h3>
              <span class="rcpt__date">{{ (r.paidAt || r.createdAt) | date:'dd/MM/yyyy HH:mm' }}</span>
            </header>
            @if (invoiceMetaFromOrder(r); as invoice) {
              <div class="invoice-meta">
                <div class="invoice-meta__row"><span>Identificación</span><strong>{{ invoice.identification }}</strong></div>
                <div class="invoice-meta__row"><span>Razón social</span><strong>{{ invoice.name }}</strong></div>
                @if (invoice.address) {
                  <div class="invoice-meta__row"><span>Dirección</span><strong>{{ invoice.address }}</strong></div>
                }
                @if (invoice.email) {
                  <div class="invoice-meta__row"><span>Email</span><strong>{{ invoice.email }}</strong></div>
                }
              </div>
            }
            <div class="rcpt__items">
              @for (item of r.items; track item.id) {
                <div class="rcpt__item">
                  <span class="rcpt__iname">{{ item.productName }}</span>
                  <span class="rcpt__iqty">x {{ item.quantity }}</span>
                  <span class="rcpt__iprice">
                    {{ (item.lineTotal ?? (item.unitPrice * item.quantity)) | currency:'USD':'symbol':'1.2-2' }}
                  </span>
                </div>
              }
            </div>
            <div class="rcpt__totals">
              <div class="rcpt__row"><span>Subtotal</span><span>{{ r.subtotal | currency:'USD':'symbol':'1.2-2' }}</span></div>
              <div class="rcpt__row"><span>IVA 15%</span><span>{{ r.tax | currency:'USD':'symbol':'1.2-2' }}</span></div>
              <div class="rcpt__row rcpt__row--g"><span>Total</span><span>{{ r.total | currency:'USD':'symbol':'1.2-2' }}</span></div>
              @if (r.paymentMethod) {
                <div class="rcpt__row"><span>Forma de pago</span><span>{{ paymentDisplayLabel(r) }}</span></div>
              }
              @if (paymentBreakdown(r); as payments) {
                @for (entry of payments; track $index) {
                  <div class="rcpt__row">
                    <span>{{ paymentBreakdownLabel(entry) }}</span>
                    <span>
                      {{ entry.amount | currency:'USD':'symbol':'1.2-2' }}@if (entry.reference) { · {{ entry.reference }} }
                    </span>
                  </div>
                }
              }
              @if (r.paymentReference) {
                <div class="rcpt__row"><span>Referencia</span><span class="rcpt__ref">{{ r.paymentReference }}</span></div>
              }
            </div>
            <div class="modal__foot">
              <button class="ghost" (click)="closeModal()">Cerrar</button>
              <button class="btn-primary" (click)="printReceipt()">Imprimir ticket</button>
            </div>
          }

          <!-- DETALLE ORDEN -->
          @if (activeModal() === 'order-detail' && detailOrder(); as o) {
            <header class="modal__hd">
              <span class="eyebrow">{{ orderStatusLabel(o) }}</span>
              <h3>Orden #{{ o.id }}</h3>
            </header>
            @if (invoiceMetaFromOrder(o); as invoice) {
              <div class="invoice-meta">
                <div class="invoice-meta__row"><span>Identificación</span><strong>{{ invoice.identification }}</strong></div>
                <div class="invoice-meta__row"><span>Razón social</span><strong>{{ invoice.name }}</strong></div>
                @if (invoice.address) {
                  <div class="invoice-meta__row"><span>Dirección</span><strong>{{ invoice.address }}</strong></div>
                }
                @if (invoice.email) {
                  <div class="invoice-meta__row"><span>Email</span><strong>{{ invoice.email }}</strong></div>
                }
              </div>
            }
            <div class="rcpt__items">
              @for (item of o.items; track item.id) {
                <div class="rcpt__item">
                  <span class="rcpt__iname">{{ item.productName }}</span>
                  <span class="rcpt__iqty">x {{ item.quantity }}</span>
                  <span class="rcpt__iprice">
                    {{ (item.lineTotal ?? (item.unitPrice * item.quantity)) | currency:'USD':'symbol':'1.2-2' }}
                  </span>
                </div>
              }
            </div>
            <div class="rcpt__totals">
              <div class="rcpt__row"><span>Subtotal</span><span>{{ o.subtotal | currency:'USD':'symbol':'1.2-2' }}</span></div>
              <div class="rcpt__row"><span>IVA 15%</span><span>{{ o.tax | currency:'USD':'symbol':'1.2-2' }}</span></div>
              <div class="rcpt__row rcpt__row--g"><span>Total</span><span>{{ o.total | currency:'USD':'symbol':'1.2-2' }}</span></div>
              @if (o.paymentMethod) {
                <div class="rcpt__row"><span>Pago</span><span>{{ paymentDisplayLabel(o) }}</span></div>
              }
              @if (paymentBreakdown(o); as payments) {
                @for (entry of payments; track $index) {
                  <div class="rcpt__row">
                    <span>{{ paymentBreakdownLabel(entry) }}</span>
                    <span>
                      {{ entry.amount | currency:'USD':'symbol':'1.2-2' }}@if (entry.reference) { · {{ entry.reference }} }
                    </span>
                  </div>
                }
              }
              @if (o.paymentReference) {
                <div class="rcpt__row"><span>Referencia</span><span class="rcpt__ref">{{ o.paymentReference }}</span></div>
              }
              @if (visibleOrderNotes(o.notes); as orderNotes) {
                <div class="rcpt__row"><span>Notas</span><span>{{ orderNotes }}</span></div>
              }
            </div>
            <div class="modal__foot">
              <button class="ghost" (click)="closeModal()">Cerrar</button>
            </div>
          }

          <!-- ABRIR CAJA -->
          @if (activeModal() === 'open-register') {
            <header class="modal__hd">
              <span class="eyebrow">Apertura de caja</span>
              <h3>Abrir caja registradora</h3>
            </header>
            <p class="modal__hint">Ingresa el monto de efectivo con el que inicias el turno.</p>
            <label class="mlabel">Monto inicial en caja</label>
            <input type="number" class="field field--amount" [(ngModel)]="registerOpeningAmount"
              placeholder="0.00" min="0" step="0.01" />
            <div class="modal__foot">
              <button class="ghost" (click)="closeModal()">Cancelar</button>
              <button class="btn-primary" [disabled]="submitting()" (click)="submitOpenRegister()">
                {{ submitting() ? 'Abriendo...' : 'Abrir caja' }}
              </button>
            </div>
          }

          <!-- Z-REPORT (post-cierre) -->
          @if (activeModal() === 'z-report' && zReportData(); as z) {
            <header class="modal__hd modal__hd--c">
              <span class="eyebrow">Reporte Z · Cierre de turno</span>
              <h3>Caja cerrada</h3>
              @if (z.closedAt) {
                <span class="rcpt__date">{{ z.closedAt | date:'dd/MM/yyyy HH:mm' }}</span>
              }
            </header>

            <!-- Resumen de ventas por método -->
            <div class="zrpt__section">
              <p class="zrpt__section-title">Ventas por método de pago</p>
              <div class="rcpt__totals">
                <div class="rcpt__row">
                  <span>Efectivo</span>
                  <span>{{ (z.cashSalesTotal ?? 0) | currency:'USD':'symbol':'1.2-2' }}</span>
                </div>
                <div class="rcpt__row">
                  <span>Tarjeta</span>
                  <span>{{ (z.cardSalesTotal ?? 0) | currency:'USD':'symbol':'1.2-2' }}</span>
                </div>
                <div class="rcpt__row">
                  <span>Transferencia</span>
                  <span>{{ (z.transferSalesTotal ?? 0) | currency:'USD':'symbol':'1.2-2' }}</span>
                </div>
                <div class="rcpt__row rcpt__row--g">
                  <span>Total ventas ({{ z.totalOrders ?? 0 }} órdenes)</span>
                  <span>{{ (z.totalSalesAmount ?? 0) | currency:'USD':'symbol':'1.2-2' }}</span>
                </div>
              </div>
            </div>

            <!-- Arqueo de caja -->
            <div class="zrpt__section">
              <p class="zrpt__section-title">Arqueo de efectivo</p>
              <div class="rcpt__totals">
                <div class="rcpt__row">
                  <span>Fondo inicial</span>
                  <span>{{ (z.openingAmount ?? 0) | currency:'USD':'symbol':'1.2-2' }}</span>
                </div>
                <div class="rcpt__row">
                  <span>Ventas en efectivo</span>
                  <span>{{ (z.cashSalesTotal ?? 0) | currency:'USD':'symbol':'1.2-2' }}</span>
                </div>
                <div class="rcpt__row">
                  <span>Efectivo esperado</span>
                  <span>{{ (z.expectedCash ?? 0) | currency:'USD':'symbol':'1.2-2' }}</span>
                </div>
                <div class="rcpt__row">
                  <span>Efectivo contado</span>
                  <span>{{ (z.actualCash ?? 0) | currency:'USD':'symbol':'1.2-2' }}</span>
                </div>
                <div class="rcpt__row" [class.rcpt__row--ok]="(z.difference ?? 0) >= 0"
                                       [class.rcpt__row--bad]="(z.difference ?? 0) < 0">
                  <span>Diferencia</span>
                  <span>{{ (z.difference ?? 0) >= 0 ? '+' : '' }}{{ (z.difference ?? 0) | currency:'USD':'symbol':'1.2-2' }}</span>
                </div>
              </div>
            </div>

            @if (z.notes) {
              <div class="zrpt__section">
                <p class="zrpt__section-title">Notas de cierre</p>
                <p class="zrpt__notes">{{ z.notes }}</p>
              </div>
            }

            <div class="modal__foot">
              <button class="ghost" (click)="closeModal()">Cerrar</button>
              <button class="btn-primary" (click)="printZReport()">Imprimir reporte Z</button>
            </div>
          }

          <!-- CERRAR CAJA -->
          @if (activeModal() === 'close-register') {
            <header class="modal__hd">
              <span class="eyebrow">Cierre de caja - Z-Report</span>
              <h3>Cerrar caja registradora</h3>
            </header>
            <p class="modal__hint">Cuenta el efectivo en caja y registra el monto real.</p>
            @if (cashRegister(); as reg) {
              <div class="zreport-info">
                <div class="rcpt__row"><span>Apertura</span><span>{{ reg.openingAmount | currency:'USD':'symbol':'1.2-2' }}</span></div>
                <div class="rcpt__row"><span>Abierta desde</span><span>{{ reg.openedAt | date:'dd/MM HH:mm' }}</span></div>
              </div>
            }
            <label class="mlabel">Efectivo real contado</label>
            <input type="number" class="field field--amount" [(ngModel)]="registerActualCash"
              placeholder="0.00" min="0" step="0.01" />
            <label class="mlabel">Notas de cierre <span class="opt">(opcional)</span></label>
            <textarea class="field field--ta" [(ngModel)]="registerCloseNotes"
              placeholder="Observaciones del turno..." rows="2"></textarea>
            <div class="modal__foot">
              <button class="ghost" (click)="closeModal()">Cancelar</button>
              <button class="btn-danger" [disabled]="submitting()" (click)="submitCloseRegister()">
                {{ submitting() ? 'Cerrando...' : 'Cerrar caja y generar reporte' }}
              </button>
            </div>
          }

        </div>
      </div>
    }

    <!-- ══════════════════════ PÁGINA PRINCIPAL ══════════════════════ -->
    <section class="page">

      <!-- HEADER -->
      <header class="page__header">
        <div>
          <span class="eyebrow">Operación diaria</span>
          <h1>Punto de venta</h1>
        </div>
        <div class="page__meta">
          @if (pendingOrders() > 0) {
            <span class="badge-warn">{{ pendingOrders() }} pendiente{{ pendingOrders() !== 1 ? 's' : '' }}</span>
          }
          <button type="button" class="ghost" (click)="reloadAll()" [disabled]="isBusy()">Recargar</button>
        </div>
      </header>

      @if (feedback(); as notice) {
        <app-request-feedback [tone]="notice.tone" [message]="notice.message" [traceId]="notice.traceId" />
      }

      <!-- STATS -->
      <div class="stats-grid">
        <article class="stat-card">
          <span>Productos</span>
          <strong>{{ filteredProducts().length }}</strong>
          <small>{{ products().length }} en catálogo</small>
        </article>
        <article class="stat-card">
          <span>Carrito</span>
          <strong>{{ cart().length }}</strong>
          <small>{{ cartUnits() }} unidades</small>
        </article>
        <article class="stat-card stat-card--hl">
          <span>Total a cobrar</span>
          <strong>{{ finalTotal() | currency:'USD':'symbol':'1.2-2' }}</strong>
          <small>IVA: {{ finalTax() | currency:'USD':'symbol':'1.2-2' }}</small>
        </article>
        <article class="stat-card">
          <span>Ventas abiertas</span>
          <strong>{{ pendingOrders() }}</strong>
          <small>pendientes de cobro</small>
        </article>
      </div>

      <!-- CASH REGISTER BAR -->
      @if (!isRegisterOpen() && !registerLoading()) {
        <div class="register-bar register-bar--closed" (click)="openRegisterModal()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2"/></svg>
          <span>Caja cerrada - <strong>Abrir caja</strong> para vender</span>
          <div class="register-bar__actions">
            <button type="button" class="ghost ghost--xs" (click)="openRegisterModal(); $event.stopPropagation()">Abrir caja</button>
          </div>
        </div>
      }
      @if (isRegisterOpen()) {
        <div class="register-bar register-bar--open">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2"/></svg>
          <span>Caja abierta desde {{ cashRegister()?.openedAt | date:'HH:mm' }} - {{ (cashRegister()?.openingAmount ?? 0) | currency:'USD':'symbol':'1.2-2' }} inicial</span>
          <button class="ghost ghost--xs" (click)="closeRegisterModal()">Cerrar caja</button>
        </div>
      }

      <!-- ── LAYOUT PRINCIPAL ── -->
      <div class="pos-layout">

        <!-- ── CATÁLOGO ── -->
        <section class="panel">
          <div class="panel__hd">
            <span class="eyebrow">Catálogo</span>
            <input type="search" [(ngModel)]="search" class="field field--search"
              placeholder="Buscar producto o código..." />
          </div>
          @if (productsLoading()) {
            <p class="state-msg">Cargando catálogo...</p>
          } @else if (!filteredProducts().length) {
            <p class="state-msg">Sin resultados{{ search ? ' para "' + search + '"' : '' }}.</p>
          } @else {
            <div class="product-scroll">
              <div class="product-grid">
                @for (p of filteredProducts(); track p.id) {
                  <button type="button" class="pcard"
                    [class.pcard--low]="p.lowStock"
                    [class.pcard--out]="(p.availableStock ?? p.stock ?? 0) <= 0"
                    [disabled]="(p.availableStock ?? p.stock ?? 0) <= 0 || submitting()"
                    (click)="addToCart(p)">
                    <span class="pcard__cat">{{ p.categoryName || 'General' }}</span>
                    <strong class="pcard__name">{{ p.name }}</strong>
                    <span class="pcard__price">{{ p.price | currency:'USD':'symbol':'1.2-2' }}</span>
                    <span class="pcard__stock"
                      [class.pcard__stock--low]="p.lowStock"
                      [class.pcard__stock--out]="(p.availableStock ?? p.stock ?? 0) <= 0">
                      @if ((p.availableStock ?? p.stock ?? 0) <= 0) { Sin stock }
                      @else { {{ p.availableStock ?? p.stock ?? 0 }} disponibles }
                    </span>
                  </button>
                }
              </div>
            </div>
          }
        </section>

        <!-- ── CAJA / CHECKOUT ── -->
        <section class="panel panel--checkout">

          <!-- Carrito header -->
          <div class="panel__hd panel__hd--row">
            <span class="eyebrow">Carrito</span>
            @if (cart().length) {
              <button type="button" class="ghost ghost--xs" (click)="clearCart()">Vaciar todo</button>
            }
          </div>

          <!-- Items del carrito -->
          <div class="cart-area">
            @if (!cart().length) {
              <p class="state-msg state-msg--c">Agrega productos para comenzar.</p>
            } @else {
              @for (line of cart(); track line.id) {
                <article class="cart-line">
                  <div class="cart-line__info">
                    <strong>{{ line.name }}</strong>
                    <span>{{ line.price | currency:'USD':'symbol':'1.2-2' }} c/u</span>
                  </div>
                  <div class="cart-line__ctrl">
                    <button class="qty-btn" type="button"
                      (click)="updateQuantity(line.id, line.quantity - 1)"
                      [disabled]="line.quantity <= 1">-</button>
                    <input type="number" min="1" class="qty-input"
                      [ngModel]="line.quantity"
                      (ngModelChange)="updateQuantity(line.id, $event)" />
                    <button class="qty-btn" type="button"
                      (click)="updateQuantity(line.id, line.quantity + 1)">+</button>
                    <input type="number" min="0" max="100" class="disc-input" title="Descuento %"
                      [ngModel]="line.discountPercent"
                      (ngModelChange)="updateLineDiscount(line.id, $event)" placeholder="%" />
                    <span class="cart-line__lt">{{ (line.price * line.quantity - lineDiscountAmount(line)) | currency:'USD':'symbol':'1.2-2' }}</span>
                    <button class="icon-btn" type="button" (click)="removeFromCart(line.id)">x</button>
                  </div>
                </article>
              }
            }
          </div>

          <!-- Totales -->
          <div class="totals-block">
            <div class="totals-row">
              <span>Subtotal</span>
              <span>{{ totals().subtotal | currency:'USD':'symbol':'1.2-2' }}</span>
            </div>
            @if (totalLineDiscounts() > 0) {
              <div class="totals-row totals-row--disc">
                <span>Desc. línea</span>
                <span>-{{ totalLineDiscounts() | currency:'USD':'symbol':'1.2-2' }}</span>
              </div>
            }
            <div class="totals-row totals-row--disc totals-row--disc-input">
              <span class="totals-row__label">Desc. global</span>
              <div class="disc-global-box">
                <span class="disc-global-prefix">$</span>
                <input type="number" min="0" step="0.50" class="disc-global-input"
                  [(ngModel)]="globalDiscount" placeholder="0.00" />
                <span class="disc-global-value">
                  @if (effectiveGlobalDiscount() > 0) {
                    -{{ effectiveGlobalDiscount() | currency:'USD':'symbol':'1.2-2' }}
                  } @else {
                    {{ 0 | currency:'USD':'symbol':'1.2-2' }}
                  }
                </span>
              </div>
            </div>
            <div class="totals-row">
              <span>IVA 15%</span>
              <span>{{ finalTax() | currency:'USD':'symbol':'1.2-2' }}</span>
            </div>
            <div class="totals-row totals-row--g">
              <span>Total</span>
              <span>{{ finalTotal() | currency:'USD':'symbol':'1.2-2' }}</span>
            </div>
            @if (totalsLoading()) {
              <span class="totals-calc">calculando...</span>
            }
          </div>

          <!-- TIPO DE COMPROBANTE -->
          <div class="doc-type-section">
            <span class="eyebrow">Tipo de comprobante</span>
            <div class="chip-row">
              <button type="button" class="chip" [class.chip--on]="documentType === 'NOTA_VENTA'" (click)="documentType = 'NOTA_VENTA'">Nota de venta</button>
              <button type="button" class="chip" [class.chip--on]="documentType === 'FACTURA'" (click)="documentType = 'FACTURA'">Factura</button>
            </div>
            @if (documentType === 'FACTURA') {
              <div class="factura-fields">
                <label class="mlabel">RUC / Cédula <span class="req">*</span></label>
                <input [(ngModel)]="clientId" class="field" placeholder="0912345678 (10 o 13 dígitos)" maxlength="13" />
                @if (clientId.trim() && !isValidCI()) {
                  <span class="field-error">Debe tener 10 o 13 dígitos</span>
                }
                <label class="mlabel">Razón social <span class="req">*</span></label>
                <input [(ngModel)]="clientName" class="field" placeholder="Nombre o razón social" />
                <label class="mlabel">Dirección del receptor <span class="req">*</span></label>
                <textarea [(ngModel)]="clientAddress" class="field field--ta" placeholder="Dirección fiscal del receptor" rows="2"></textarea>
                <label class="mlabel">Email <span class="opt">(opcional)</span></label>
                <input [(ngModel)]="clientEmail" class="field" placeholder="correo@ejemplo.com" type="email" />
              </div>
            }
          </div>

          <!-- METODOS DE PAGO -->
          <div class="pay-section">
            <span class="eyebrow">Forma de pago</span>

            <!-- TABS -->
            <div class="pay-tabs">
              <button type="button" class="pay-tab"
                [class.pay-tab--on]="paymentMethod === 'CASH'"
                (click)="setPaymentMethod('CASH')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="2" y="6" width="20" height="12" rx="2"/>
                  <circle cx="12" cy="12" r="3"/>
                  <path d="M6 12h.01M18 12h.01"/>
                </svg>
                <span>Efectivo</span>
              </button>
              <button type="button" class="pay-tab"
                [class.pay-tab--on]="paymentMethod === 'CARD'"
                (click)="setPaymentMethod('CARD')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="2" y="5" width="20" height="14" rx="2"/>
                  <line x1="2" y1="10" x2="22" y2="10"/>
                  <line x1="6" y1="15" x2="10" y2="15"/>
                </svg>
                <span>Tarjeta</span>
              </button>
              <button type="button" class="pay-tab"
                [class.pay-tab--on]="paymentMethod === 'TRANSFER'"
                (click)="setPaymentMethod('TRANSFER')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/>
                  <polyline points="9 22 9 12 15 12 15 22"/>
                </svg>
                <span>Transferencia</span>
              </button>
              <button type="button" class="pay-tab"
                [class.pay-tab--on]="paymentMethod === 'MIXED'"
                (click)="setPaymentMethod('MIXED')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="8" cy="12" r="4"/><circle cx="16" cy="12" r="4"/></svg>
                <span>Mixto</span>
              </button>
            </div>

            <!-- ── EFECTIVO ── -->
            @if (paymentMethod === 'CASH') {
              <div class="pay-panel">
                <label class="mlabel">Monto recibido</label>
                <input type="number" [(ngModel)]="cashReceived" class="field field--amount"
                  placeholder="0.00" min="0" step="0.01" />
                <div class="quick-bills">
                  @for (bill of quickBills; track bill) {
                    <button type="button" class="ghost ghost--xs"
                      (click)="setCashReceived(bill)">{{ billLabel(bill) }}</button>
                  }
                </div>
                @if (cashReceived && +cashReceived > 0 && totals().total > 0) {
                  <div class="change-box" [class.change-box--warn]="!cashSufficient()">
                    <span>{{ cashSufficient() ? 'Cambio' : 'Falta' }}</span>
                    <strong>
                      {{ cashSufficient()
                          ? (cashChange() | currency:'USD':'symbol':'1.2-2')
                          : (insufficientAmount() | currency:'USD':'symbol':'1.2-2') }}
                    </strong>
                  </div>
                }
                <label class="mlabel">Referencia / comentario <span class="opt">(opcional)</span></label>
                <input [(ngModel)]="paymentReference" class="field"
                  placeholder="Referencia interna o comentario de caja" />
              </div>
            }

            <!-- ── TARJETA ── -->
            @if (paymentMethod === 'CARD') {
              <div class="pay-panel">
                <label class="mlabel">Red de tarjeta</label>
                <div class="chip-row">
                  @for (ct of cardTypes; track ct.value) {
                    <button type="button" class="chip"
                      [class.chip--on]="cardNetwork === ct.value"
                      (click)="cardNetwork = ct.value">{{ ct.label }}</button>
                  }
                </div>
                <div class="chip-row" style="margin-top:2px">
                  <button type="button" class="chip" [class.chip--on]="cardCredit" (click)="cardCredit = true">Crédito</button>
                  <button type="button" class="chip" [class.chip--on]="!cardCredit" (click)="cardCredit = false">Débito</button>
                </div>
                <label class="mlabel">No. Autorización / Voucher <span class="req">*</span></label>
                <input [(ngModel)]="cardAuth" class="field"
                  placeholder="Número de autorización del terminal POS" />
                <label class="mlabel">Banco emisor <span class="opt">(opcional)</span></label>
                <select [(ngModel)]="cardBank" class="field">
                  <option value="">Selecciona banco</option>
                  @for (b of ecuadorBanks; track b) {
                    <option [value]="b">{{ b }}</option>
                  }
                </select>
              </div>
            }

            <!-- ── TRANSFERENCIA ── -->
            @if (paymentMethod === 'TRANSFER') {
              <div class="pay-panel">
                <div class="transfer-notice">
                  La orden queda pendiente hasta confirmar la recepción bancaria desde la tabla de ventas.
                </div>
                <label class="mlabel">Banco origen <span class="req">*</span></label>
                <select [(ngModel)]="transferBank" class="field">
                  <option value="">Selecciona banco</option>
                  @for (b of ecuadorBanks; track b) {
                    <option [value]="b">{{ b }}</option>
                  }
                </select>
                <label class="mlabel">Tipo de transferencia</label>
                <div class="chip-row">
                  <button type="button" class="chip" [class.chip--on]="transferType === 'SPI'" (click)="transferType = 'SPI'">SPI</button>
                  <button type="button" class="chip" [class.chip--on]="transferType === 'ACH'" (click)="transferType = 'ACH'">ACH</button>
                  <button type="button" class="chip" [class.chip--on]="transferType === 'MANUAL'" (click)="transferType = 'MANUAL'">Manual</button>
                </div>
                <label class="mlabel">No. Comprobante <span class="req">*</span></label>
                <input [(ngModel)]="paymentReference" class="field"
                  placeholder="Número de comprobante bancario" />
              </div>
            }

            <!-- ── MIXTO ── -->
            @if (paymentMethod === 'MIXED') {
              <div class="pay-panel">
                <div class="split-progress-bar">
                  <div class="split-progress-fill" [style.width.%]="splitProgress()"></div>
                </div>
                <div class="split-balance">
                  <span>Cubierto: {{ splitTotal() | currency:'USD':'symbol':'1.2-2' }}</span>
                  <span [class.split-ok]="!splitNeedsAdjustment()" [class.split-pending]="splitNeedsAdjustment()">
                    Restante: {{ splitRemaining() | currency:'USD':'symbol':'1.2-2' }}
                  </span>
                </div>
                <div class="split-hint" [class.split-hint--ok]="!splitNeedsAdjustment()" [class.split-hint--warn]="splitNeedsAdjustment()">
                  {{ splitNeedsAdjustment() ? 'Completa o ajusta los montos para habilitar el cobro.' : 'El pago mixto está balanceado y listo para cobrarse.' }}
                </div>
                @for (entry of splitEntries; track $index) {
                  <div class="split-entry">
                    <div class="chip-row chip-row--sm">
                      <button type="button" class="chip chip--xs" [class.chip--on]="entry.method === 'CASH'" (click)="updateSplitEntry($index, 'method', 'CASH')">Efectivo</button>
                      <button type="button" class="chip chip--xs" [class.chip--on]="entry.method === 'CARD'" (click)="updateSplitEntry($index, 'method', 'CARD')">Tarjeta</button>
                      <button type="button" class="chip chip--xs" [class.chip--on]="entry.method === 'TRANSFER'" (click)="updateSplitEntry($index, 'method', 'TRANSFER')">Transfer.</button>
                    </div>
                    <span class="split-entry__meta">{{ splitReferenceLabel(entry) }}</span>
                    <div class="split-entry__fields">
                      <input type="number" min="0" step="0.01" class="field field--amount"
                        [ngModel]="entry.amount" (ngModelChange)="updateSplitEntry($index, 'amount', $event)" placeholder="$0.00" />
                      <input class="field field--ref" [ngModel]="entry.reference" (ngModelChange)="updateSplitEntry($index, 'reference', $event)" [placeholder]="splitReferencePlaceholder(entry)" />
                      @if (splitEntries.length > 1) {
                        <button type="button" class="icon-btn" (click)="removeSplitEntry($index)">x</button>
                      }
                    </div>
                  </div>
                }
                <button type="button" class="ghost ghost--xs" (click)="addSplitEntry()">+ Agregar método</button>
              </div>
            }
          </div>

          <!-- NOTAS -->
          <div class="panel-pad">
            <textarea [(ngModel)]="notes" class="field field--ta"
              placeholder="Notas para caja o para el cliente (opcional)" rows="2"></textarea>
          </div>

          <!-- BOTONES -->
          <div class="checkout-actions">
            <button type="button" class="ghost"
              [disabled]="!cart().length || isBusy() || !isRegisterOpen()"
              (click)="reserveOrder()">Reservar</button>
            <button type="button" class="btn-cobrar"
              [disabled]="!canSubmitPayment()"
              (click)="createAndPay()">
              @if (submitting()) { Procesando... }
              @else { {{ primaryActionLabel() }} }
            </button>
          </div>

        </section>
      </div>

      <!-- ── VENTAS RECIENTES ── -->
      <section class="panel panel--full">
        <div class="panel__hd panel__hd--row">
          <span class="eyebrow">Ventas recientes</span>
          <select [(ngModel)]="orderFilter" class="field field--compact"
            (ngModelChange)="loadOrders()">
            <option value="">Todas</option>
            <option value="PENDING_PAYMENT">Pendientes</option>
            <option value="PAID">Pagadas</option>
            <option value="CANCELLED">Canceladas</option>
            <option value="REFUNDED">Reembolsadas</option>
          </select>
        </div>

        @if (ordersLoading()) {
          <p class="state-msg">Consultando ventas...</p>
        } @else if (!orders().length) {
          <p class="state-msg">No hay ventas para este filtro.</p>
        } @else {
          <div class="vtable">
            <div class="vtable__head">
              <span>ID</span>
              <span>Estado</span>
              <span>Inventario</span>
              <span>Total</span>
              <span>Fecha</span>
              <span>Acciones</span>
            </div>
            @for (order of orders(); track order.id) {
              <div class="vtable__row">
                <span class="vtable__id">#{{ order.id }}</span>
                <span>
                  <span class="pill" [class]="orderStatusClass(order)">{{ orderStatusLabel(order) }}</span>
                </span>
                <span class="vtable__muted">{{ inventoryStatusLabel(order) }}</span>
                <span class="vtable__amount">{{ order.total | currency:'USD':'symbol':'1.2-2' }}</span>
                <span class="vtable__muted">{{ order.createdAt | date:'dd/MM HH:mm' }}</span>
                <div class="vtable__acts">
                  <button type="button" class="ghost ghost--xs" (click)="openOrderDetail(order)">Ver</button>
                  @if (order.transactionStatus === 'PAID') {
                    <button type="button" class="ghost ghost--xs" (click)="showReceipt(order)">Ticket</button>
                    <button type="button" class="ghost ghost--xs ghost--danger" (click)="openRefundOrder(order.id)">Reembolsar</button>
                  }
                  @if (order.transactionStatus === 'PENDING_PAYMENT') {
                    @if (order.paymentMethod === 'TRANSFER' && order.paymentStatus === 'PENDING') {
                      <button type="button" class="ghost ghost--xs" (click)="openConfirmTransfer(order.id)">Confirmar</button>
                    } @else {
                      <button type="button" class="ghost ghost--xs" (click)="payExisting(order, 'CASH')">Efectivo</button>
                      <button type="button" class="ghost ghost--xs" (click)="payExisting(order, 'CARD')">Tarjeta</button>
                    }
                    <button type="button" class="ghost ghost--xs ghost--danger" (click)="openCancelOrder(order.id)">Anular</button>
                  }
                </div>
              </div>
            }
          </div>
        }
      </section>

    </section>
  `,
  styles: `
    :host { display: contents; }

    /* ── PAGE ── */
    .page { display: grid; gap: 20px; padding: 28px 32px; align-content: start; }

    .page__header {
      display: flex; align-items: center; justify-content: space-between; gap: 16px;
    }

    .page__header h1 {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 36px; font-weight: 400; color: var(--text-strong);
      margin: 0 0 4px; letter-spacing: -0.02em; line-height: 1.1;
    }

    .page__meta { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }

    /* ── EYEBROW ── */
    .eyebrow {
      display: block; font-size: 10px; letter-spacing: 0.2em; text-transform: uppercase;
      font-weight: 500; color: var(--aurora-dim);
    }

    /* ── BADGE ── */
    .badge-warn {
      font-size: 11px; font-weight: 500; padding: 3px 10px; border-radius: 20px;
      background: var(--warn-bg); color: var(--warn); border: 1px solid var(--warn-border);
    }

    /* ── STATS ── */
    .stats-grid {
      display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px;
    }

    .stat-card {
      padding: 16px 18px; background: var(--bg-panel);
      border: 1px solid var(--line); border-radius: 6px;
    }

    .stat-card > span {
      display: block; font-size: 10px; letter-spacing: 0.18em; text-transform: uppercase;
      color: var(--text-faint); margin-bottom: 6px;
    }

    .stat-card > strong {
      display: block; font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 28px; font-weight: 400; color: var(--text-strong);
      letter-spacing: -0.02em; margin-bottom: 2px;
    }

    .stat-card > small { font-size: 11px; color: var(--text-muted); font-weight: 300; }

    .stat-card--hl { border-color: var(--aurora-border); background: var(--aurora-ghost); }
    .stat-card--hl > span { color: var(--aurora-dim); }
    .stat-card--hl > strong { color: var(--aurora); }

    /* ── LAYOUT ── */
    .pos-layout {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(340px, 360px);
      gap: 12px;
      align-items: start;
    }

    /* ── PANELS ── */
    .panel {
      background: var(--bg-panel); border: 1px solid var(--line); border-radius: 6px;
      display: flex; flex-direction: column; overflow: hidden;
      align-self: start; min-height: 0;
    }

    .panel__hd {
      display: flex; flex-direction: column; gap: 10px;
      padding: 16px 18px 14px; border-bottom: 1px solid var(--line-subtle);
    }

    .panel__hd--row { flex-direction: row; align-items: center; justify-content: space-between; }

    .panel--checkout { position: sticky; top: 0; }

    /* ── CATALOGO: producto scroll ── */
    .product-scroll {
      overflow-y: auto; max-height: 280px; padding: 14px 16px 12px;
    }

    .product-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(168px, 188px));
      gap: 8px; align-content: start;
      justify-content: start;
      align-items: stretch;
    }

    .pcard {
      display: flex; flex-direction: column; gap: 3px; text-align: left;
      border: 1px solid var(--line); border-radius: 5px; padding: 12px 13px;
      background: var(--bg-panel); cursor: pointer; width: 100%; max-width: 188px;
      transition: background 130ms ease, border-color 130ms ease, transform 120ms ease;
    }

    .pcard:hover:not(:disabled) {
      background: var(--aurora-ghost); border-color: var(--aurora-border);
      transform: translateY(-1px);
    }

    .pcard:disabled { opacity: 0.35; cursor: not-allowed; }
    .pcard--out { opacity: 0.3; }

    .pcard__cat {
      font-size: 9px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--text-faint);
    }

    .pcard__name {
      font-size: 12.5px; font-weight: 500; color: var(--text); line-height: 1.3; margin-top: 1px;
    }

    .pcard__price {
      font-size: 13.5px; font-weight: 600; color: var(--aurora); margin-top: 4px;
    }

    .pcard__stock { font-size: 10.5px; color: var(--text-faint); margin-top: 1px; }
    .pcard__stock--low { color: var(--warn); }
    .pcard__stock--out { color: var(--danger); }

    /* ── CARRITO ── */
    .cart-area {
      overflow-y: auto; max-height: 200px; padding: 10px 14px;
      display: flex; flex-direction: column; gap: 5px;
    }

    .cart-line {
      display: flex; align-items: center; justify-content: space-between; gap: 8px;
      padding: 8px 10px; background: var(--surface-muted); border: 1px solid var(--line-subtle);
      border-radius: 4px; animation: slideUp var(--dur-fast) var(--ease-out);
    }

    .cart-line__info { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
    .cart-line__info strong { font-size: 12px; color: var(--text); font-weight: 500; }
    .cart-line__info span { font-size: 11px; color: var(--text-faint); }

    .cart-line__ctrl { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }

    .cart-line__lt {
      font-size: 12px; font-weight: 600; color: var(--aurora); min-width: 58px; text-align: right;
    }

    .qty-btn {
      width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;
      border: 1px solid var(--line-strong); background: var(--bg-panel); color: var(--text);
      border-radius: 3px; font-size: 15px; cursor: pointer; padding: 0; line-height: 1;
      transition: background 100ms ease;
    }

    .qty-btn:hover:not(:disabled) { background: var(--surface-strong); }
    .qty-btn:disabled { opacity: 0.3; cursor: not-allowed; }

    .qty-input {
      width: 40px; text-align: center; background: transparent; border: none;
      border-bottom: 1px solid var(--line-strong); color: var(--text); font-size: 12px;
      outline: none; padding: 2px 0;
    }

    .icon-btn {
      width: 22px; height: 22px; display: flex; align-items: center; justify-content: center;
      background: transparent; border: none; color: var(--text-faint); font-size: 16px;
      cursor: pointer; border-radius: 3px; padding: 0; line-height: 1;
      transition: color 100ms ease, background 100ms ease;
    }

    .icon-btn:hover { color: var(--danger); background: var(--danger-bg); }

    /* ── TOTALES ── */
    .totals-block {
      padding: 10px 14px; display: flex; flex-direction: column; gap: 4px;
      border-top: 1px solid var(--line-subtle); border-bottom: 1px solid var(--line-subtle);
    }

    .totals-row {
      display: flex; justify-content: space-between; font-size: 12px; color: var(--text-muted);
    }

    .totals-row__label {
      flex: 1; min-width: 0; white-space: nowrap;
    }

    .totals-row--g {
      font-size: 15px; font-weight: 600; color: var(--text-strong);
      margin-top: 4px; padding-top: 6px; border-top: 1px solid var(--line);
    }

    .totals-row--g span:last-child { color: var(--aurora); }
    .totals-row--disc { color: var(--warn); }
    .totals-row--disc-input { align-items: center; gap: 12px; }
    .disc-global-box {
      display: grid; grid-template-columns: auto minmax(56px, 72px) minmax(78px, auto);
      align-items: center; column-gap: 8px; flex: 0 0 auto;
    }
    .disc-global-prefix {
      font-size: 12px; font-weight: 500; color: var(--warn); line-height: 1;
    }
    .disc-global-input {
      width: 100%; min-width: 0; text-align: right; background: transparent; border: none;
      border-bottom: 1px solid var(--line); color: var(--text-muted); font-size: 12px;
      outline: none; padding: 2px 0; transition: border-color 180ms ease, color 180ms ease;
    }
    .disc-global-input:focus {
      border-bottom-color: var(--warn); color: var(--warn);
    }
    .disc-global-value {
      min-width: 76px; text-align: right; color: var(--warn); font-weight: 500;
    }
    .totals-calc { font-size: 10px; color: var(--text-faint); text-align: right; }

    /* ── SECCIAN PAGOS ── */
    .doc-type-section,
    .pay-section {
      padding: 14px 14px 0;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .doc-type-section .eyebrow,
    .pay-section .eyebrow {
      padding: 0 2px 4px;
      display: block;
    }

    /* TABS */
    .pay-tabs {
      display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px;
      align-items: stretch;
    }

    .pay-tab {
      display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px;
      min-height: 64px;
      padding: 11px 8px 10px; border: 1px solid var(--line); border-radius: 6px;
      background: var(--surface-muted); color: var(--text-muted);
      font-size: 11.5px; font-weight: 400; cursor: pointer;
      transition: background 140ms ease, border-color 140ms ease, color 140ms ease;
    }

    .pay-tab span { line-height: 1.2; text-align: center; }

    .pay-tab svg { opacity: 0.6; transition: opacity 140ms ease; }

    .pay-tab:hover { background: var(--surface); color: var(--text); }
    .pay-tab:hover svg { opacity: 0.9; }

    .pay-tab--on {
      background: var(--aurora-ghost); border-color: var(--aurora-border);
      color: var(--aurora); font-weight: 500;
    }

    .pay-tab--on svg { opacity: 1; stroke: var(--aurora); }

    /* PANEL CONTENIDO PAGO */
    .pay-panel {
      display: flex; flex-direction: column; gap: 8px;
      animation: fadeIn var(--dur-fast) var(--ease-out);
    }

    /* ── QUICK BILLS ── */
    .quick-bills { display: flex; flex-wrap: wrap; gap: 5px; }

    /* ── CHANGE BOX ── */
    .change-box {
      display: flex; justify-content: space-between; align-items: center;
      padding: 9px 14px; border-radius: 5px;
      background: var(--ok-bg); border: 1px solid var(--ok-border);
      animation: scaleIn var(--dur-fast) var(--ease-out);
    }

    .change-box span { font-size: 11px; color: var(--ok); font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; }
    .change-box strong { font-family: 'Cormorant Garamond', Georgia, serif; font-size: 22px; font-weight: 400; color: var(--ok); }
    .change-box--warn { background: var(--danger-bg); border-color: var(--danger-border); }
    .change-box--warn span, .change-box--warn strong { color: var(--danger); }

    /* ── CHIPS ── */
    .chip-row { display: flex; flex-wrap: wrap; gap: 5px; }

    .chip {
      padding: 5px 13px; border: 1px solid var(--line-strong); border-radius: 20px;
      background: transparent; color: var(--text-muted); font-size: 11px; cursor: pointer;
      transition: background 110ms ease, border-color 110ms ease, color 110ms ease;
    }

    .chip:hover { background: var(--surface); color: var(--text); }

    .chip--on {
      background: var(--aurora-ghost); border-color: var(--aurora-border);
      color: var(--aurora); font-weight: 500;
    }

    /* TRANSFER NOTICE */
    .transfer-notice {
      font-size: 11.5px; color: var(--text-muted); line-height: 1.5;
      padding: 9px 12px; background: var(--info-bg);
      border: 1px solid var(--info-border); border-radius: 4px;
    }

    /* PANEL-PAD */
    .panel-pad { padding: 0 14px; }

    /* ── CHECKOUT ACTIONS ── */
    .checkout-actions {
      display: grid; grid-template-columns: 1fr 2fr; gap: 8px;
      padding: 12px 14px 14px;
    }

    .btn-cobrar {
      padding: 13px 16px; background: var(--aurora); border: none; color: #fff;
      font-size: 13px; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase;
      border-radius: 5px; cursor: pointer;
      transition: opacity 150ms ease, transform 150ms ease;
    }

    .btn-cobrar:hover:not(:disabled) { opacity: 0.87; transform: translateY(-1px); }
    .btn-cobrar:disabled { opacity: 0.33; cursor: not-allowed; transform: none; }

    /* ── VENTAS TABLE ── */
    .vtable { display: flex; flex-direction: column; gap: 3px; padding: 12px 16px 16px; }

    .vtable__head {
      display: grid;
      grid-template-columns: 52px 1.5fr 1fr 0.85fr 0.95fr 2fr;
      gap: 10px; padding: 0 10px 8px;
      font-size: 10px; text-transform: uppercase; letter-spacing: 0.16em;
      color: var(--text-faint); font-weight: 500;
    }

    .vtable__row {
      display: grid;
      grid-template-columns: 52px 1.5fr 1fr 0.85fr 0.95fr 2fr;
      gap: 10px; align-items: center; padding: 9px 10px;
      background: var(--surface-muted); border: 1px solid transparent; border-radius: 4px;
      font-size: 12.5px; color: var(--text-muted);
      transition: background 110ms ease, border-color 110ms ease;
    }

    .vtable__row:hover { background: var(--surface); border-color: var(--line-subtle); }

    .vtable__id { font-weight: 500; color: var(--text); }
    .vtable__muted { color: var(--text-faint); font-size: 11.5px; }
    .vtable__amount { font-weight: 500; color: var(--text); }
    .vtable__acts { display: flex; flex-wrap: wrap; gap: 5px; }

    /* ── PILLS ── */
    .pill {
      display: inline-block; padding: 2px 8px; border-radius: 20px;
      font-size: 10.5px; font-weight: 500; border: 1px solid transparent;
    }

    .pill--paid { background: var(--ok-bg); color: var(--ok); border-color: var(--ok-border); }
    .pill--pending { background: var(--warn-bg); color: var(--warn); border-color: var(--warn-border); }
    .pill--cancelled { background: var(--danger-bg); color: var(--danger); border-color: var(--danger-border); }
    .pill--refunded { background: var(--surface); color: var(--text-faint); border-color: var(--line); }

    /* ── BUTTONS ── */
    .btn-primary {
      padding: 9px 18px; background: transparent;
      border: 1px solid var(--aurora-border); color: var(--aurora);
      font-size: 11px; font-weight: 500; letter-spacing: 0.1em; text-transform: uppercase;
      border-radius: 4px; cursor: pointer; transition: background 140ms ease;
    }

    .btn-primary:hover:not(:disabled) { background: var(--aurora-ghost); }
    .btn-primary:disabled { opacity: 0.35; cursor: not-allowed; }

    .ghost {
      padding: 8px 14px; background: transparent; border: 1px solid var(--line-strong);
      color: var(--text-muted); font-size: 11.5px; font-weight: 400; border-radius: 4px;
      cursor: pointer; transition: border-color 110ms ease, color 110ms ease, background 110ms ease;
    }

    .ghost:hover:not(:disabled) { color: var(--text); background: var(--surface); }
    .ghost:disabled { opacity: 0.35; cursor: not-allowed; }
    .ghost--xs { padding: 4px 10px; font-size: 11px; }
    .ghost--danger { border-color: var(--danger-border); color: var(--danger); }
    .ghost--danger:hover { background: var(--danger-bg); }

    .btn-danger {
      padding: 9px 18px; background: var(--danger); border: none; color: #fff;
      font-size: 11.5px; font-weight: 500; border-radius: 4px; cursor: pointer;
      transition: opacity 140ms ease;
    }

    .btn-danger:hover:not(:disabled) { opacity: 0.84; }
    .btn-danger:disabled { opacity: 0.35; cursor: not-allowed; }

    /* ── FIELDS ── */
    .field {
      width: 100%; background: transparent; border: none;
      border-bottom: 1px solid var(--line-strong); color: var(--text);
      padding: 8px 0; font-size: 13px; font-weight: 300; outline: none;
      transition: border-color 180ms ease; box-sizing: border-box;
    }

    .field:focus { border-bottom-color: var(--aurora); }
    .field option { background: var(--bg-panel); color: var(--text); }
    .field--compact { width: auto; min-width: 150px; }
    .field--search { font-size: 12.5px; }
    .field--amount { font-size: 20px; font-weight: 400; letter-spacing: -0.01em; }

    .field--ta {
      min-height: 58px; resize: vertical; border: 1px solid var(--line-strong);
      border-radius: 4px; padding: 8px 10px; width: 100%; box-sizing: border-box;
      font-size: 12.5px; font-weight: 300; color: var(--text); background: transparent;
      outline: none; font-family: inherit; transition: border-color 180ms ease;
    }

    .field--ta:focus { border-color: var(--aurora); }

    /* ── LABELS ── */
    .mlabel {
      font-size: 10.5px; font-weight: 500; color: var(--text-muted); letter-spacing: 0.06em;
      margin-bottom: -4px;
    }

    .req { color: var(--danger); }
    .opt { color: var(--text-faint); font-weight: 300; }

    /* ── STATE ── */
    .state-msg {
      margin: 12px 16px; padding: 11px 14px; font-size: 12.5px; color: var(--text-muted);
      background: var(--surface-muted); border: 1px solid var(--line-subtle); border-radius: 4px;
    }

    .state-msg--c { text-align: center; }

    /* ── MODAL ── */
    .modal-bd {
      position: fixed; inset: 0; background: rgba(0,0,0,0.36); z-index: 1000;
      display: flex; align-items: center; justify-content: center; padding: 20px;
      animation: fadeIn var(--dur-fast) ease; backdrop-filter: blur(2px);
    }

    .modal-box {
      background: var(--bg-panel); border: 1px solid var(--line-strong);
      border-radius: 8px; padding: 28px; width: 100%; max-width: 440px;
      display: flex; flex-direction: column; gap: 14px;
      box-shadow: var(--shadow-lg);
      animation: scaleIn var(--dur-fast) var(--ease-out);
    }

    .modal__hd { display: flex; flex-direction: column; gap: 4px; }
    .modal__hd--c { align-items: center; text-align: center; }

    .modal__hd h3 {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 22px; font-weight: 400; color: var(--text-strong); margin: 0;
    }

    .modal__hint {
      font-size: 12.5px; color: var(--text-muted); line-height: 1.55; margin: 0;
      padding: 8px 12px; background: var(--surface-muted);
      border-left: 2px solid var(--aurora-border); border-radius: 0 4px 4px 0;
    }

    .modal__foot { display: flex; justify-content: flex-end; gap: 8px; margin-top: 4px; }

    /* ── RECEIPT ── */
    .rcpt__date { font-size: 12px; color: var(--text-faint); margin-top: 2px; }

    .rcpt__items {
      display: flex; flex-direction: column; gap: 5px; padding: 8px 0;
      border-top: 1px solid var(--line-subtle); border-bottom: 1px solid var(--line-subtle);
      max-height: 220px; overflow-y: auto;
    }

    .rcpt__item {
      display: grid; grid-template-columns: 1fr auto auto; gap: 12px; align-items: center;
      font-size: 12.5px;
    }

    .rcpt__iname { color: var(--text); }
    .rcpt__iqty { color: var(--text-faint); font-size: 11.5px; }
    .rcpt__iprice { font-weight: 500; color: var(--text); text-align: right; }

    .rcpt__totals { display: flex; flex-direction: column; gap: 5px; }

    .rcpt__row { display: flex; justify-content: space-between; font-size: 12.5px; color: var(--text-muted); }

    .rcpt__row--g {
      font-size: 15px; font-weight: 600; color: var(--text-strong);
      padding-top: 5px; border-top: 1px solid var(--line); margin-top: 2px;
    }

    .rcpt__ref {
      font-size: 11px; color: var(--text-faint); font-family: monospace;
      text-align: right; max-width: 200px; word-break: break-all;
    }

    .rcpt__row--ok span { color: var(--ok); font-weight: 600; }
    .rcpt__row--bad span { color: var(--danger); font-weight: 600; }

    /* ── Z-REPORT ── */
    .zrpt__section { margin-bottom: 14px; }
    .zrpt__section-title {
      font-size: 10px; font-weight: 700; letter-spacing: .08em; text-transform: uppercase;
      color: var(--text-faint); margin: 0 0 6px; padding-bottom: 4px;
      border-bottom: 1px solid var(--line);
    }
    .zrpt__notes {
      font-size: 12.5px; color: var(--text-muted); margin: 0;
      background: var(--bg-panel); border: 1px solid var(--line); border-radius: 6px;
      padding: 8px 10px;
    }

    /* ── DISCOUNT INPUT (per-line) ── */
    .disc-input {
      width: 42px; text-align: center; background: transparent; border: none;
      border-bottom: 1px solid var(--line); color: var(--text-muted); font-size: 11px;
      outline: none; padding: 2px 0; transition: border-color 180ms ease;
    }
    .disc-input:focus { border-bottom-color: var(--aurora); color: var(--aurora); }
    .disc-label { font-size: 10px; color: var(--text-faint); }

    /* global discount row */
    .discount-row { padding: 0 14px 6px; display: flex; align-items: center; gap: 6px; }
    .discount-row label { font-size: 11px; color: var(--text-muted); white-space: nowrap; }
    .discount-row .field { width: 80px; font-size: 13px; }
    .totals-row--disc { color: var(--warn); }
    .totals-row--disc-line { color: var(--text-faint); font-style: italic; font-size: 11px; }

    /* ── DOCUMENT TYPE TOGGLE ── */
    .doc-toggle {
      padding: 6px 14px; display: flex; align-items: center; gap: 10px;
      border-top: 1px solid var(--line-subtle);
    }
    .doc-toggle label { font-size: 11px; color: var(--text-muted); white-space: nowrap; }
    .doc-toggle .chip-row { flex: 1; }
    .factura-fields {
      display: flex; flex-direction: column; gap: 6px; padding: 4px 14px 8px;
      animation: fadeIn var(--dur-fast) var(--ease-out);
    }
    .factura-fields .field { font-size: 12.5px; }
    .invoice-meta {
      display: flex; flex-direction: column; gap: 6px; padding: 0 0 12px;
      margin-bottom: 12px; border-bottom: 1px solid var(--line-subtle);
    }
    .invoice-meta__row {
      display: flex; justify-content: space-between; gap: 12px; font-size: 11.5px; color: var(--text-muted);
    }
    .invoice-meta__row strong {
      color: var(--text); font-weight: 500; text-align: right;
    }

    /* ── SPLIT PAYMENT ── */
    .split-progress-bar {
      height: 6px; background: var(--surface-muted); border-radius: 3px; overflow: hidden;
    }
    .split-progress-fill {
      height: 100%; background: var(--aurora); border-radius: 3px;
      transition: width 250ms var(--ease-out);
    }
    .split-balance {
      display: flex; justify-content: space-between; font-size: 11.5px; color: var(--text-muted);
    }
    .split-ok { color: var(--ok); font-weight: 600; }
    .split-pending { color: var(--warn); font-weight: 500; }
    .split-hint {
      font-size: 11px; line-height: 1.45; color: var(--text-muted);
      padding: 8px 10px; border-radius: 4px; border: 1px solid var(--line-subtle);
      background: var(--surface-muted);
    }
    .split-hint--ok { color: var(--ok); border-color: var(--ok-border); background: var(--ok-bg); }
    .split-hint--warn { color: var(--warn); border-color: var(--warn-border); background: var(--warn-bg); }
    .split-entry {
      display: flex; flex-direction: column; gap: 6px; padding: 8px 10px;
      background: var(--surface-muted); border: 1px solid var(--line-subtle);
      border-radius: 4px; animation: slideUp var(--dur-fast) var(--ease-out);
    }
    .split-entry__meta { font-size: 10.5px; color: var(--text-faint); }
    .split-entry__fields { display: flex; gap: 6px; align-items: center; }
    .split-entry__fields .field--amount { flex: 1; font-size: 14px; }
    .split-entry__fields .field--ref { flex: 1.5; font-size: 12px; }
    .chip-row--sm { gap: 4px; }
    .chip--xs { padding: 3px 9px; font-size: 10px; }

    /* ── CASH REGISTER BAR ── */
    .register-bar {
      display: flex; align-items: center; gap: 10px; padding: 7px 14px;
      background: var(--surface-muted); border: 1px solid var(--line-subtle); border-radius: 4px;
      font-size: 11.5px; color: var(--text-muted);
    }
    .register-bar--open { border-color: var(--ok-border); background: var(--ok-bg); }
    .register-bar--closed { border-color: var(--warn-border); background: var(--warn-bg); cursor: pointer; }
    .register-bar__label { font-weight: 500; }
    .register-bar__open { color: var(--ok); }
    .register-bar__closed { color: var(--warn); }
    .register-bar__actions { margin-left: auto; display: flex; gap: 6px; }

    /* ── RESPONSIVE ── */
    @media (max-width: 1100px) {
      .page { padding: 20px 16px; }
      .stats-grid { grid-template-columns: repeat(2, 1fr); }
      .pos-layout { grid-template-columns: 1fr; }
    }

    @media (max-width: 720px) {
      .stats-grid { grid-template-columns: 1fr 1fr; }
      .vtable__head, .vtable__row { grid-template-columns: 40px 1fr 1fr; }
      .vtable__head span:nth-child(n+4), .vtable__row > *:nth-child(n+4) { display: none; }
      .checkout-actions { grid-template-columns: 1fr; }
    }

    /* ── PRINT ── */
    @media print {
      .modal-bd { position: static; background: none; backdrop-filter: none; }
      .modal-box { box-shadow: none; border: none; max-width: 100%; }
      .modal__foot { display: none; }
    }
  `,
})
export class Pos {
  private readonly erpApi = inject(ErpApi);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly destroyRef = inject(DestroyRef);

  // aa core state aa
  protected readonly products = signal<ProductDto[]>([]);
  protected readonly productsLoading = signal(false);
  protected readonly ordersLoading = signal(false);
  protected readonly totalsLoading = signal(false);
  protected readonly cart = signal<CartLine[]>([]);
  protected readonly totals = signal({ subtotal: 0, tax: 0, total: 0 });
  protected readonly orders = signal<OrderDto[]>([]);
  protected readonly submitting = signal(false);
  protected readonly feedback = signal<UiFeedback | null>(null);

  // aa modal state aa
  protected readonly activeModal = signal<ActiveModal>(null);
  protected readonly modalOrderId = signal<number | null>(null);
  protected modalReason = '';
  protected modalTransferRef = '';
  protected readonly lastReceipt = signal<OrderDto | null>(null);
  protected readonly detailOrder = signal<OrderDto | null>(null);

  // aa filters aa
  protected search = '';
  protected orderFilter = 'PENDING_PAYMENT';
  protected notes = '';

  // aa discounts aa
  protected globalDiscount = 0;

  // aa document type aa
  protected documentType: DocumentType = 'NOTA_VENTA';
  protected clientId = '';
  protected clientName = '';
  protected clientEmail = '';
  protected clientAddress = '';

  // aa cash aa
  protected cashReceived = '';
  protected paymentReference = '';

  // aa card aa
  protected cardNetwork: CardNetwork = 'VISA';
  protected cardCredit = true;
  protected cardAuth = '';
  protected cardBank = '';

  // aa transfer aa
  protected transferBank = '';
  protected transferType: 'SPI' | 'ACH' | 'MANUAL' = 'SPI';

  // aa payment method aa
  protected paymentMethod: 'CASH' | 'CARD' | 'TRANSFER' | 'MIXED' = 'CASH';

  // aa split payments aa
  protected splitEntries: SplitEntry[] = [{ method: 'CASH', amount: 0, reference: '' }];

  // aa cash register aa
  protected readonly cashRegister = signal<CashRegisterDto | null>(null);
  protected readonly registerLoading = signal(false);
  protected readonly zReportData = signal<CashRegisterDto | null>(null);
  protected registerOpeningAmount = '';
  protected registerActualCash = '';
  protected registerCloseNotes = '';

  // aa constants aa
  protected readonly ecuadorBanks = ECUADOR_BANKS;
  protected readonly cardTypes = CARD_TYPES;
  protected readonly quickBills = QUICK_BILLS;

  constructor() {
    this.reloadAll();
  }

  @HostListener('document:keydown.escape')
  onEsc(): void { if (this.activeModal()) this.closeModal(); }

  // ── COMPUTED ──────────────────────────────────────────────────────

  protected filteredProducts(): ProductDto[] {
    const term = this.search.trim().toLowerCase();
    if (!term) return this.products();
    return this.products().filter((p) =>
      `${p.name} ${p.code ?? ''}`.toLowerCase().includes(term),
    );
  }

  protected isBusy(): boolean {
    return this.submitting() || this.productsLoading() || this.ordersLoading() || this.totalsLoading();
  }

  protected cartUnits(): number {
    return this.cart().reduce((s, l) => s + l.quantity, 0);
  }

  protected pendingOrders(): number {
    return this.orders().filter((o) => o.transactionStatus === 'PENDING_PAYMENT').length;
  }

  protected cashChange(): number {
    return Math.max(0, (parseFloat(this.cashReceived) || 0) - this.finalTotal());
  }

  protected insufficientAmount(): number {
    return Math.max(0, this.finalTotal() - (parseFloat(this.cashReceived) || 0));
  }

  protected cashSufficient(): boolean {
    return (parseFloat(this.cashReceived) || 0) >= this.finalTotal();
  }

  // aa discount computed aa
  protected lineDiscountAmount(line: CartLine): number {
    return line.price * line.quantity * (line.discountPercent || 0) / 100;
  }

  protected totalLineDiscounts(): number {
    return this.cart().reduce((s, l) => s + this.lineDiscountAmount(l), 0);
  }

  protected effectiveGlobalDiscount(): number {
    const net = this.totals().subtotal - this.totalLineDiscounts();
    return Math.min(this.globalDiscount || 0, Math.max(0, net));
  }

  protected finalTotal(): number {
    const sub = this.totals().subtotal - this.totalLineDiscounts() - this.effectiveGlobalDiscount();
    const taxable = Math.max(0, sub);
    const tax = taxable * 0.15;
    return +(taxable + tax).toFixed(2);
  }

  protected finalTax(): number {
    const sub = this.totals().subtotal - this.totalLineDiscounts() - this.effectiveGlobalDiscount();
    return +(Math.max(0, sub) * 0.15).toFixed(2);
  }

  protected finalSubtotal(): number {
    return +(this.totals().subtotal - this.totalLineDiscounts() - this.effectiveGlobalDiscount()).toFixed(2);
  }

  protected updateLineDiscount(productId: number, percent: number): void {
    const p = Math.max(0, Math.min(100, Number(percent) || 0));
    this.cart.set(this.cart().map(l => l.id === productId ? { ...l, discountPercent: p } : l));
  }

  // aa split computed aa
  protected splitTotal(): number {
    return this.splitEntries.reduce((s, e) => s + (e.amount || 0), 0);
  }

  protected splitRemaining(): number {
    return +(this.finalTotal() - this.splitTotal()).toFixed(2);
  }

  protected splitNeedsAdjustment(): boolean {
    return Math.abs(this.splitRemaining()) > 0.01;
  }

  protected splitProgress(): number {
    const t = this.finalTotal();
    return t > 0 ? Math.min(100, (this.splitTotal() / t) * 100) : 0;
  }

  protected addSplitEntry(): void {
    this.splitEntries = [...this.splitEntries, { method: 'CASH', amount: 0, reference: '' }];
  }

  protected removeSplitEntry(index: number): void {
    this.splitEntries = this.splitEntries.filter((_, i) => i !== index);
  }

  protected updateSplitEntry(index: number, field: keyof SplitEntry, value: any): void {
    this.splitEntries = this.splitEntries.map((e, i) => i === index ? { ...e, [field]: value } : e);
  }

  protected splitReferenceLabel(entry: SplitEntry): string {
    if (entry.method === 'CARD') return 'Voucher o autorización del pago con tarjeta';
    if (entry.method === 'TRANSFER') return 'Comprobante o referencia bancaria';
    return 'Referencia interna del efectivo';
  }

  protected splitReferencePlaceholder(entry: SplitEntry): string {
    if (entry.method === 'CARD') return 'Voucher / auth.';
    if (entry.method === 'TRANSFER') return 'Comprobante bancario';
    return 'Referencia opcional';
  }

  protected splitEntryRequiresReference(entry: SplitEntry): boolean {
    return entry.method !== 'CASH';
  }

  // aa document type helpers aa
  protected isValidCI(): boolean {
    const id = this.clientId.trim();
    return id.length === 10 || id.length === 13;
  }

  protected canSubmitPayment(): boolean {
    if (!this.cart().length || this.isBusy() || !this.isRegisterOpen()) return false;
    if (this.documentType === 'FACTURA') {
      if (!this.clientId.trim() || !this.clientName.trim() || !this.clientAddress.trim() || !this.isValidCI()) {
        return false;
      }
    }
    if (this.paymentMethod === 'CARD') return this.cardAuth.trim().length > 0;
    if (this.paymentMethod === 'TRANSFER') {
      return this.transferBank.trim().length > 0 && this.paymentReference.trim().length > 0;
    }
    if (this.paymentMethod === 'MIXED') {
      return this.splitEntries.length > 0
        && this.splitEntries.every((entry) => entry.amount > 0 && (!this.splitEntryRequiresReference(entry) || entry.reference.trim().length > 0))
        && !this.splitNeedsAdjustment();
    }
    return true;
  }

  protected primaryActionLabel(): string {
    if (this.paymentMethod === 'TRANSFER') return 'Registrar transferencia';
    if (this.paymentMethod === 'MIXED') return 'Cobrar mixto';
    return 'Cobrar ahora';
  }

  protected orderStatusLabel(order: OrderDto): string {
    if (order.transactionStatus === 'PAID') return 'Pagada';
    if (order.transactionStatus === 'CANCELLED') return 'Cancelada';
    if (order.transactionStatus === 'REFUNDED') return 'Reembolsada';
    return order.paymentStatus === 'PENDING' ? 'Por confirmar' : 'Pendiente';
  }

  protected orderStatusClass(order: OrderDto): string {
    if (order.transactionStatus === 'PAID') return 'pill pill--paid';
    if (order.transactionStatus === 'CANCELLED') return 'pill pill--cancelled';
    if (order.transactionStatus === 'REFUNDED') return 'pill pill--refunded';
    return 'pill pill--pending';
  }

  protected inventoryStatusLabel(order: OrderDto): string {
    if (order.inventoryStatus === 'RESERVED') return 'Reservado';
    if (order.inventoryStatus === 'COMMITTED') return 'Confirmado';
    if (order.inventoryStatus === 'RELEASED') return 'Liberado';
    return '-';
  }

  protected methodLabel(method: string): string {
    if (method === 'CARD') return 'Tarjeta';
    if (method === 'TRANSFER') return 'Transferencia';
    return 'Efectivo';
  }

  protected paymentDisplayLabel(order: OrderDto): string {
    if (this.paymentBreakdown(order)?.length) return 'Mixto';
    if (order.paymentReference?.startsWith('MIXTO:')) return 'Mixto';
    return this.methodLabel(order.paymentMethod || 'CASH');
  }

  protected paymentBreakdown(order: OrderDto): SplitEntry[] | null {
    const raw = order.paymentBreakdownJson?.trim();
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed) || parsed.length === 0) return null;
      return parsed
        .filter((entry) => entry && typeof entry === 'object' && typeof entry.method === 'string')
        .map((entry) => ({
          method: entry.method,
          amount: Number(entry.amount) || 0,
          reference: String(entry.reference || ''),
        })) as SplitEntry[];
    } catch {
      return null;
    }
  }

  protected paymentBreakdownLabel(entry: SplitEntry): string {
    return this.methodLabel(entry.method);
  }

  protected receiptDocumentLabel(order: OrderDto): string {
    return this.invoiceMetaFromOrder(order) ? 'Factura electrónica' : 'Nota de venta';
  }

  protected invoiceMetaFromOrder(order: OrderDto): InvoiceMeta | null {
    if (order.documentType === 'FACTURA' && order.customerIdentification && order.customerName) {
      return {
        identification: order.customerIdentification,
        name: order.customerName,
        email: order.customerEmail || undefined,
        address: order.customerAddress || undefined,
      };
    }
    return this.invoiceMetaFromNotes(order.notes);
  }

  protected invoiceMetaFromNotes(notes?: string): InvoiceMeta | null {
    if (!notes) return null;

    const pipeMatch = notes.match(/\[FACTURA\|([^\]]+)\]/);
    if (pipeMatch?.[1]) {
      const [identification = '', name = '', email = '', address = ''] = pipeMatch[1]
        .split('|')
        .map((part) => this.decodeInvoiceToken(part));

      if (!identification || !name) return null;
      return {
        identification,
        name,
        email: email || undefined,
        address: address || undefined,
      };
    }

    const legacyMatch = notes.match(/\[FACTURA:([^:\]]*):([^:\]]*):([^:\]]*)\]/);
    if (legacyMatch) {
      return {
        identification: legacyMatch[1] || '',
        name: legacyMatch[2] || '',
        email: legacyMatch[3] || undefined,
      };
    }

    return null;
  }

  protected visibleOrderNotes(notes?: string): string | null {
    if (!notes) return null;
    const cleaned = notes
      .replace(/\[FACTURA(?:\|[^\]]+|:[^\]]+)\]/g, '')
      .replace(/\[DESC:[^\]]+\]/g, '')
      .trim();
    return cleaned || null;
  }

  protected billLabel(amount: number): string {
    return '$' + amount;
  }

  // aaaaaaaaaaaaaaaaaaaaaaaaaa CART aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

  protected addToCart(product: ProductDto): void {
    if (!this.isRegisterOpen()) {
      this.feedback.set(this.httpFeedback.warning('Abre la caja para registrar ventas.'));
      return;
    }
    const available = product.availableStock ?? product.stock ?? 0;
    if (available <= 0) {
      this.feedback.set(this.httpFeedback.warning(`"${product.name}" sin stock disponible.`));
      return;
    }
    const current = [...this.cart()];
    const existing = current.find((l) => l.id === product.id);
    if (existing) {
      existing.quantity += 1;
    } else {
      current.push({ ...product, quantity: 1, discountPercent: 0 });
    }
    this.cart.set(current);
    this.refreshTotals();
  }

  protected updateQuantity(productId: number, quantity: number): void {
    const n = Math.max(1, Number(quantity) || 1);
    this.cart.set(this.cart().map((l) => (l.id === productId ? { ...l, quantity: n } : l)));
    this.refreshTotals();
  }

  protected removeFromCart(productId: number): void {
    this.cart.set(this.cart().filter((l) => l.id !== productId));
    this.refreshTotals();
  }

  protected clearCart(): void {
    this.cart.set([]);
    this.refreshTotals();
  }

  // aaaaaaaaaaaaaaaaaaaaaaaaaa PAYMENT aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

  protected setPaymentMethod(method: 'CASH' | 'CARD' | 'TRANSFER' | 'MIXED'): void {
    this.paymentMethod = method;
    if (method === 'MIXED' && this.splitEntries.length === 0) {
      this.splitEntries = [{ method: 'CASH', amount: 0, reference: '' }];
    }
  }

  protected setCashReceived(amount: number): void {
    this.cashReceived = String(amount);
  }

  private buildPaymentReference(): string | undefined {
    if (this.paymentMethod === 'MIXED') {
      return 'MIXTO: ' + this.splitEntries.map(e => {
        const m = e.method === 'CASH' ? 'Efectivo' : e.method === 'CARD' ? 'Tarjeta' : 'Transferencia';
        return `${m} $${(e.amount || 0).toFixed(2)}` + (e.reference ? ` (${e.reference})` : '');
      }).join(' + ');
    }
    if (this.paymentMethod === 'CASH') {
      return this.paymentReference.trim() || undefined;
    }
    if (this.paymentMethod === 'CARD') {
      const parts: string[] = [this.cardNetwork, this.cardCredit ? 'CRÉDITO' : 'DÉBITO'];
      if (this.cardAuth.trim()) parts.push('AUTH:' + this.cardAuth.trim());
      if (this.cardBank) parts.push(this.cardBank);
      return parts.join(' - ');
    }
    const parts: string[] = [];
    if (this.transferBank) parts.push(this.transferBank);
    parts.push(this.transferType);
    if (this.paymentReference.trim()) parts.push('COMP:' + this.paymentReference.trim());
    return parts.join(' - ') || undefined;
  }

  private buildOrderItemsPayload(): Array<{ productId: number; quantity: number; unitPrice: number; discountAmount?: number }> {
    const lines = this.cart().map((line) => ({
      productId: line.id,
      quantity: line.quantity,
      unitPrice: line.price,
      lineDiscount: +this.lineDiscountAmount(line).toFixed(2),
      lineNet: +(line.price * line.quantity - this.lineDiscountAmount(line)).toFixed(2),
    }));

    const totalNetBeforeGlobal = lines.reduce((sum, line) => sum + line.lineNet, 0);
    let remainingGlobal = +this.effectiveGlobalDiscount().toFixed(2);

    return lines.map((line, index) => {
      let globalShare = 0;
      if (remainingGlobal > 0 && totalNetBeforeGlobal > 0) {
        if (index === lines.length - 1) {
          globalShare = remainingGlobal;
        } else {
          globalShare = +((this.effectiveGlobalDiscount() * line.lineNet) / totalNetBeforeGlobal).toFixed(2);
          globalShare = Math.min(globalShare, remainingGlobal);
          remainingGlobal = +(remainingGlobal - globalShare).toFixed(2);
        }
      }

      return {
        productId: line.productId,
        quantity: line.quantity,
        unitPrice: line.unitPrice,
        discountAmount: +(line.lineDiscount + globalShare).toFixed(2),
      };
    });
  }

  private buildNotes(): string | undefined {
    const parts: string[] = [];
    const discLines = this.totalLineDiscounts();
    const discGlobal = this.effectiveGlobalDiscount();
    if (discLines > 0 || discGlobal > 0) {
      const discParts: string[] = [];
      if (discLines > 0) discParts.push(`Desc.línea:-$${discLines.toFixed(2)}`);
      if (discGlobal > 0) discParts.push(`Desc.global:-$${discGlobal.toFixed(2)}`);
      parts.push(`[DESC:${discParts.join(',')}]`);
    }
    if (this.notes.trim()) parts.push(this.notes.trim());
    return parts.length ? parts.join(' ') : undefined;
  }

  private encodeInvoiceToken(value: string): string {
    return encodeURIComponent(value || '');
  }

  private decodeInvoiceToken(value: string): string {
    try {
      return decodeURIComponent(value || '');
    } catch {
      return value || '';
    }
  }

  // aaaaaaaaaaaaaaaaaaaaaaaaaa ORDER FLOW aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

  protected reserveOrder(): void {
    this.createOrder(false);
  }

  protected createAndPay(): void {
    this.createOrder(true);
  }

  protected payExisting(order: OrderDto, method: 'CASH' | 'CARD' | 'TRANSFER'): void {
    if (!this.isRegisterOpen()) {
      this.feedback.set(this.httpFeedback.warning('Abre la caja antes de registrar el cobro.'));
      return;
    }
    this.submitting.set(true);
    this.feedback.set(null);
    this.executePayment(order.id, method);
  }

  private resolveMainPaymentMethod(): 'CASH' | 'CARD' | 'TRANSFER' {
    if (this.paymentMethod === 'MIXED') {
      if (this.splitEntries.some((entry) => entry.method === 'TRANSFER' && entry.amount > 0)) {
        return 'TRANSFER';
      }
      const sorted = [...this.splitEntries].sort((a, b) => b.amount - a.amount);
      return sorted[0]?.method || 'CASH';
    }
    return this.paymentMethod as 'CASH' | 'CARD' | 'TRANSFER';
  }

  private createOrder(payAfterCreate: boolean): void {
    if (!this.cart().length) return;
    if (!this.isRegisterOpen()) {
      this.feedback.set(this.httpFeedback.warning('Abre la caja para registrar ventas o reservas.'));
      return;
    }
    if (payAfterCreate && !this.canSubmitPayment()) {
      this.feedback.set(this.httpFeedback.warning('Completa los campos requeridos del método de pago.'));
      return;
    }

    this.submitting.set(true);
    this.feedback.set(null);

    const mainMethod = this.resolveMainPaymentMethod();
    const isInvoice = this.documentType === 'FACTURA';
    this.erpApi
      .createOrder({
        items: this.buildOrderItemsPayload(),
        paymentMethod: mainMethod,
        notes: this.buildNotes(),
        documentType: isInvoice ? 'FACTURA' : 'NOTA_VENTA',
        customerName: isInvoice ? this.clientName.trim() || undefined : this.clientName.trim() || undefined,
        customerEmail: this.clientEmail.trim() || undefined,
        customerIdentification: isInvoice ? this.clientId.trim() || undefined : this.clientId.trim() || undefined,
        customerAddress: isInvoice ? this.clientAddress.trim() || undefined : undefined,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          if (payAfterCreate) {
            this.executePayment(order.id, mainMethod, true);
            return;
          }
          this.orderFilter = 'PENDING_PAYMENT';
          this.finishOrderFlow('Orden #' + order.id + ' reservada correctamente.');
        },
        error: (err) => {
          this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo crear la orden.'));
          this.submitting.set(false);
        },
      });
  }

  private executePayment(
    orderId: number,
    method: 'CASH' | 'CARD' | 'TRANSFER',
    clearCartAfter = false,
  ): void {
    const reference = this.buildPaymentReference();
    const paymentBreakdownJson = this.paymentMethod === 'MIXED'
      ? JSON.stringify(this.splitEntries.map((entry) => ({
          method: entry.method,
          amount: +(entry.amount || 0),
          reference: entry.reference?.trim() || '',
        })))
      : undefined;

    this.erpApi
      .payOrder(orderId, method, reference, paymentBreakdownJson)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          this.orderFilter = method === 'TRANSFER' ? 'PENDING_PAYMENT' : '';
          const suffix = method === 'TRANSFER'
            ? 'Pendiente de confirmación bancaria.'
            : 'Pago confirmado. Venta cerrada.';
          const shouldClear = clearCartAfter || method !== 'TRANSFER';
          this.finishOrderFlow('Orden #' + order.id + '. ' + suffix, shouldClear, order);
        },
        error: (err) => {
          this.feedback.set(
            this.httpFeedback.fromError(err, 'Orden creada pero no se pudo registrar el pago.'),
          );
          this.submitting.set(false);
          this.loadOrders();
        },
      });
  }

  private finishOrderFlow(message: string, clearCart = true, receipt?: OrderDto): void {
    this.feedback.set(this.httpFeedback.success(message));
    if (clearCart) {
      this.cart.set([]);
      this.notes = '';
      this.paymentReference = '';
      this.cashReceived = '';
      this.cardAuth = '';
      this.globalDiscount = 0;
      this.documentType = 'NOTA_VENTA';
      this.clientId = '';
      this.clientName = '';
      this.clientEmail = '';
      this.clientAddress = '';
      this.splitEntries = [{ method: 'CASH', amount: 0, reference: '' }];
      this.paymentMethod = 'CASH';
      this.refreshTotals();
    }
    if (receipt && receipt.transactionStatus === 'PAID') {
      this.lastReceipt.set(receipt);
      this.activeModal.set('receipt');
    }
    this.loadOrders();
    this.loadProducts();
    this.submitting.set(false);
  }

  // aaaaaaaaaaaaaaaaaaaaaaaaaa MODAL OPENERS aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

  protected openCancelOrder(orderId: number): void {
    this.modalOrderId.set(orderId);
    this.modalReason = '';
    this.activeModal.set('cancel');
  }

  protected openRefundOrder(orderId: number): void {
    this.modalOrderId.set(orderId);
    this.modalReason = '';
    this.activeModal.set('refund');
  }

  protected openConfirmTransfer(orderId: number): void {
    this.modalOrderId.set(orderId);
    this.modalTransferRef = '';
    this.activeModal.set('confirm-transfer');
  }

  protected openOrderDetail(order: OrderDto): void {
    this.detailOrder.set(order);
    this.activeModal.set('order-detail');
  }

  protected showReceipt(order: OrderDto): void {
    this.lastReceipt.set(order);
    this.activeModal.set('receipt');
  }

  protected closeModal(): void {
    this.activeModal.set(null);
  }

  protected printReceipt(): void {
    window.print();
  }

  protected printZReport(): void {
    window.print();
  }

  // aaaaaaaaaaaaaaaaaaaaaaaaaa MODAL SUBMITS aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

  protected submitCancelOrder(): void {
    const orderId = this.modalOrderId();
    if (!orderId) return;
    this.submitting.set(true);
    this.erpApi
      .cancelOrder(orderId, this.modalReason.trim() || 'Cancelación manual en POS')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.closeModal();
          this.feedback.set(this.httpFeedback.success('Orden #' + orderId + ' cancelada.'));
          this.loadOrders(); this.loadProducts(); this.submitting.set(false);
        },
        error: (err) => {
          this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo cancelar la orden.'));
          this.submitting.set(false);
        },
      });
  }

  protected submitRefundOrder(): void {
    const orderId = this.modalOrderId();
    if (!orderId) return;
    this.submitting.set(true);
    this.erpApi
      .refundOrder(orderId, this.modalReason.trim() || 'Reembolso manual en POS')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.closeModal();
          this.feedback.set(this.httpFeedback.success('Orden #' + orderId + ' reembolsada.'));
          this.loadOrders(); this.loadProducts(); this.submitting.set(false);
        },
        error: (err) => {
          this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo reembolsar la orden.'));
          this.submitting.set(false);
        },
      });
  }

  protected submitConfirmTransfer(): void {
    const orderId = this.modalOrderId();
    if (!orderId) return;
    this.submitting.set(true);
    this.erpApi
      .confirmTransfer(orderId, this.modalTransferRef.trim() || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (confirmedOrder) => {
          this.orderFilter = '';
          this.feedback.set(this.httpFeedback.success('Transferencia confirmada para orden #' + orderId + '.'));
          this.loadOrders(); this.loadProducts(); this.submitting.set(false);
          // Auto-ticket: show receipt immediately after confirming transfer
          if (confirmedOrder) {
            this.lastReceipt.set(confirmedOrder);
            this.activeModal.set('receipt');
          } else {
            this.closeModal();
          }
        },
        error: (err) => {
          this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo confirmar la transferencia.'));
          this.submitting.set(false);
        },
      });
  }

  // aaaaaaaaaaaaaaaaaaaaaaaaaa DATA LOADING aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

  protected reloadAll(): void {
    this.feedback.set(null);
    this.loadProducts();
    this.loadOrders();
    this.loadCashRegister();
    this.refreshTotals();
  }

  protected loadProducts(): void {
    this.productsLoading.set(true);
    this.erpApi
      .getPublicProducts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (products) => { this.products.set(products ?? []); this.productsLoading.set(false); },
        error: (err) => {
          this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo cargar el catálogo.'));
          this.productsLoading.set(false);
        },
      });
  }

  protected loadOrders(): void {
    this.ordersLoading.set(true);
    this.erpApi
      .listOrders(this.orderFilter || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => { this.orders.set(res.content ?? []); this.ordersLoading.set(false); },
        error: (err) => {
          this.feedback.set(this.httpFeedback.fromError(err, 'No se pudieron cargar las ventas.'));
          this.ordersLoading.set(false);
        },
      });
  }

  private refreshTotals(): void {
    if (!this.cart().length) {
      this.totals.set({ subtotal: 0, tax: 0, total: 0 });
      this.totalsLoading.set(false);
      return;
    }
    this.totalsLoading.set(true);
    this.erpApi
      .previewOrderTotals(
        this.cart().map((l) => ({ productId: l.id, quantity: l.quantity, unitPrice: l.price })),
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (t) => { this.totals.set(t); this.totalsLoading.set(false); },
        error: () => {
          const subtotal = this.cart().reduce((s, l) => s + l.price * l.quantity, 0);
          this.totals.set({ subtotal, tax: 0, total: subtotal });
          this.totalsLoading.set(false);
        },
      });
  }

  // aaaaaaaaaaaaaaaaaaaaaaaaaa CASH REGISTER aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

  protected loadCashRegister(): void {
    this.registerLoading.set(true);
    this.erpApi.getActiveCashRegister()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.cashRegister.set(res?.active ? res : null);
          this.registerLoading.set(false);
        },
        error: () => {
          this.cashRegister.set(null);
          this.registerLoading.set(false);
        },
      });
  }

  protected openRegisterModal(): void {
    this.registerOpeningAmount = '';
    this.activeModal.set('open-register');
  }

  protected closeRegisterModal(): void {
    this.registerActualCash = '';
    this.registerCloseNotes = '';
    this.activeModal.set('close-register');
  }

  protected submitOpenRegister(): void {
    const amount = parseFloat(this.registerOpeningAmount) || 0;
    this.submitting.set(true);
    this.erpApi.openCashRegister(amount)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (reg) => {
          this.cashRegister.set(reg);
          this.closeModal();
          this.feedback.set(this.httpFeedback.success('Caja abierta con $' + amount.toFixed(2)));
          this.submitting.set(false);
        },
        error: (err) => {
          this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo abrir la caja.'));
          this.submitting.set(false);
        },
      });
  }

  protected submitCloseRegister(): void {
    const reg = this.cashRegister();
    if (!reg?.id) return;
    const actualCash = parseFloat(this.registerActualCash) || 0;
    this.submitting.set(true);
    this.erpApi.closeCashRegister(reg.id, actualCash, this.registerCloseNotes || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (closed) => {
          this.cashRegister.set(null);
          this.submitting.set(false);
          this.zReportData.set(closed);
          this.activeModal.set('z-report');
        },
        error: (err) => {
          this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo cerrar la caja.'));
          this.submitting.set(false);
        },
      });
  }

  protected isRegisterOpen(): boolean {
    return this.cashRegister() !== null;
  }
}
