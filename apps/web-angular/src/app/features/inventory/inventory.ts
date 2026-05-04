import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UiFeedback } from '../../core/models/ui-feedback';
import { HttpFeedback } from '../../core/services/http-feedback';
import { FileDownloadService } from '../../core/services/file-download';
import { ExportFormat, ProductDto, StockMovementDto, ErpApi } from '../../core/services/erp-api';
import { ExportActions } from '../shared/components/export-actions';
import { Paginator } from '../shared/components/paginator';
import { RequestFeedback } from '../shared/components/request-feedback';

type ModalType = 'adjust' | 'product' | 'purchase' | null;
type PurchaseLine = { productId: number; qty: number; unitCost: number };
type CatSummary  = { cat: string; count: number; units: number; value: number };

@Component({
  selector: 'app-inventory',
  imports: [FormsModule, CurrencyPipe, DatePipe, RequestFeedback, ExportActions, Paginator],
  template: `
    <!-- ═══════════════════════════════ MODAL LAYER ═══════════════════════════════ -->

    @if (activeModal()) {
      <!-- ── PRODUCT MODAL (create / edit) ── -->
      @if (activeModal() === 'product') {
        <div class="modal-bd" (click)="closeModal()">
          <div class="modal-box modal-box--lg" (click)="$event.stopPropagation()">
            <header class="modal__hd">
              <span class="modal__eyebrow">{{ pIsEdit ? 'Editar referencia' : 'Nuevo producto' }}</span>
              <h3>{{ pIsEdit ? pName || 'Sin nombre' : 'Agregar producto al catálogo' }}</h3>
            </header>

            @if (modalFeedback(); as mf) {
              <app-request-feedback [tone]="mf.tone" [message]="mf.message" [traceId]="mf.traceId" [hideTrace]="mf.tone === 'warning'" />
            }

            <div class="pform">
              <div class="pform__field pform__field--full">
                <label class="mlabel" for="prod-name">Nombre del producto <span class="req">*</span></label>
                <input id="prod-name" class="field"
                  [class.field--err]="fieldErrors()['name']"
                  [(ngModel)]="pName"
                  [attr.aria-invalid]="!!fieldErrors()['name']"
                  [attr.aria-describedby]="fieldErrors()['name'] ? 'err-name' : null"
                  placeholder="ej. Café americano" />
                @if (fieldErrors()['name']; as e) { <small id="err-name" class="field-err">{{ e }}</small> }
              </div>
              <div class="pform__field">
                <label class="mlabel">Código / SKU</label>
                <input class="field" [(ngModel)]="pCode" placeholder="ej. CAF-001" />
              </div>
              <div class="pform__field">
                <label class="mlabel">Categoría</label>
                <input class="field" [(ngModel)]="pCategory"
                  placeholder="ej. Bebidas"
                  list="cat-datalist" />
                <datalist id="cat-datalist">
                  @for (cat of categories; track cat) {
                    <option [value]="cat"></option>
                  }
                </datalist>
              </div>
              <div class="pform__field">
                <label class="mlabel">Unidad de medida</label>
                <input class="field" [(ngModel)]="pUnit" placeholder="ej. unidad, kg, lt" list="unit-datalist" />
                <datalist id="unit-datalist">
                  <option value="unidad"></option><option value="kg"></option><option value="lt"></option>
                  <option value="docena"></option><option value="caja"></option><option value="porción"></option>
                </datalist>
              </div>
              <div class="pform__field">
                <label class="mlabel" for="prod-price">Precio de venta <span class="req">*</span></label>
                <input id="prod-price" type="number" class="field"
                  [class.field--err]="fieldErrors()['price']"
                  [(ngModel)]="pPrice" min="0" step="0.01" placeholder="0.00"
                  [attr.aria-invalid]="!!fieldErrors()['price']"
                  [attr.aria-describedby]="fieldErrors()['price'] ? 'err-price' : null" />
                @if (fieldErrors()['price']; as e) { <small id="err-price" class="field-err">{{ e }}</small> }
              </div>
              <div class="pform__field">
                <label class="mlabel" for="prod-cost">Costo de compra</label>
                <input id="prod-cost" type="number" class="field"
                  [class.field--err]="fieldErrors()['purchasePrice']"
                  [(ngModel)]="pPurchasePrice" min="0" step="0.01" placeholder="0.00" />
                @if (fieldErrors()['purchasePrice']; as e) { <small class="field-err">{{ e }}</small> }
              </div>
              <div class="pform__field">
                <label class="mlabel" for="prod-min">Stock mínimo (reorden)</label>
                <input id="prod-min" type="number" class="field"
                  [class.field--err]="fieldErrors()['minStock']"
                  [(ngModel)]="pMinStock" min="0" placeholder="0" />
                @if (fieldErrors()['minStock']; as e) { <small class="field-err">{{ e }}</small> }
              </div>
              <div class="pform__field pform__field--toggle">
                <label class="mlabel">Estado</label>
                <div class="toggle-row">
                  <button class="tog-chip" [class.tog-chip--on]="pActive" (click)="pActive = true">Activo</button>
                  <button class="tog-chip" [class.tog-chip--off]="!pActive" (click)="pActive = false">Inactivo</button>
                </div>
              </div>
            </div>

            @if (pPrice > 0 && pPurchasePrice > 0) {
              <p class="margin-preview">
                Margen estimado:
                <strong [class.col--ok]="marginFromPrices(pPrice, pPurchasePrice) > 20"
                        [class.col--warn]="marginFromPrices(pPrice, pPurchasePrice) <= 20">
                  {{ marginFromPrices(pPrice, pPurchasePrice) }}%
                </strong>
              </p>
            }

            <div class="modal__foot">
              <button class="ghost" (click)="closeModal()">Cancelar</button>
              <button class="btn-primary" [disabled]="submitting()" (click)="submitProduct()">
                {{ submitting() ? 'Guardando...' : (pIsEdit ? 'Guardar cambios' : 'Crear producto') }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- ── ADJUST MODAL (stock movement) ── -->
      @if (activeModal() === 'adjust' && modalProduct(); as p) {
        <div class="modal-bd" (click)="closeModal()">
          <div class="modal-box" (click)="$event.stopPropagation()">
            <header class="modal__hd">
              <span class="modal__eyebrow">{{ p.categoryName || 'General' }}</span>
              <h3>{{ p.name }}</h3>
              @if (p.code) { <span class="modal__code">Ref. {{ p.code }}</span> }
            </header>

            <div class="msi">
              <div class="msi__item">
                <span>Disponible</span>
                <strong [class.col--danger]="stockStatus(p) === 'critical'"
                        [class.col--warn]="stockStatus(p) === 'low'"
                        [class.col--ok]="stockStatus(p) === 'ok'">
                  {{ p.availableStock ?? p.stock ?? 0 }}
                </strong>
              </div>
              <div class="msi__item">
                <span>Reservado</span>
                <strong>{{ p.reservedStock ?? 0 }}</strong>
              </div>
              <div class="msi__item">
                <span>Total físico</span>
                <strong>{{ p.stock ?? 0 }}</strong>
              </div>
              <div class="msi__item">
                <span>Stock mínimo</span>
                <strong>{{ p.minStock ?? '—' }}</strong>
              </div>
            </div>

            @if (p.minStock && p.minStock > 0) {
              <div class="modal-bar">
                <div class="modal-bar__fill"
                  [class.modal-bar__fill--ok]="stockStatus(p) === 'ok'"
                  [class.modal-bar__fill--low]="stockStatus(p) === 'low'"
                  [class.modal-bar__fill--critical]="stockStatus(p) === 'critical'"
                  [style.width.%]="stockPct(p)"></div>
              </div>
              <p class="modal-bar__hint">
                {{ p.availableStock ?? p.stock ?? 0 }} uds. · mínimo {{ p.minStock }}
              </p>
            }

            <p class="mlabel">Tipo de movimiento</p>
            <div class="type-chips">
              <button class="type-chip" [class.type-chip--in]="modalMvtType === 'IN'"
                (click)="modalMvtType = 'IN'">Ingreso</button>
              <button class="type-chip" [class.type-chip--out]="modalMvtType === 'OUT'"
                (click)="modalMvtType = 'OUT'">Salida</button>
              <button class="type-chip" [class.type-chip--adj]="modalMvtType === 'ADJUST'"
                (click)="modalMvtType = 'ADJUST'">Ajuste</button>
            </div>

            <p class="mlabel">Cantidad</p>
            <input type="number" class="field" [(ngModel)]="modalMvtQty" min="1" placeholder="0" />

            <p class="mlabel">Motivo <span class="opt">(opcional)</span></p>
            <input class="field" [(ngModel)]="modalMvtReason" placeholder="Razón del movimiento..." />

            <div class="modal__foot">
              <button class="ghost" (click)="openProductModal(p)">Editar producto</button>
              <button class="ghost" (click)="closeModal()">Cancelar</button>
              <button class="btn-primary" [disabled]="submitting()" (click)="submitModalAdjust()">
                {{ submitting() ? 'Guardando...' : 'Aplicar movimiento' }}
              </button>
            </div>
          </div>
        </div>
      }

      <!-- ── PURCHASE MODAL (compra masiva a proveedor) ── -->
      @if (activeModal() === 'purchase') {
        <div class="modal-bd" (click)="closeModal()">
          <div class="modal-box modal-box--xl" (click)="$event.stopPropagation()">
            <header class="modal__hd">
              <span class="modal__eyebrow">Entrada de mercancía</span>
              <h3>Registrar compra a proveedor</h3>
            </header>

            <!-- Supplier info -->
            <div class="purch-info">
              <div>
                <label class="mlabel">Proveedor <span class="req">*</span></label>
                <input class="field" [(ngModel)]="purchaseSupplier" placeholder="Nombre del proveedor" />
              </div>
              <div>
                <label class="mlabel">RUC / Cédula</label>
                <input class="field" [(ngModel)]="purchaseSupplierRuc" placeholder="1712345678001" />
              </div>
              <div>
                <label class="mlabel">N° Factura / Documento</label>
                <input class="field" [(ngModel)]="purchaseDoc" placeholder="001-001-000000123" />
              </div>
              <div>
                <label class="mlabel">Vencimiento pago</label>
                <input type="date" class="field" [(ngModel)]="purchaseDueDate" />
              </div>
            </div>

            <!-- Items -->
            <div class="purch-lines-hd">
              <span>Producto</span><span>Cantidad</span><span>Costo unit.</span><span>Subtotal</span><span></span>
            </div>
            <div class="purch-lines">
              @for (line of purchaseLines; track line; let i = $index) {
                <div class="purch-line">
                  <select [(ngModel)]="line.productId" class="field">
                    <option [ngValue]="0">— Selecciona —</option>
                    @for (p of products(); track p.id) {
                      <option [ngValue]="p.id">{{ p.name }}</option>
                    }
                  </select>
                  <input type="number" [(ngModel)]="line.qty" min="1" class="field" placeholder="1" />
                  <input type="number" [(ngModel)]="line.unitCost" min="0" step="0.01" class="field" placeholder="0.00" />
                  <span class="line-subtotal">{{ (line.qty * line.unitCost) | currency:'USD':'symbol':'1.2-2' }}</span>
                  <button class="remove-line" (click)="removePurchaseLine(i)"
                    [disabled]="purchaseLines.length <= 1">×</button>
                </div>
              }
            </div>

            <div class="purch-footer-row">
              <button class="ghost" (click)="addPurchaseLine()">+ Agregar línea</button>
              <div class="purch-total">
                <span>Total compra:</span>
                <strong>{{ purchaseTotal() | currency:'USD':'symbol':'1.2-2' }}</strong>
              </div>
            </div>

            <div>
              <label class="mlabel">Notas <span class="opt">(opcional)</span></label>
              <input class="field" [(ngModel)]="purchaseNotes" placeholder="Observaciones de la compra..." />
            </div>

            <div class="modal__foot">
              <button class="ghost" (click)="closeModal()">Cancelar</button>
              <button class="btn-primary" [disabled]="submitting()" (click)="submitPurchase()">
                {{ submitting() ? 'Registrando...' : 'Registrar compra y actualizar stock' }}
              </button>
            </div>
          </div>
        </div>
      }
    }

    <!-- ═══════════════════════════════ PÁGINA ═══════════════════════════════ -->
    <section class="page">

      <!-- HEADER -->
      <header class="page__header">
        <div>
          <span class="page__eyebrow">Operación diaria</span>
          <h1>Inventario</h1>
          <p>Controla stock, movimientos y rentabilidad de cada referencia.</p>
        </div>
        <div class="page__actions">
          <small>{{ products().length }} referencias activas</small>
          <div class="hdr-btns">
            <button class="hdr-btn" [disabled]="productsLoading() || movementsLoading()" (click)="reload()">Recargar</button>
            <app-export-actions
              [disabled]="productsLoading() || movementsLoading()"
              [exporting]="exporting()"
              (exportRequested)="exportInventory($event)"
            />
            <button class="hdr-btn" (click)="openPurchaseModal()">Nueva compra</button>
            <button class="hdr-btn hdr-btn--primary" (click)="openProductModal()">+ Producto</button>
          </div>
        </div>
      </header>

      @if (feedback(); as notice) {
        <app-request-feedback [tone]="notice.tone" [message]="notice.message" [traceId]="notice.traceId" />
      }

      <!-- KPI CARDS -->
      <div class="stats-grid">
        <article class="stat-card" [class.stat-card--loading]="!productsLoaded()">
          <span>Productos activos</span>
          <strong>{{ productsLoaded() ? products().length : '—' }}</strong>
          <small>referencias en catálogo</small>
        </article>
        <article class="stat-card" [class.stat-card--warn]="productsLoaded() && lowStock().length > 0" [class.stat-card--loading]="!productsLoaded()">
          <span>Stock bajo</span>
          <strong>{{ productsLoaded() ? lowStock().length : '—' }}</strong>
          <small>{{ !productsLoaded() ? 'consultando…' : (lowStock().length === 0 ? 'sin alertas activas' : 'requieren reposición') }}</small>
        </article>
        <article class="stat-card" [class.stat-card--loading]="!productsLoaded()">
          <span>Unidades disponibles</span>
          <strong>{{ productsLoaded() ? totalAvailableUnits() : '—' }}</strong>
          <small>inventario listo para vender</small>
        </article>
        <article class="stat-card" [class.stat-card--loading]="!productsLoaded()">
          <span>Valor del inventario</span>
          <strong class="stat-card__money">{{ productsLoaded() ? (totalInventoryValue() | currency:'USD':'symbol':'1.2-2') : '—' }}</strong>
          <small>costo del stock en mano</small>
        </article>
        <article class="stat-card" [class.stat-card--loading]="!productsLoaded()">
          <span>Margen promedio</span>
          <strong [class.col--ok]="productsLoaded() && avgMargin() > 30"
                  [class.col--warn]="productsLoaded() && avgMargin() > 0 && avgMargin() <= 30">
            {{ productsLoaded() ? (avgMargin() + '%') : '—' }}
          </strong>
          <small>rentabilidad del catálogo</small>
        </article>
      </div>

      <!-- VALORIZACIÓN POR CATEGORÍA -->
      @if (categorySummary.length > 1) {
        <section class="card">
          <header class="card__hd">
            <div>
              <span class="page__eyebrow">Análisis</span>
              <h2>Valorización por categoría</h2>
            </div>
            <span class="card__count">{{ categorySummary.length }} categorías</span>
          </header>
          <div class="cat-value-grid">
            @for (s of categorySummary; track s.cat) {
              <div class="cat-value-card">
                <span class="cvc__cat">{{ s.cat }}</span>
                <strong class="cvc__val">{{ s.value | currency:'USD':'symbol':'1.2-2' }}</strong>
                <span class="cvc__meta">{{ s.count }} prods. · {{ s.units }} uds.</span>
                <div class="cvc__bar">
                  <div class="cvc__fill" [style.width.%]="catValuePct(s.value)"></div>
                </div>
              </div>
            }
          </div>
        </section>
      }

      <!-- TWO-COL: Quick-adjust + Alerts -->
      <div class="layout">

        <section class="card">
          <header class="card__hd">
            <div>
              <span class="page__eyebrow">Ajuste rápido</span>
              <h2>Registrar movimiento</h2>
            </div>
          </header>

          @if (selectedProductInPanel; as sp) {
            <div class="stock-hint">
              <div class="stock-hint__item">
                <span>Stock actual</span>
                <strong [class.col--danger]="stockStatus(sp) === 'critical'"
                        [class.col--warn]="stockStatus(sp) === 'low'">
                  {{ sp.availableStock ?? sp.stock ?? 0 }}{{ sp.unit ? ' ' + sp.unit : ' uds.' }}
                </strong>
              </div>
              <div class="stock-hint__item">
                <span>Total físico</span>
                <strong>{{ sp.stock ?? 0 }}{{ sp.unit ? ' ' + sp.unit : ' uds.' }}</strong>
              </div>
              @if (sp.minStock) {
                <div class="stock-hint__item">
                  <span>Mínimo</span>
                  <strong>{{ sp.minStock }}</strong>
                </div>
              }
              @if (sp.price && sp.purchasePrice) {
                <div class="stock-hint__item">
                  <span>Margen</span>
                  <strong [class.col--ok]="margin(sp) > 20" [class.col--warn]="margin(sp) <= 20">
                    {{ margin(sp) }}%
                  </strong>
                </div>
              }
              <span class="stock-pill"
                [class.stock-pill--ok]="stockStatus(sp) === 'ok'"
                [class.stock-pill--low]="stockStatus(sp) === 'low'"
                [class.stock-pill--critical]="stockStatus(sp) === 'critical'"
                [title]="stockStatusTooltip(sp)"
                [attr.aria-label]="stockStatusTooltip(sp)">
                {{ stockStatusLabel(sp) }}
              </span>
            </div>
          }

          @if (!selectedProductInPanel) {
            <p class="adjust-hint">Selecciona un producto para ver su stock actual y registrar el movimiento.</p>
          }

          <div class="form-grid">
            <select [(ngModel)]="selectedProductId" class="field" aria-label="Producto a ajustar">
              <option [ngValue]="0">Selecciona un producto</option>
              @for (p of products(); track p.id) {
                <option [ngValue]="p.id">{{ p.name }}</option>
              }
            </select>
            <select [(ngModel)]="movementType" class="field">
              <option value="IN">Ingreso</option>
              <option value="OUT">Salida</option>
              <option value="ADJUST">Ajuste</option>
            </select>
            <input [(ngModel)]="movementQuantity" type="number" min="1" class="field" placeholder="Cantidad" />
            <input [(ngModel)]="movementReason" class="field" placeholder="Motivo del ajuste" />
          </div>

          <button type="button" class="page__btn page__btn--full"
            [disabled]="productsLoading() || submitting()" (click)="submitAdjustment()">
            {{ submitting() ? 'Guardando...' : 'Guardar movimiento' }}
          </button>
        </section>

        <section class="card">
          <header class="card__hd">
            <div>
              <span class="page__eyebrow">Alertas</span>
              <h2>Productos que necesitan atención</h2>
            </div>
            @if (lowStock().length > 0) {
              <span class="badge-alert">{{ lowStock().length }}</span>
            }
          </header>

          @if (productsLoading()) {
            <p class="page__state">Revisando alertas de stock...</p>
          } @else if (!lowStock().length) {
            <p class="page__state">No hay alertas de inventario en este momento.</p>
          } @else {
            <div class="alert-list">
              @for (p of lowStock(); track p.id) {
                <article class="alert-item" (click)="openAdjustModal(p)">
                  <div class="alert-item__top">
                    <div class="alert-item__info">
                      <strong class="alert-item__name">{{ p.name }}</strong>
                      <span class="alert-item__cat">{{ p.categoryName || 'General' }}</span>
                    </div>
                    <div class="alert-item__right">
                      <span class="stock-pill stock-pill--critical">{{ p.availableStock ?? p.stock ?? 0 }} uds.</span>
                      <span class="alert-item__min">mín. {{ p.minStock ?? 0 }}</span>
                    </div>
                  </div>
                  <div class="alert-bar">
                    <div class="alert-bar__fill"
                      [class.alert-bar__fill--low]="stockStatus(p) === 'low'"
                      [style.width.%]="stockPct(p)"></div>
                  </div>
                </article>
              }
            </div>
          }
        </section>
      </div>

      <!-- PRODUCTS TABLE -->
      <section class="card">
        <header class="card__hd">
          <div>
            <span class="page__eyebrow">Vista general</span>
            <h2>Productos y disponibilidad</h2>
          </div>
          <span class="card__count">{{ filteredProducts.length }} / {{ products().length }} productos</span>
        </header>

          <div class="filters-row">
            <div class="search-wrap">
              <input [(ngModel)]="searchQuery" class="search-input" placeholder="Buscar por nombre o código..." />
            </div>
            <div class="cat-chips">
            <button class="cat-chip" [class.cat-chip--active]="!categoryFilter"
              (click)="categoryFilter = ''">Todos</button>
            @for (cat of categories; track cat) {
              <button class="cat-chip" [class.cat-chip--active]="categoryFilter === cat"
                (click)="categoryFilter = cat">{{ cat }}</button>
            }
            </div>
          </div>

        @if (productsLoading()) {
          <p class="page__state">Cargando catálogo...</p>
        } @else if (!filteredProducts.length) {
          <p class="page__state">Sin resultados para los filtros aplicados.</p>
        } @else {
            <div class="table">
              <div class="table__head table__head--products">
              <button class="sort-btn" [class.sort-btn--active]="sortCol === 'name'" (click)="sort('name')">
                Producto <span class="sort-icon">{{ sortIndicator('name') }}</span>
              </button>
              <button class="sort-btn" [class.sort-btn--active]="sortCol === 'categoryName'" (click)="sort('categoryName')">
                Categoría <span class="sort-icon">{{ sortIndicator('categoryName') }}</span>
              </button>
              <span>Estado</span>
              <button class="sort-btn" [class.sort-btn--active]="sortCol === 'availableStock'" (click)="sort('availableStock')">
                Disponible <span class="sort-icon">{{ sortIndicator('availableStock') }}</span>
              </button>
              <button class="sort-btn" [class.sort-btn--active]="sortCol === 'stock'" (click)="sort('stock')">
                Total <span class="sort-icon">{{ sortIndicator('stock') }}</span>
              </button>
              <button class="sort-btn" [class.sort-btn--active]="sortCol === 'price'" (click)="sort('price')">
                P. venta <span class="sort-icon">{{ sortIndicator('price') }}</span>
              </button>
              <button class="sort-btn" [class.sort-btn--active]="sortCol === 'purchasePrice'" (click)="sort('purchasePrice')">
                Costo <span class="sort-icon">{{ sortIndicator('purchasePrice') }}</span>
              </button>
              <button class="sort-btn" [class.sort-btn--active]="sortCol === 'margin'" (click)="sort('margin')">
                Margen <span class="sort-icon">{{ sortIndicator('margin') }}</span>
              </button>
                <span></span>
              </div>
              @for (p of pagedProducts; track p.id) {
                <div class="table__row table__row--products table__row--click" (click)="openAdjustModal(p)">
                <span class="prod-name">
                  {{ p.name }}
                  @if (p.code) { <span class="prod-code">{{ p.code }}</span> }
                </span>
                <span class="prod-cat">{{ p.categoryName || 'General' }}</span>
                <span>
                  <span class="stock-pill"
                    [class.stock-pill--ok]="stockStatus(p) === 'ok'"
                    [class.stock-pill--low]="stockStatus(p) === 'low'"
                    [class.stock-pill--critical]="stockStatus(p) === 'critical'"
                    [title]="stockStatusTooltip(p)"
                    [attr.aria-label]="stockStatusTooltip(p)">
                    {{ stockStatusLabel(p) }}
                  </span>
                </span>
                <span [class.col--danger]="stockStatus(p) === 'critical'"
                      [class.col--warn]="stockStatus(p) === 'low'">
                  {{ p.availableStock ?? p.stock ?? 0 }}
                </span>
                <span class="col--faint">{{ p.stock ?? 0 }}</span>
                <span>{{ p.price | currency:'USD':'symbol':'1.2-2' }}</span>
                <span class="col--faint">{{ p.purchasePrice ? (p.purchasePrice | currency:'USD':'symbol':'1.2-2') : '—' }}</span>
                <span [class.col--ok]="margin(p) > 30"
                      [class.col--warn]="margin(p) > 0 && margin(p) <= 30"
                      [class.col--faint]="margin(p) === 0">
                  {{ margin(p) > 0 ? margin(p) + '%' : '—' }}
                </span>
                <span class="row-actions" (click)="$event.stopPropagation()">
                  <button class="row-btn" title="Editar producto" (click)="openProductModal(p)">✎</button>
                </span>
                </div>
              }
            </div>
            <app-paginator
              [page]="productPage"
              [pageSize]="productPageSize"
              [totalElements]="filteredProducts.length"
              (pageChange)="productPage = $event"
            />
          }
        </section>

      <!-- MOVEMENTS TABLE -->
      <section class="card">
        <header class="card__hd">
          <div>
            <span class="page__eyebrow">Historial</span>
            <h2>Movimientos de inventario</h2>
          </div>
          <span class="card__count">{{ filteredMovements.length }} registros</span>
        </header>

        <!-- Type filter -->
        <div class="mvt-filter">
          <button class="mvt-chip" [class.mvt-chip--active]="!movementTypeFilter"
            (click)="movementTypeFilter = ''">Todos</button>
          <button class="mvt-chip" [class.mvt-chip--active]="movementTypeFilter === 'IN'"
            (click)="movementTypeFilter = 'IN'">Ingresos</button>
          <button class="mvt-chip" [class.mvt-chip--active]="movementTypeFilter === 'OUT'"
            (click)="movementTypeFilter = 'OUT'">Salidas</button>
          <button class="mvt-chip" [class.mvt-chip--active]="movementTypeFilter === 'ADJUST'"
            (click)="movementTypeFilter = 'ADJUST'">Ajustes</button>
          <button class="mvt-chip" [class.mvt-chip--active]="movementTypeFilter === 'COMMITTED'"
            (click)="movementTypeFilter = 'COMMITTED'">Comprometidos</button>
          <button class="mvt-chip" [class.mvt-chip--active]="movementTypeFilter === 'RELEASED'"
            (click)="movementTypeFilter = 'RELEASED'">Liberados</button>
        </div>

        <!-- Extended filters: product + date range -->
        <div class="history-filters">
          <div class="hf__group">
            <label class="hf__label">Producto</label>
            <select [(ngModel)]="historyProductId" class="hf__select">
              <option [ngValue]="0">Todos los productos</option>
              @for (p of products(); track p.id) {
                <option [ngValue]="p.id">{{ p.name }}</option>
              }
            </select>
          </div>
          <div class="hf__group">
            <label class="hf__label">Desde</label>
            <input type="date" [(ngModel)]="dateFrom" class="hf__date" />
          </div>
          <div class="hf__group">
            <label class="hf__label">Hasta</label>
            <input type="date" [(ngModel)]="dateTo" class="hf__date" />
          </div>
          @if (movementTypeFilter || historyProductId || dateFrom || dateTo) {
            <button class="ghost ghost--sm" (click)="clearHistoryFilters()">Limpiar filtros</button>
          }
        </div>

        @if (movementsLoading()) {
          <p class="page__state">Consultando historial...</p>
        } @else if (!filteredMovements.length) {
          <p class="page__state">No hay movimientos que coincidan con los filtros.</p>
        } @else {
            <div class="table">
            <div class="table__head table__head--movements">
              <span>Fecha</span>
              <span>Producto</span>
              <span>Tipo</span>
              <span>Cantidad</span>
              <span>Antes</span>
              <span>Después</span>
              <span>Motivo / Referencia</span>
            </div>
            @for (m of pagedMovements; track m.id) {
              <div class="table__row table__row--movements">
                <span class="col--faint">{{ m.createdAt | date:'dd/MM/yy HH:mm' }}</span>
                <span class="prod-name">{{ m.productName }}</span>
                <span>
                  <span class="mvt-badge"
                    [class.mvt-badge--in]="m.movementType === 'IN'"
                    [class.mvt-badge--out]="m.movementType === 'OUT'"
                    [class.mvt-badge--adj]="m.movementType === 'ADJUST'"
                    [class.mvt-badge--auto]="m.movementType === 'COMMITTED' || m.movementType === 'RELEASED'">
                    {{ movTypeLabel(m.movementType) }}
                  </span>
                </span>
                <span [class.col--ok]="m.movementType === 'IN'"
                      [class.col--danger]="m.movementType === 'OUT' || m.movementType === 'COMMITTED'">
                  {{ (m.movementType === 'OUT' || m.movementType === 'COMMITTED') ? '-' : '+' }}{{ m.quantity }}
                </span>
                <span class="col--faint">{{ m.stockBefore }}</span>
                <span class="col--faint">{{ m.stockAfter }}</span>
                <span class="mvt-reason">{{ m.reason || m.referenceType || '—' }}</span>
                </div>
              }
            </div>
            <app-paginator
              [page]="movementPage"
              [pageSize]="movementPageSize"
              [totalElements]="filteredMovements.length"
              (pageChange)="movementPage = $event"
            />
          }
        </section>

    </section>
  `,
  styles: `
    :host { display: contents; }

    /* ═══ PAGE ═══ */
    .page { display: grid; gap: 24px; padding: 32px; align-content: start; }

    .page__header {
      display: flex; align-items: flex-start;
      justify-content: space-between; gap: 24px;
    }
    .page__eyebrow {
      display: block; font-size: 10px; letter-spacing: .22em;
      text-transform: uppercase; font-weight: 500;
      color: var(--aurora); margin-bottom: 8px;
    }
    .page__header h1 {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 36px; font-weight: 400; color: var(--text-strong);
      margin: 0 0 4px; letter-spacing: -.02em; line-height: 1.1;
    }
    .page__header p {
      font-size: 13px; font-weight: 300; color: var(--text-muted);
      margin: 0; line-height: 1.6;
    }

    .page__actions {
      display: flex; flex-direction: column;
      align-items: flex-end; gap: 8px; flex-shrink: 0;
    }
    .page__actions small { font-size: 11px; color: var(--text-faint); }

    .hdr-btns { display: flex; gap: 8px; align-items: center; }

    .hdr-btn {
      padding: 9px 16px; font-size: 11px; font-weight: 500;
      letter-spacing: .1em; text-transform: uppercase; border-radius: 2px;
      cursor: pointer; transition: all var(--dur-fast, 140ms) ease;
      background: transparent; border: 1px solid var(--line); color: var(--text-muted);
    }
    .hdr-btn:hover:not(:disabled) { border-color: var(--aurora-border); color: var(--aurora); }
    .hdr-btn:disabled { opacity: .35; cursor: not-allowed; }
    .hdr-btn--primary {
      background: var(--aurora); border-color: var(--aurora); color: #fff;
    }
    .hdr-btn--primary:hover:not(:disabled) { opacity: .85; color: #fff; }

    .page__btn {
      padding: 9px 20px; background: transparent;
      border: 1px solid var(--aurora-border); color: var(--aurora);
      font-size: 11px; font-weight: 500; letter-spacing: .12em;
      text-transform: uppercase; border-radius: 2px; cursor: pointer;
      transition: background var(--dur-fast, 140ms) ease;
    }
    .page__btn:hover:not(:disabled) { background: var(--aurora-ghost); }
    .page__btn:disabled { opacity: .35; cursor: not-allowed; }
    .page__btn--full { width: 100%; }

    .page__state {
      margin: 0; padding: 11px 14px; border-radius: 3px;
      border: 1px solid var(--line); background: var(--surface);
      font-size: 12.5px; color: var(--text-muted);
    }

    /* Error helpers en formularios y skeletons */
    .field--err {
      border-color: var(--danger-border, #f3c2c2) !important;
      box-shadow: 0 0 0 1px var(--danger-border, #f3c2c2) !important;
    }
    .field-err {
      display: block;
      margin-top: 4px;
      font-size: 11.5px;
      color: var(--danger, #c0392b);
      line-height: 1.4;
    }
    .stat-card--loading strong {
      color: var(--text-faint);
      font-weight: 500;
      letter-spacing: 0.04em;
    }
    .stat-card--loading::after {
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(110deg, transparent 0%, rgba(255,255,255,0.18) 45%, transparent 80%);
      animation: stat-shimmer 1.6s linear infinite;
      pointer-events: none;
      border-radius: inherit;
    }
    .stat-card { position: relative; overflow: hidden; }
    @keyframes stat-shimmer {
      0%   { transform: translateX(-100%); }
      100% { transform: translateX(100%); }
    }
    .adjust-hint {
      margin: 0 0 10px;
      padding: 8px 12px;
      border-radius: 4px;
      background: var(--info-bg, #e7f0fa);
      border: 1px solid var(--info-border, #c4d8ee);
      color: var(--info, #1f4f8a);
      font-size: 12px;
    }

    /* ═══ KPI STATS ═══ */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 10px;
    }
    .stat-card {
      padding: 18px 20px; background: var(--bg-panel);
      border: 1px solid var(--line); border-radius: 4px;
      transition: border-color var(--dur-fast, 140ms) ease;
    }
    .stat-card--warn { border-color: var(--warn-border); background: var(--warn-bg); }
    .stat-card > span {
      display: block; font-size: 10px; letter-spacing: .18em;
      text-transform: uppercase; color: var(--text-faint); margin-bottom: 8px;
    }
    .stat-card > strong {
      display: block; font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 28px; font-weight: 400; color: var(--text-strong);
      letter-spacing: -.02em; margin-bottom: 3px;
    }
    .stat-card__money { font-size: 22px !important; }
    .stat-card > small { font-size: 11px; font-weight: 300; color: var(--text-muted); }

    /* ═══ CATEGORY VALUE SECTION ═══ */
    .cat-value-grid {
      display: flex; gap: 12px; overflow-x: auto;
      padding-bottom: 4px; scroll-snap-type: x mandatory;
    }
    .cat-value-card {
      flex: 0 0 180px; padding: 16px 18px;
      background: var(--surface); border: 1px solid var(--line);
      border-radius: 4px; display: grid; gap: 5px;
      scroll-snap-align: start;
    }
    .cvc__cat {
      font-size: 10px; letter-spacing: .16em; text-transform: uppercase;
      color: var(--text-faint); font-weight: 500;
    }
    .cvc__val {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 20px; font-weight: 400; color: var(--text-strong);
    }
    .cvc__meta { font-size: 11px; color: var(--text-muted); }
    .cvc__bar { height: 3px; background: var(--line); border-radius: 2px; overflow: hidden; margin-top: 4px; }
    .cvc__fill { height: 100%; background: var(--aurora); border-radius: 2px; transition: width var(--dur-slow, 420ms) ease; }

    /* ═══ LAYOUT ═══ */
    .layout {
      display: grid;
      grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.1fr);
      gap: 12px;
    }

    /* ═══ CARDS ═══ */
    .card {
      padding: 20px; background: var(--bg-panel);
      border: 1px solid var(--line); border-radius: 4px;
      display: grid; gap: 16px;
    }
    .card__hd {
      display: flex; align-items: flex-start;
      justify-content: space-between; gap: 12px;
    }
    .card h2 { font-size: 13px; font-weight: 500; color: var(--text-strong); margin: 0; }
    .card__count { font-size: 11px; color: var(--text-faint); flex-shrink: 0; align-self: flex-end; }

    .badge-alert {
      display: inline-flex; align-items: center; justify-content: center;
      min-width: 20px; height: 20px; padding: 0 5px;
      background: var(--danger); color: #fff;
      font-size: 10px; font-weight: 700; border-radius: 999px; flex-shrink: 0;
    }

    /* ═══ STOCK HINT ═══ */
    .stock-hint {
      display: flex; align-items: center; gap: 18px; flex-wrap: wrap;
      padding: 10px 14px; background: var(--surface);
      border: 1px solid var(--line); border-radius: 3px;
      animation: fadeIn var(--dur-fast, 140ms) ease;
    }
    .stock-hint__item { display: flex; flex-direction: column; gap: 2px; }
    .stock-hint__item > span {
      font-size: 10px; text-transform: uppercase; letter-spacing: .12em; color: var(--text-faint);
    }
    .stock-hint__item > strong {
      font-size: 17px; font-weight: 500; font-family: 'Cormorant Garamond', Georgia, serif;
      color: var(--text-strong);
    }

    /* ═══ FORM ═══ */
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }

    .field {
      width: 100%; background: transparent; border: none;
      border-bottom: 1px solid var(--line-strong); color: var(--text);
      padding: 9px 0; font-size: 13px; font-weight: 300; outline: none;
      transition: border-color 200ms ease; box-sizing: border-box;
    }
    .field:focus { border-bottom-color: var(--aurora); }
    .field option { background: var(--bg-panel); }

    /* ═══ STOCK PILLS ═══ */
    .stock-pill {
      display: inline-flex; align-items: center; padding: 2px 8px;
      border-radius: 2px; font-size: 10px; font-weight: 700;
      letter-spacing: .08em; text-transform: uppercase;
    }
    .stock-pill--ok       { background: var(--ok-bg);     color: var(--ok);     border: 1px solid var(--ok-border); }
    .stock-pill--low      { background: var(--warn-bg);    color: var(--warn);   border: 1px solid var(--warn-border); }
    .stock-pill--critical { background: var(--danger-bg);  color: var(--danger); border: 1px solid var(--danger-border); }

    /* ═══ ALERTS LIST ═══ */
    .alert-list { display: grid; gap: 8px; }
    .alert-item {
      padding: 12px 14px; background: var(--surface);
      border: 1px solid var(--warn-border); border-radius: 3px;
      cursor: pointer; display: grid; gap: 8px;
      transition: border-color var(--dur-fast, 140ms) ease, background var(--dur-fast, 140ms) ease;
    }
    .alert-item:hover { border-color: var(--warn); background: var(--warn-bg); }
    .alert-item__top { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
    .alert-item__info { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
    .alert-item__name { font-size: 12.5px; font-weight: 500; color: var(--text-strong); }
    .alert-item__cat  { font-size: 11px; color: var(--text-faint); }
    .alert-item__right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
    .alert-item__min  { font-size: 11px; color: var(--text-faint); white-space: nowrap; }
    .alert-bar { height: 3px; background: var(--line); border-radius: 2px; overflow: hidden; }
    .alert-bar__fill { height: 100%; border-radius: 2px; background: var(--danger); transition: width var(--dur-normal, 260ms) ease; max-width: 100%; }
    .alert-bar__fill--low { background: var(--warn); }

    /* ═══ FILTERS ═══ */
    .filters-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .search-wrap { flex: 0 0 220px; }
    .search-input {
      width: 100%; background: var(--surface); border: 1px solid var(--line);
      border-radius: 3px; padding: 7px 12px; font-size: 12.5px; color: var(--text);
      outline: none; transition: border-color var(--dur-fast, 140ms) ease; box-sizing: border-box;
    }
    .search-input:focus { border-color: var(--aurora); }
    .search-input::placeholder { color: var(--text-faint); }

    .cat-chips { display: flex; gap: 6px; flex-wrap: wrap; }
    .cat-chip {
      padding: 5px 12px; font-size: 11px; font-weight: 500;
      border: 1px solid var(--line); border-radius: 2px;
      background: transparent; color: var(--text-muted);
      cursor: pointer; transition: all var(--dur-fast, 140ms) ease; white-space: nowrap;
    }
    .cat-chip:hover { border-color: var(--aurora-border); color: var(--text); }
    .cat-chip--active { background: var(--aurora-ghost); border-color: var(--aurora); color: var(--aurora); }

    /* ═══ MOVEMENT FILTER ═══ */
    .mvt-filter { display: flex; gap: 6px; flex-wrap: wrap; }
    .mvt-chip {
      padding: 5px 12px; font-size: 11px; font-weight: 500;
      border: 1px solid var(--line); border-radius: 2px;
      background: transparent; color: var(--text-muted);
      cursor: pointer; transition: all var(--dur-fast, 140ms) ease;
    }
    .mvt-chip:hover { border-color: var(--aurora-border); color: var(--text); }
    .mvt-chip--active { background: var(--surface-strong); border-color: var(--aurora); color: var(--aurora); }

    /* ═══ HISTORY EXTENDED FILTERS ═══ */
    .history-filters {
      display: flex; align-items: flex-end; gap: 14px; flex-wrap: wrap;
      padding: 12px 14px; background: var(--surface);
      border: 1px solid var(--line); border-radius: 3px;
    }
    .hf__group { display: grid; gap: 4px; }
    .hf__label { font-size: 10px; text-transform: uppercase; letter-spacing: .12em; color: var(--text-faint); font-weight: 500; }
    .hf__select {
      background: transparent; border: none;
      border-bottom: 1px solid var(--line-strong);
      color: var(--text); font-size: 12.5px; font-weight: 300;
      padding: 6px 0; outline: none; min-width: 180px;
      transition: border-color 200ms ease;
    }
    .hf__select:focus { border-bottom-color: var(--aurora); }
    .hf__select option { background: var(--bg-panel); }
    .hf__date {
      background: transparent; border: none;
      border-bottom: 1px solid var(--line-strong);
      color: var(--text); font-size: 12.5px; padding: 6px 0; outline: none;
      transition: border-color 200ms ease;
    }
    .hf__date:focus { border-bottom-color: var(--aurora); }

    /* ═══ MOVEMENT BADGES ═══ */
    .mvt-badge {
      display: inline-flex; align-items: center; padding: 2px 8px;
      border-radius: 2px; font-size: 10px; font-weight: 700; letter-spacing: .06em; text-transform: uppercase;
    }
    .mvt-badge--in   { background: var(--ok-bg);    color: var(--ok);     }
    .mvt-badge--out  { background: var(--danger-bg); color: var(--danger); }
    .mvt-badge--adj  { background: var(--info-bg);   color: var(--info);   }
    .mvt-badge--auto { background: var(--surface);   color: var(--text-faint); border: 1px solid var(--line); }

    /* ═══ PRODUCT TABLE ═══ */
    .table { display: grid; gap: 3px; }

    .table__head--products,
    .table__row--products {
      display: grid;
      grid-template-columns: 2.2fr 1fr 0.75fr 0.75fr 0.65fr 0.75fr 0.65fr 0.65fr 0.35fr;
      gap: 10px; align-items: center; padding: 10px 12px;
    }
    .table__head--movements,
    .table__row--movements {
      display: grid;
      grid-template-columns: 1.1fr 1.5fr 1fr 0.65fr 0.6fr 0.6fr 1.8fr;
      gap: 10px; align-items: center; padding: 10px 12px;
    }

    .table__head {
      font-size: 10px; text-transform: uppercase;
      letter-spacing: .18em; font-weight: 500; color: var(--text-faint);
    }
    .table__row {
      background: var(--surface); border-radius: 3px;
      font-size: 12.5px; color: var(--text);
      transition: background var(--dur-fast, 120ms) ease;
    }
    .table__row--click { cursor: pointer; }
    .table__row--click:hover { background: var(--surface-strong); }

    .sort-btn {
      background: none; border: none; padding: 0; cursor: pointer;
      font-size: 10px; text-transform: uppercase; letter-spacing: .18em;
      font-weight: 500; color: var(--text-faint);
      display: flex; align-items: center; gap: 4px;
      transition: color var(--dur-fast, 140ms) ease; white-space: nowrap;
    }
    .sort-btn:hover { color: var(--text-muted); }
    .sort-btn--active { color: var(--aurora); }
    .sort-icon { font-size: 9px; opacity: .7; }

    .prod-name { font-weight: 500; color: var(--text-strong); display: flex; flex-direction: column; gap: 1px; }
    .prod-code { font-size: 10px; font-weight: 400; color: var(--text-faint); font-family: monospace; }
    .prod-cat  { color: var(--text-muted); font-size: 12px; }
    .mvt-reason { color: var(--text-muted); font-size: 11.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

    .row-actions { display: flex; justify-content: flex-end; }
    .row-btn {
      background: transparent; border: 1px solid var(--line); color: var(--text-faint);
      border-radius: 2px; padding: 2px 8px; font-size: 12px; cursor: pointer;
      transition: all var(--dur-fast, 140ms) ease; opacity: 0;
    }
    .table__row--click:hover .row-btn { opacity: 1; }
    .row-btn:hover { border-color: var(--aurora); color: var(--aurora); }

    /* ═══ COLOR MODIFIERS ═══ */
    .col--ok     { color: var(--ok);     font-weight: 600; }
    .col--warn   { color: var(--warn);   font-weight: 600; }
    .col--danger { color: var(--danger); font-weight: 600; }
    .col--faint  { color: var(--text-faint); }

    /* ═══ GHOST SMALL ═══ */
    .ghost {
      padding: 9px 18px; background: transparent; border: 1px solid var(--line);
      color: var(--text-muted); font-size: 12px; font-weight: 500; border-radius: 3px;
      cursor: pointer; transition: border-color var(--dur-fast, 140ms) ease, color var(--dur-fast, 140ms) ease;
    }
    .ghost:hover { border-color: var(--text-muted); color: var(--text); }
    .ghost--sm { padding: 5px 12px; font-size: 11px; }

    /* ═══ MODAL ═══ */
    .modal-bd {
      position: fixed; inset: 0; z-index: 1000;
      background: rgba(0, 0, 0, 0.45);
      display: flex; align-items: center; justify-content: center;
      padding: 24px; animation: fadeIn var(--dur-fast, 140ms) ease;
    }
    .modal-box {
      width: 100%; max-width: 460px; max-height: 92dvh; overflow-y: auto;
      background: var(--bg-panel); border: 1px solid var(--line); border-radius: 6px;
      padding: 24px; display: grid; gap: 14px;
      animation: slideUp var(--dur-normal, 260ms) ease;
    }
    .modal-box--lg  { max-width: 600px; }
    .modal-box--xl  { max-width: 720px; }

    .modal__hd { display: grid; gap: 3px; }
    .modal__eyebrow {
      font-size: 10px; letter-spacing: .2em; text-transform: uppercase;
      font-weight: 500; color: var(--aurora);
    }
    .modal__hd h3 {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 22px; font-weight: 400; color: var(--text-strong); margin: 0;
    }
    .modal__code { font-size: 11px; color: var(--text-faint); font-family: monospace; }
    .modal__foot { display: flex; justify-content: flex-end; gap: 10px; padding-top: 4px; }

    /* ═══ PRODUCT FORM ═══ */
    .pform {
      display: grid; grid-template-columns: 1fr 1fr; gap: 18px 20px;
    }
    .pform__field { display: grid; gap: 6px; }
    .pform__field--full { grid-column: 1 / -1; }
    .pform__field--toggle { display: grid; gap: 6px; }

    .toggle-row { display: flex; gap: 8px; }
    .tog-chip {
      flex: 1; padding: 8px; font-size: 11.5px; font-weight: 500;
      border: 1px solid var(--line); border-radius: 3px;
      background: transparent; color: var(--text-muted);
      cursor: pointer; transition: all var(--dur-fast, 140ms) ease;
    }
    .tog-chip--on  { background: var(--ok-bg);    border-color: var(--ok-border);     color: var(--ok);     }
    .tog-chip--off { background: var(--danger-bg); border-color: var(--danger-border);  color: var(--danger); }

    .margin-preview {
      font-size: 12.5px; color: var(--text-muted); margin: 0;
      padding: 8px 12px; background: var(--surface); border-radius: 3px;
      border: 1px solid var(--line);
    }
    .margin-preview strong { font-size: 14px; }

    /* ═══ STOCK SNAPSHOT IN ADJUST MODAL ═══ */
    .msi {
      display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px;
      padding: 14px 16px; background: var(--surface);
      border: 1px solid var(--line); border-radius: 4px;
    }
    .msi__item { display: grid; gap: 3px; }
    .msi__item > span { font-size: 10px; text-transform: uppercase; letter-spacing: .12em; color: var(--text-faint); }
    .msi__item > strong { font-size: 20px; font-family: 'Cormorant Garamond', Georgia, serif; font-weight: 400; color: var(--text-strong); }

    .modal-bar { height: 4px; background: var(--line); border-radius: 2px; overflow: hidden; }
    .modal-bar__fill { height: 100%; border-radius: 2px; background: var(--ok); transition: width var(--dur-slow, 420ms) ease; max-width: 100%; }
    .modal-bar__fill--low      { background: var(--warn); }
    .modal-bar__fill--critical { background: var(--danger); }
    .modal-bar__hint { font-size: 11px; color: var(--text-faint); margin: 0; }

    .type-chips { display: flex; gap: 8px; }
    .type-chip {
      flex: 1; padding: 9px; font-size: 12px; font-weight: 500;
      border: 1px solid var(--line); border-radius: 3px;
      background: transparent; color: var(--text-muted);
      cursor: pointer; transition: all var(--dur-fast, 140ms) ease;
    }
    .type-chip--in  { background: var(--ok-bg);     border-color: var(--ok-border);     color: var(--ok);     }
    .type-chip--out { background: var(--danger-bg);  border-color: var(--danger-border);  color: var(--danger); }
    .type-chip--adj { background: var(--info-bg);    border-color: var(--aurora-border);  color: var(--info);   }
    .type-chip:not(.type-chip--in):not(.type-chip--out):not(.type-chip--adj):hover {
      border-color: var(--aurora-border); color: var(--text);
    }

    .mlabel { font-size: 10.5px; font-weight: 500; letter-spacing: .12em; text-transform: uppercase; color: var(--text-faint); margin: 0; }
    .req { color: var(--danger); }
    .opt { font-weight: 300; text-transform: none; letter-spacing: 0; }

    /* ═══ PURCHASE MODAL ═══ */
    .purch-info {
      display: grid; grid-template-columns: 1fr 1fr; gap: 16px 20px;
    }
    .purch-lines-hd {
      display: grid; grid-template-columns: 2fr 0.7fr 0.9fr 0.8fr 0.3fr;
      gap: 8px; padding: 6px 8px;
      font-size: 10px; text-transform: uppercase; letter-spacing: .14em;
      color: var(--text-faint); font-weight: 500;
      border-bottom: 1px solid var(--line);
    }
    .purch-lines { display: grid; gap: 8px; }
    .purch-line {
      display: grid; grid-template-columns: 2fr 0.7fr 0.9fr 0.8fr 0.3fr;
      gap: 8px; align-items: center;
    }
    .line-subtotal { font-size: 13px; font-weight: 500; color: var(--text-strong); text-align: right; }
    .remove-line {
      background: transparent; border: none; color: var(--text-faint);
      font-size: 16px; cursor: pointer; padding: 0 4px; line-height: 1;
      transition: color var(--dur-fast, 140ms) ease;
    }
    .remove-line:hover:not(:disabled) { color: var(--danger); }
    .remove-line:disabled { opacity: .3; cursor: not-allowed; }

    .purch-footer-row { display: flex; align-items: center; justify-content: space-between; }
    .purch-total { display: flex; align-items: center; gap: 10px; font-size: 12.5px; color: var(--text-muted); }
    .purch-total strong { font-family: 'Cormorant Garamond', Georgia, serif; font-size: 20px; color: var(--text-strong); }

    /* ═══ BUTTONS ═══ */
    .btn-primary {
      padding: 9px 22px; background: var(--aurora); color: #fff;
      border: 1px solid var(--aurora); font-size: 12px; font-weight: 500;
      border-radius: 3px; cursor: pointer; transition: opacity var(--dur-fast, 140ms) ease;
    }
    .btn-primary:hover:not(:disabled) { opacity: .85; }
    .btn-primary:disabled { opacity: .4; cursor: not-allowed; }

    /* ═══ RESPONSIVE ═══ */
    @media (max-width: 1400px) {
      .stats-grid { grid-template-columns: repeat(3, 1fr); }
    }
    @media (max-width: 1200px) {
      .table__head--products, .table__row--products {
        grid-template-columns: 2fr 1fr 0.75fr 0.8fr 0.75fr 0.75fr 0.35fr;
      }
    }
    @media (max-width: 1100px) {
      .page { padding: 20px 16px; }
      .page__header { flex-direction: column; }
      .hdr-btns { flex-wrap: wrap; }
      .layout { grid-template-columns: 1fr; }
      .form-grid { grid-template-columns: 1fr; }
      .table__head--products, .table__row--products { grid-template-columns: 2fr 1fr 0.8fr 0.8fr 0.35fr; }
      .table__head--movements, .table__row--movements { grid-template-columns: 1fr 1.4fr 0.9fr 0.6fr 1.4fr; }
    }
    @media (max-width: 768px) {
      .stats-grid { grid-template-columns: repeat(2, 1fr); }
      .msi { grid-template-columns: repeat(2, 1fr); }
      .pform { grid-template-columns: 1fr; }
      .pform__field--full { grid-column: 1; }
      .purch-info { grid-template-columns: 1fr; }
      .purch-lines-hd, .purch-line { grid-template-columns: 1.5fr 0.5fr 0.8fr 0.7fr 0.3fr; }
      .table__head--products, .table__row--products,
      .table__head--movements, .table__row--movements { grid-template-columns: 1fr; }
      .filters-row { flex-direction: column; align-items: stretch; }
      .search-wrap { flex: none; }
    }
  `,
})
export class Inventory {
  private readonly erpApi        = inject(ErpApi);
  private readonly httpFeedback  = inject(HttpFeedback);
  private readonly fileDownload  = inject(FileDownloadService);
  private readonly destroyRef    = inject(DestroyRef);

  // ── Signals ──────────────────────────────────────────────────────────────
  protected readonly products         = signal<ProductDto[]>([]);
  protected readonly lowStock         = signal<ProductDto[]>([]);
  protected readonly movements        = signal<StockMovementDto[]>([]);
  protected readonly feedback         = signal<UiFeedback | null>(null);
  protected readonly modalFeedback    = signal<UiFeedback | null>(null);
  protected readonly productsLoading  = signal(true);
  protected readonly productsLoaded   = signal(false);
  protected readonly movementsLoading = signal(true);
  protected readonly submitting       = signal(false);
  protected readonly exporting        = signal<ExportFormat | null>(null);
  protected readonly activeModal      = signal<ModalType>(null);
  protected readonly modalProduct     = signal<ProductDto | null>(null);
  protected readonly fieldErrors      = signal<Record<string, string>>({});

  // ── Quick-adjust panel ───────────────────────────────────────────────────
  protected selectedProductId  = 0;
  protected movementType       = 'IN';
  protected movementQuantity   = 1;
  protected movementReason     = '';

  // ── Product form ─────────────────────────────────────────────────────────
  protected pName          = '';
  protected pCode          = '';
  protected pCategory      = '';
  protected pUnit          = '';
  protected pPrice         = 0;
  protected pPurchasePrice = 0;
  protected pMinStock      = 0;
  protected pActive        = true;
  protected pIsEdit        = false;
  protected pEditId        = 0;

  // ── Adjust modal ─────────────────────────────────────────────────────────
  protected modalMvtType   = 'IN';
  protected modalMvtQty    = 1;
  protected modalMvtReason = '';

  // ── Purchase form ─────────────────────────────────────────────────────────
  protected purchaseSupplier    = '';
  protected purchaseSupplierRuc = '';
  protected purchaseDoc         = '';
  protected purchaseDueDate     = '';
  protected purchaseNotes       = '';
  protected purchaseLines: PurchaseLine[] = [{ productId: 0, qty: 1, unitCost: 0 }];

  // ── Products table filters / sort ─────────────────────────────────────────
  protected searchQuery    = '';
  protected categoryFilter = '';
  protected sortCol        = '';
  protected sortDir: 'asc' | 'desc' = 'asc';
  protected productPage    = 0;
  protected readonly productPageSize = 10;

  // ── Movements table filters ───────────────────────────────────────────────
  protected movementTypeFilter = '';
  protected historyProductId   = 0;
  protected dateFrom           = '';
  protected dateTo             = '';
  protected movementPage       = 0;
  protected readonly movementPageSize = 10;

  constructor() { this.reload(); }

  // ── Derived getters (template-evaluated) ─────────────────────────────────

  protected get categories(): string[] {
    return [...new Set(this.products().map(p => p.categoryName || 'General'))].sort();
  }

  protected get filteredProducts(): ProductDto[] {
    let ps = this.products();
    if (this.categoryFilter) {
      ps = ps.filter(p => (p.categoryName || 'General') === this.categoryFilter);
    }
    const q = this.searchQuery.trim().toLowerCase();
    if (q) {
      ps = ps.filter(p => p.name.toLowerCase().includes(q) || (p.code ?? '').toLowerCase().includes(q));
    }
    if (this.sortCol) {
      ps = [...ps].sort((a, b) => {
        let aVal: string | number, bVal: string | number;
        switch (this.sortCol) {
          case 'name':           aVal = a.name;                           bVal = b.name;                           break;
          case 'categoryName':   aVal = a.categoryName ?? '';             bVal = b.categoryName ?? '';             break;
          case 'availableStock': aVal = a.availableStock ?? a.stock ?? 0; bVal = b.availableStock ?? b.stock ?? 0; break;
          case 'stock':          aVal = a.stock ?? 0;                     bVal = b.stock ?? 0;                     break;
          case 'price':          aVal = a.price;                          bVal = b.price;                          break;
          case 'purchasePrice':  aVal = a.purchasePrice ?? 0;             bVal = b.purchasePrice ?? 0;             break;
          case 'margin':         aVal = this.margin(a);                   bVal = this.margin(b);                   break;
          default:               return 0;
        }
        const cmp = typeof aVal === 'string' ? aVal.localeCompare(bVal as string) : (aVal - (bVal as number));
        return this.sortDir === 'asc' ? cmp : -cmp;
      });
    }
    return ps;
  }

  protected get productPageCount(): number {
    return Math.max(1, Math.ceil(this.filteredProducts.length / this.productPageSize));
  }

  protected get effectiveProductPage(): number {
    return Math.min(this.productPage, this.productPageCount - 1);
  }

  protected get pagedProducts(): ProductDto[] {
    const start = this.effectiveProductPage * this.productPageSize;
    return this.filteredProducts.slice(start, start + this.productPageSize);
  }

  protected get filteredMovements(): StockMovementDto[] {
    let ms = this.movements();
    if (this.movementTypeFilter) {
      ms = ms.filter(m => m.movementType === this.movementTypeFilter);
    }
    if (this.historyProductId) {
      ms = ms.filter(m => m.productId === this.historyProductId);
    }
    if (this.dateFrom) {
      ms = ms.filter(m => m.createdAt >= this.dateFrom);
    }
    if (this.dateTo) {
      ms = ms.filter(m => m.createdAt <= this.dateTo + 'T23:59:59');
    }
    return ms;
  }

  protected get movementPageCount(): number {
    return Math.max(1, Math.ceil(this.filteredMovements.length / this.movementPageSize));
  }

  protected get effectiveMovementPage(): number {
    return Math.min(this.movementPage, this.movementPageCount - 1);
  }

  protected get pagedMovements(): StockMovementDto[] {
    const start = this.effectiveMovementPage * this.movementPageSize;
    return this.filteredMovements.slice(start, start + this.movementPageSize);
  }

  protected get selectedProductInPanel(): ProductDto | undefined {
    return this.products().find(p => p.id === this.selectedProductId);
  }

  protected get categorySummary(): CatSummary[] {
    const map = new Map<string, { count: number; units: number; value: number }>();
    for (const p of this.products()) {
      const cat  = p.categoryName || 'General';
      const prev = map.get(cat) ?? { count: 0, units: 0, value: 0 };
      map.set(cat, {
        count: prev.count + 1,
        units: prev.units + (p.stock ?? 0),
        value: prev.value + ((p.purchasePrice ?? p.price) * (p.stock ?? 0)),
      });
    }
    return [...map.entries()]
      .map(([cat, d]) => ({ cat, ...d }))
      .sort((a, b) => b.value - a.value);
  }

  // ── KPI helpers ──────────────────────────────────────────────────────────

  protected totalAvailableUnits(): number {
    return this.products().reduce((s, p) => s + (p.availableStock ?? p.stock ?? 0), 0);
  }

  protected totalInventoryValue(): number {
    return this.products().reduce((s, p) => s + ((p.purchasePrice ?? p.price) * (p.stock ?? 0)), 0);
  }

  protected avgMargin(): number {
    const withCost = this.products().filter(p => p.purchasePrice && p.purchasePrice > 0);
    if (!withCost.length) return 0;
    const total = withCost.reduce((s, p) => s + this.margin(p), 0);
    return Math.round(total / withCost.length);
  }

  // ── Stock / Margin helpers ────────────────────────────────────────────────

  protected stockStatus(p: ProductDto): 'critical' | 'low' | 'ok' {
    const avail = p.availableStock ?? p.stock ?? 0;
    const min   = p.minStock ?? 0;
    if (min > 0 && avail === 0) return 'critical';
    if (p.lowStock || (min > 0 && avail < min)) return 'low';
    return 'ok';
  }

  protected stockStatusLabel(p: ProductDto): string {
    const s = this.stockStatus(p);
    return s === 'critical' ? 'CRÍTICO' : s === 'low' ? 'BAJO' : 'OK';
  }

  protected stockStatusTooltip(p: ProductDto): string {
    const s = this.stockStatus(p);
    if (s === 'critical') return 'Sin stock disponible. Reabastece con urgencia.';
    if (s === 'low') return 'Stock por debajo del mínimo de reorden.';
    return 'Stock dentro del nivel correcto.';
  }

  protected stockPct(p: ProductDto): number {
    const avail = p.availableStock ?? p.stock ?? 0;
    const min   = p.minStock;
    if (!min || min <= 0) return 100;
    return Math.min(100, Math.round((avail / min) * 100));
  }

  protected margin(p: { price: number; purchasePrice?: number }): number {
    if (!p.purchasePrice || p.purchasePrice <= 0 || p.price <= 0) return 0;
    return Math.round(((p.price - p.purchasePrice) / p.price) * 100);
  }

  protected marginFromPrices(price: number, cost: number): number {
    if (!cost || cost <= 0 || price <= 0) return 0;
    return Math.round(((price - cost) / price) * 100);
  }

  protected catValuePct(value: number): number {
    const max = Math.max(...this.categorySummary.map(s => s.value), 1);
    return Math.round((value / max) * 100);
  }

  @HostListener('document:keydown.escape')
  onEsc(): void { if (this.activeModal()) this.closeModal(); }

  // ── Sort ─────────────────────────────────────────────────────────────────

  protected sort(col: string): void {
    if (this.sortCol === col) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortCol = col;
      this.sortDir = 'asc';
    }
    this.productPage = 0;
  }

  protected sortIndicator(col: string): string {
    if (this.sortCol !== col) return '↕';
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  // ── Movement type helper ──────────────────────────────────────────────────

  protected movTypeLabel(type: string): string {
    const MAP: Record<string, string> = {
      IN: 'Ingreso', OUT: 'Salida', ADJUST: 'Ajuste',
      COMMITTED: 'Comprometido', RELEASED: 'Liberado',
    };
    return MAP[type] ?? type;
  }

  // ── Export ────────────────────────────────────────────────────────────────

  protected exportInventory(format: ExportFormat): void {
    if (this.exporting()) {
      return;
    }
    this.exporting.set(format);
    this.erpApi.exportInventory(format, {
      search: this.searchQuery.trim() || undefined,
      category: this.categoryFilter || undefined,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: response => {
          this.fileDownload.download(response, `inventario.${format}`);
          this.exporting.set(null);
        },
        error: err => {
          this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo exportar el inventario.'));
          this.exporting.set(null);
        },
      });
  }

  // ── Modal management ──────────────────────────────────────────────────────

  protected openProductModal(product?: ProductDto): void {
    this.modalFeedback.set(null);
    this.fieldErrors.set({});
    if (product) {
      this.pName          = product.name;
      this.pCode          = product.code ?? '';
      this.pCategory      = product.categoryName ?? '';
      this.pUnit          = product.unit ?? '';
      this.pPrice         = product.price;
      this.pPurchasePrice = product.purchasePrice ?? 0;
      this.pMinStock      = product.minStock ?? 0;
      this.pActive        = (product.status ?? 'ACTIVE') === 'ACTIVE';
      this.pEditId        = product.id;
      this.pIsEdit        = true;
    } else {
      this.pName = ''; this.pCode = ''; this.pCategory = ''; this.pUnit = '';
      this.pPrice = 0; this.pPurchasePrice = 0; this.pMinStock = 0;
      this.pActive = true; this.pEditId = 0; this.pIsEdit = false;
    }
    this.activeModal.set('product');
  }

  protected openAdjustModal(product: ProductDto): void {
    this.modalProduct.set(product);
    this.modalMvtType = 'IN'; this.modalMvtQty = 1; this.modalMvtReason = '';
    this.activeModal.set('adjust');
  }

  protected openPurchaseModal(): void {
    this.purchaseSupplier = ''; this.purchaseSupplierRuc = '';
    this.purchaseDoc = ''; this.purchaseDueDate = ''; this.purchaseNotes = '';
    this.purchaseLines = [{ productId: 0, qty: 1, unitCost: 0 }];
    this.activeModal.set('purchase');
  }

  protected closeModal(): void {
    this.activeModal.set(null);
    this.modalProduct.set(null);
    this.modalFeedback.set(null);
    this.fieldErrors.set({});
  }

  // ── Filters helpers ───────────────────────────────────────────────────────

  protected clearHistoryFilters(): void {
    this.movementTypeFilter = '';
    this.historyProductId   = 0;
    this.dateFrom           = '';
    this.dateTo             = '';
    this.movementPage       = 0;
  }

  // ── Purchase lines ────────────────────────────────────────────────────────

  protected addPurchaseLine(): void {
    this.purchaseLines = [...this.purchaseLines, { productId: 0, qty: 1, unitCost: 0 }];
  }

  protected removePurchaseLine(i: number): void {
    if (this.purchaseLines.length <= 1) return;
    this.purchaseLines = this.purchaseLines.filter((_, idx) => idx !== i);
  }

  protected purchaseTotal(): number {
    return this.purchaseLines.reduce((s, l) => s + (l.qty * l.unitCost), 0);
  }

  // ── Data loading ──────────────────────────────────────────────────────────

  protected reload(): void {
    this.feedback.set(null);
    this.productsLoading.set(true);
    this.movementsLoading.set(true);

    this.erpApi.getProducts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  (page)  => { this.products.set(page.content ?? []); this.productsLoaded.set(true); },
        error: (err)   => this.feedback.set(this.httpFeedback.fromError(err, 'No se pudieron cargar los productos.')),
      });

    this.erpApi.getLowStockProducts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  (list)  => { this.lowStock.set(list ?? []); this.productsLoading.set(false); this.productsLoaded.set(true); },
        error: (err)   => { this.feedback.set(this.httpFeedback.fromError(err, 'No se pudieron cargar las alertas.')); this.productsLoading.set(false); },
      });

    this.erpApi.getStockMovements()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  (list)  => { this.movements.set(list ?? []); this.movementsLoading.set(false); },
        error: (err)   => { this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo cargar el historial.')); this.movementsLoading.set(false); },
      });
  }

  // ── Form submissions ──────────────────────────────────────────────────────

  protected submitAdjustment(): void {
    this.feedback.set(null);
    if (!this.selectedProductId) {
      this.feedback.set(this.httpFeedback.warning('Selecciona un producto.'));
      return;
    }
    const qty = Number(this.movementQuantity);
    if (!qty || qty <= 0) {
      this.feedback.set(this.httpFeedback.warning('La cantidad debe ser mayor a cero.'));
      return;
    }
    this.submitting.set(true);
    this.erpApi.updateStock(this.selectedProductId, qty, this.movementType, this.movementReason.trim() || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  (p)   => { this.feedback.set(this.httpFeedback.success('Movimiento aplicado sobre ' + p.name + '.')); this.movementReason = ''; this.movementQuantity = 1; this.submitting.set(false); this.reload(); },
        error: (err) => { this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo registrar el movimiento.')); this.submitting.set(false); },
      });
  }

  protected submitModalAdjust(): void {
    const product = this.modalProduct();
    if (!product) return;
    const qty = Number(this.modalMvtQty);
    if (!qty || qty <= 0) {
      this.feedback.set(this.httpFeedback.warning('La cantidad debe ser mayor a cero.'));
      return;
    }
    this.submitting.set(true);
    this.erpApi.updateStock(product.id, qty, this.modalMvtType, this.modalMvtReason.trim() || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  (p)   => { this.feedback.set(this.httpFeedback.success('Movimiento sobre ' + p.name + ' registrado.')); this.submitting.set(false); this.closeModal(); this.reload(); },
        error: (err) => { this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo registrar el movimiento.')); this.submitting.set(false); },
      });
  }

  protected submitProduct(): void {
    const errors: Record<string, string> = {};
    if (!this.pName.trim()) {
      errors['name'] = 'Ingresa el nombre del producto.';
    }
    if (!this.pPrice || this.pPrice <= 0) {
      errors['price'] = 'El precio de venta debe ser mayor a cero.';
    }
    if (this.pPurchasePrice && this.pPurchasePrice < 0) {
      errors['purchasePrice'] = 'El costo no puede ser negativo.';
    }
    if (this.pMinStock && this.pMinStock < 0) {
      errors['minStock'] = 'El stock mínimo no puede ser negativo.';
    }
    this.fieldErrors.set(errors);
    if (Object.keys(errors).length > 0) {
      this.modalFeedback.set(this.httpFeedback.warning('Revisa los campos marcados antes de continuar.'));
      return;
    }
    this.modalFeedback.set(null);
    this.submitting.set(true);
    const payload = {
      name:          this.pName.trim(),
      code:          this.pCode.trim()     || undefined,
      price:         this.pPrice,
      purchasePrice: this.pPurchasePrice   || undefined,
      categoryName:  this.pCategory.trim() || undefined,
      unit:          this.pUnit.trim()     || undefined,
      minStock:      this.pMinStock        || undefined,
      status:        this.pActive ? 'ACTIVE' : 'INACTIVE',
    };
    const obs = this.pIsEdit
      ? this.erpApi.updateProduct(this.pEditId, payload)
      : this.erpApi.createProduct(payload);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:  (p)   => {
        this.feedback.set(this.httpFeedback.success((this.pIsEdit ? 'Producto actualizado: ' : 'Producto creado: ') + p.name));
        this.submitting.set(false);
        this.closeModal();
        this.reload();
      },
      error: (err) => {
        this.modalFeedback.set(this.httpFeedback.fromError(err, 'No se pudo guardar el producto. Revisa los datos e intenta nuevamente.'));
        this.submitting.set(false);
      },
    });
  }

  protected submitPurchase(): void {
    if (!this.purchaseSupplier.trim()) {
      this.feedback.set(this.httpFeedback.warning('El nombre del proveedor es obligatorio.'));
      return;
    }
    const validLines = this.purchaseLines.filter(l => l.productId > 0 && l.qty > 0);
    if (!validLines.length) {
      this.feedback.set(this.httpFeedback.warning('Agrega al menos un producto válido a la compra.'));
      return;
    }
    this.submitting.set(true);
    const payload = {
      supplierName:           this.purchaseSupplier.trim(),
      supplierIdentification: this.purchaseSupplierRuc.trim() || undefined,
      externalDocumentNumber: this.purchaseDoc.trim()         || undefined,
      dueDate:                this.purchaseDueDate            || undefined,
      notes:                  this.purchaseNotes.trim()       || undefined,
      items:                  validLines.map(l => ({ productId: l.productId, quantity: l.qty, unitCost: l.unitCost })),
    };
    this.erpApi.createPurchase(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (purchase) => {
          this.erpApi.receivePurchase(purchase.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next:  () => { this.feedback.set(this.httpFeedback.success('Compra de ' + this.purchaseSupplier + ' registrada. Stock actualizado.')); this.submitting.set(false); this.closeModal(); this.reload(); },
              error: (err) => { this.feedback.set(this.httpFeedback.fromError(err, 'Compra creada pero no se pudo recibir el stock.')); this.submitting.set(false); this.closeModal(); this.reload(); },
            });
        },
        error: (err) => { this.feedback.set(this.httpFeedback.fromError(err, 'No se pudo registrar la compra.')); this.submitting.set(false); },
      });
  }
}
