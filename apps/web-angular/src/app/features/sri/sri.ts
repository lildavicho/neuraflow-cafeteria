import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime } from 'rxjs';
import { ImageCroppedEvent, ImageCropperComponent } from 'ngx-image-cropper';
import { UiFeedback } from '../../core/models/ui-feedback';
import { HttpFeedback } from '../../core/services/http-feedback';
import { FileDownloadService } from '../../core/services/file-download';
import {
  AtsPeriodDto,
  RideTemplateDto,
  SequenceDto,
  SriDocumentDto,
  SriHealthSnapshotDto,
  SriTransmissionDto,
  ErpApi,
  ExportFormat,
  UpsertSequencePayload,
} from '../../core/services/erp-api';
import { RequestFeedback } from '../shared/components/request-feedback';
import { ExportActions } from '../shared/components/export-actions';
import { RideCanvas, RideCanvasData, RideCanvasLayout } from './ride-canvas';
import { sriDocTypeLabel, sriStatusLabel } from '../../core/labels';

const IVA_RATE = 0.15; // IVA Ecuador 15%

type RideLayoutState = {
  headerColor: string;
  accentColor: string;
  borderColor: string;
  fontFamily: string;
  logoUrl: string;
  footerNote: string;
};

const LEGAL_RIDE_BODY = `
  <section class="ride-schema-grid">
    <article class="ride-schema-card"><div class="ride-schema-title"><span>infoTributaria</span><strong>Datos del emisor</strong></div><table class="ride-schema-table"><tbody><tr><td>ambiente</td><th>{{legal.ambiente}}</th></tr><tr><td>tipoEmision</td><th>{{legal.tipoEmision}}</th></tr><tr><td>razonSocial</td><th>{{legal.razonSocial}}</th></tr><tr><td>nombreComercial</td><th>{{legal.nombreComercial}}</th></tr><tr><td>ruc</td><th>{{legal.ruc}}</th></tr><tr><td>codDoc</td><th>{{documento.codDoc}}</th></tr><tr><td>estab / ptoEmi</td><th>{{documento.estab}}-{{documento.ptoEmi}}</th></tr><tr><td>secuencial</td><th>{{documento.secuencial}}</th></tr><tr><td>dirMatriz</td><th>{{legal.dirMatriz}}</th></tr></tbody></table></article>
    <article class="ride-schema-card"><div class="ride-schema-title"><span>infoFactura</span><strong>Cliente y totales</strong></div><table class="ride-schema-table"><tbody><tr><td>fechaEmision</td><th>{{documento.fechaEmision}}</th></tr><tr><td>dirEstablecimiento</td><th>{{legal.dirEstablecimiento}}</th></tr><tr><td>contribuyenteEspecial</td><th>{{legal.contribuyenteEspecial}}</th></tr><tr><td>obligadoContabilidad</td><th>{{legal.obligadoContabilidad}}</th></tr><tr><td>tipoIdentificacionComprador</td><th>{{cliente.tipoIdentificacion}}</th></tr><tr><td>razonSocialComprador</td><th>{{cliente.nombre}}</th></tr><tr><td>identificacionComprador</td><th>{{cliente.identificacion}}</th></tr><tr><td>importeTotal</td><th>{{totales.total}}</th></tr></tbody></table></article>
  </section>
  <section class="ride-section"><div class="ride-section-title"><span>detalles</span><strong>Productos / servicios con impuesto por línea</strong></div><table class="ride-table ride-table--legal"><thead><tr><th>Cód. principal</th><th>Cód. auxiliar</th><th>Descripción</th><th class="number">Cant.</th><th class="number">Unitario</th><th class="number">Desc.</th><th class="number">Total sin impuesto</th><th>IVA cód.</th><th class="number">Base / valor</th></tr></thead><tbody>{{#each detalles}}<tr><td>{{codigo}}</td><td>{{codigoAuxiliar}}</td><td><strong>{{descripcion}}</strong></td><td class="number">{{cantidad}}</td><td class="number">{{precioUnitario}}</td><td class="number">{{descuento}}</td><td class="number">{{subtotal}}</td><td>{{ivaCodigo}}/{{ivaCodigoPorcentaje}} · {{ivaTarifa}}%</td><td class="number">{{ivaBase}} / {{ivaValor}}</td></tr>{{/each}}</tbody></table></section>
  <section class="ride-compliance-grid"><article class="ride-schema-card"><div class="ride-schema-title"><span>totalConImpuestos</span><strong>Resumen IVA</strong></div><table class="ride-schema-table"><tbody><tr><td>codigo</td><th>2</th></tr><tr><td>codigoPorcentaje</td><th>4</th></tr><tr><td>baseImponible</td><th>{{totales.subtotal}}</th></tr><tr><td>tarifa</td><th>15.00</th></tr><tr><td>valor</td><th>{{totales.iva15}}</th></tr></tbody></table></article><article class="ride-schema-card"><div class="ride-schema-title"><span>pagos</span><strong>Forma de pago SRI</strong></div>{{#each totales.pagos}}<table class="ride-schema-table"><tbody><tr><td>formaPago</td><th>{{formaPago}}</th></tr><tr><td>total</td><th>{{total}}</th></tr><tr><td>plazo</td><th>{{plazo}}</th></tr><tr><td>unidadTiempo</td><th>{{unidadTiempo}}</th></tr></tbody></table>{{/each}}</article><aside class="ride-totals"><table><tbody><tr><td>totalSinImpuestos</td><th>{{totales.subtotal}}</th></tr><tr><td>totalDescuento</td><th>{{totales.descuento}}</th></tr><tr class="ride-totals__tax"><td>IVA 15% código 4</td><th>{{totales.iva15}}</th></tr><tr><td>propina</td><th>{{totales.propina}}</th></tr><tr class="total-box ride-totals__grand"><td>Total a pagar</td><th>{{totales.total}}</th></tr></tbody></table></aside></section>
  <section class="ride-bottom"><div class="ride-additional"><span class="ride-label">infoAdicional</span>{{#each infoAdicional}}<p><strong>{{nombre}}:</strong> {{valor}}</p>{{/each}}</div></section>
`;

const RIDE_TEMPLATE_BODIES: Record<string, string> = {
  Classic: LEGAL_RIDE_BODY,
  Modern: LEGAL_RIDE_BODY,
  Minimal: LEGAL_RIDE_BODY,
};

@Component({
  selector: 'app-sri',
  imports: [FormsModule, CurrencyPipe, DatePipe, DecimalPipe, RequestFeedback, ExportActions, ImageCropperComponent, RideCanvas],
  template: `
    <section class="sri">
      <!-- Header -->
      <header class="sri__header">
        <div>
          <span class="sri__eyebrow">Facturación electrónica · IVA 15%</span>
          <h1>Comprobantes SRI</h1>
          <p>Gestiona emisión, validación y seguimiento de documentos electrónicos. IVA aplicado: 15%.</p>
        </div>
        <div class="sri__header-actions">
          <small class="sri__ts">{{ documents().length }} documentos cargados</small>
          <button type="button" class="sri__btn" [disabled]="loading()" (click)="reload()">
            Recargar
          </button>
          <app-export-actions
            [disabled]="loading() || !documents().length"
            [exporting]="exporting()"
            (exportRequested)="exportDocuments($event)"
          />
        </div>
      </header>

      @if (feedback(); as notice) {
        <app-request-feedback [tone]="notice.tone" [message]="notice.message" [traceId]="notice.traceId" />
      }

      <!-- IVA info banner -->
      <div class="sri__iva-banner">
        <span class="sri__iva-rate">IVA 15%</span>
        <span>Tarifa vigente Ecuador. Todos los totales mostrados incluyen IVA calculado al 15% sobre el subtotal.</span>
        @if (totalIvaCollected() > 0) {
          <span class="sri__iva-total">
            IVA acumulado autorizados: {{ totalIvaCollected() | currency: 'USD':'symbol':'1.2-2' }}
          </span>
        }
      </div>

      <section class="sri__studio">
        <div class="sri__studio-panel">
          <header class="sri__card-header">
            <div>
              <span class="sri__eyebrow">RIDE Studio</span>
              <h2>Diseño del comprobante</h2>
              <p class="sri__ride-hint">
                Auto-guarda en cada cambio. Aplica a comprobantes generados a continuación.
              </p>
            </div>
            <div class="sri__ride-actions">
              <span class="sri__ride-save-state" [attr.data-state]="rideAutoSaveState()">
                {{ rideAutoSaveLabel() }}
              </span>
              <button type="button" class="sri__btn sri__btn--ghost" [disabled]="rideSaving()" (click)="resetRideStudio()">
                Restablecer
              </button>
              <button type="button" class="sri__btn" [disabled]="rideSaving()" (click)="saveRideStudio(true)">
                Guardar
              </button>
            </div>
          </header>

          <div class="sri__template-gallery">
            @for (base of rideTemplateBases; track base.code) {
              <button
                type="button"
                class="sri__template-choice"
                [class.sri__template-choice--active]="rideBase() === base.code"
                (click)="applyRideBase(base.code)"
              >
                <strong>{{ base.name }}</strong>
                <span>{{ base.description }}</span>
              </button>
            }
          </div>

          <div class="sri__studio-controls">
            <label>
              <span>Color primario</span>
              <input class="sri__color" type="color" [ngModel]="rideLayout().headerColor" (ngModelChange)="updateRideLayout('headerColor', $event)" />
            </label>
            <label>
              <span>Acento</span>
              <input class="sri__color" type="color" [ngModel]="rideLayout().accentColor" (ngModelChange)="updateRideLayout('accentColor', $event)" />
            </label>
            <label>
              <span>Bordes</span>
              <input class="sri__color" type="color" [ngModel]="rideLayout().borderColor" (ngModelChange)="updateRideLayout('borderColor', $event)" />
            </label>
            <label>
              <span>Tipografía</span>
              <select class="sri__field" [ngModel]="rideLayout().fontFamily" (ngModelChange)="updateRideLayout('fontFamily', $event)">
                <option value="'DM Sans', system-ui, sans-serif">DM Sans</option>
                <option value="'Cormorant Garamond', Georgia, serif">Cormorant Garamond</option>
                <option value="Inter, sans-serif">Inter</option>
                <option value="Montserrat, sans-serif">Montserrat</option>
                <option value="Roboto, sans-serif">Roboto</option>
                <option value="Georgia, serif">Editorial</option>
              </select>
            </label>
            <label>
              <span>Logo</span>
              <input class="sri__field" type="file" accept="image/*" (change)="openLogoCropper($event)" />
            </label>
          </div>

          @if (logoCropperEvent()) {
            <div class="sri__logo-cropper">
              <div class="sri__cropper-head">
                <strong>Recorte del logo</strong>
                <span>Relación 3:1 para evitar estiramientos en el RIDE.</span>
              </div>
              <image-cropper
                [imageChangedEvent]="logoCropperEvent()"
                [maintainAspectRatio]="true"
                [aspectRatio]="3 / 1"
                [resizeToWidth]="500"
                [onlyScaleDown]="true"
                [format]="'png'"
                [output]="'blob'"
                [imageQuality]="0.82"
                cropperFrameAriaLabel="Recorte del logo del negocio"
                (imageCropped)="onLogoCropped($event)"
                (loadImageFailed)="logoLoadFailed()"
              />
              <div class="sri__cropper-actions">
                <button type="button" class="sri__action-btn" (click)="cancelLogoCrop()">Cancelar</button>
                <button type="button" class="sri__action-btn sri__action-btn--primary" [disabled]="!logoCroppedBlob()" (click)="uploadCroppedRideLogo()">
                  Usar logo
                </button>
              </div>
            </div>
          }

          <label class="sri__studio-footer">
            <span>Nota de pie</span>
            <input class="sri__field" [ngModel]="rideLayout().footerNote" (ngModelChange)="updateRideLayout('footerNote', $event)" />
          </label>
        </div>

        <div class="sri__preview-panel">
          <header class="sri__card-header">
            <div>
              <span class="sri__eyebrow">Vista previa</span>
              <h2>Factura en vivo</h2>
            </div>
            <button type="button" class="sri__btn sri__btn--ghost sri__expand-btn" (click)="togglePreviewExpanded(true)" title="Ver en grande">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 9V3h6M21 9V3h-6M3 15v6h6M21 15v6h-6"/>
              </svg>
              Expandir
            </button>
          </header>
          <app-ride-canvas
            [layout]="rideCanvasLayout()"
            [data]="ridePreviewData()"
            [variant]="rideBase()"
          />
        </div>
      </section>

      @if (previewExpanded()) {
        <div class="sri__preview-overlay" (click)="togglePreviewExpanded(false)" role="dialog" aria-modal="true" aria-label="Vista previa ampliada de la factura">
          <div class="sri__preview-overlay__inner" (click)="$event.stopPropagation()">
            <header class="sri__preview-overlay__head">
              <h3>Vista previa ampliada</h3>
              <button type="button" class="sri__btn sri__btn--ghost" (click)="togglePreviewExpanded(false)" aria-label="Cerrar vista previa">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
                </svg>
                Cerrar
              </button>
            </header>
            <div class="sri__preview-overlay__body">
              <app-ride-canvas
                [layout]="rideCanvasLayout()"
                [data]="ridePreviewData()"
                [variant]="rideBase()"
              />
            </div>
          </div>
        </div>
      }

      <!-- KPIs -->
      <div class="sri__kpis">
        <article class="sri__kpi">
          <span>Listos para enviar</span>
          <strong>{{ countStatus('READY_TO_SEND') }}</strong>
          <small>pendientes de emisión</small>
        </article>
        <article class="sri__kpi">
          <span>Enviados</span>
          <strong>{{ countStatus('SENT') }}</strong>
          <small>en espera de respuesta SRI</small>
        </article>
        <article class="sri__kpi sri__kpi--success">
          <span>Autorizados</span>
          <strong>{{ countStatus('AUTHORIZED') }}</strong>
          <small>documentos listos</small>
        </article>
        <article class="sri__kpi sri__kpi--warn">
          <span>Con observación</span>
          <strong>{{ countStatus('REJECTED') }}</strong>
          <small>requieren corrección</small>
        </article>
        <article class="sri__kpi">
          <span>Numeraciones</span>
          <strong>{{ sequences().length }}</strong>
          <small>series activas</small>
        </article>
      </div>

      <!-- Layout: sequences + summary -->
      <div class="sri__layout">
        <section class="sri__card">
          <header class="sri__card-header">
            <span class="sri__eyebrow">Numeración</span>
            <h2>Series activas</h2>
          </header>
          @if (!sequences().length) {
            <p class="sri__state">Aún no hay numeraciones configuradas para este negocio.</p>
          } @else {
            <div class="sri__sequence-list">
              @for (seq of sequences(); track seq.id) {
                <article class="sri__sequence-item">
                  <div class="sri__sequence-code">{{ seq.documentCode }}</div>
                  <div class="sri__sequence-meta">
                    <span>{{ seq.establishmentCode }}-{{ seq.emissionPointCode }}</span>
                    <small>Próximo: {{ seq.currentNumber + seq.incrementStep }}</small>
                  </div>
                  <span class="sri__seq-badge" [class.sri__seq-badge--active]="seq.active">
                    {{ seq.active ? 'Activa' : 'Inactiva' }}
                  </span>
                  <button type="button" class="sri__btn sri__btn--ghost" (click)="openSequenceEditor(seq)">Editar</button>
                </article>
              }
            </div>
          }

          @if (sequenceEditor(); as draft) {
            <div class="sri__modal-backdrop" (click)="closeSequenceEditor()"></div>
            <div class="sri__modal" role="dialog" aria-labelledby="seq-editor-title">
              <h3 id="seq-editor-title">Editar secuencia {{ draft.documentCode }}</h3>
              <p class="sri__modal-hint">
                Cambiar el número actual puede causar saltos o duplicados en los comprobantes. Hazlo sólo coordinado con el SRI.
              </p>
              <label class="sri__field">
                <span>Establecimiento</span>
                <input type="text" maxlength="3" [(ngModel)]="draft.establishmentCode" name="estab" placeholder="001" />
              </label>
              <label class="sri__field">
                <span>Punto de emisión</span>
                <input type="text" maxlength="3" [(ngModel)]="draft.emissionPointCode" name="emision" placeholder="001" />
              </label>
              <label class="sri__field">
                <span>Número actual</span>
                <input type="number" min="0" [(ngModel)]="draft.currentNumber" name="current" />
              </label>
              <label class="sri__field">
                <span>Incremento</span>
                <input type="number" min="1" [(ngModel)]="draft.incrementStep" name="step" />
              </label>
              <label class="sri__field sri__field--inline">
                <input type="checkbox" [(ngModel)]="draft.active" name="active" />
                <span>Secuencia activa</span>
              </label>
              <div class="sri__modal-actions">
                <button type="button" class="sri__btn sri__btn--ghost" [disabled]="sequenceSaving()" (click)="closeSequenceEditor()">Cancelar</button>
                <button type="button" class="sri__btn" [disabled]="sequenceSaving()" (click)="saveSequenceEditor()">
                  {{ sequenceSaving() ? 'Guardando…' : 'Guardar' }}
                </button>
              </div>
            </div>
          }
        </section>

        <section class="sri__card">
          <header class="sri__card-header">
            <span class="sri__eyebrow">Resumen fiscal</span>
            <h2>Totales del periodo</h2>
          </header>
          <div class="sri__fiscal-grid">
            <article class="sri__fiscal-item">
              <span>Total facturado</span>
              <strong>{{ totalBilled() | currency: 'USD':'symbol':'1.2-2' }}</strong>
            </article>
            <article class="sri__fiscal-item sri__fiscal-item--green">
              <span>Total autorizado</span>
              <strong>{{ totalAuthorized() | currency: 'USD':'symbol':'1.2-2' }}</strong>
            </article>
            <article class="sri__fiscal-item sri__fiscal-item--green">
              <span>IVA 15% autorizado</span>
              <strong>{{ totalIvaCollected() | currency: 'USD':'symbol':'1.2-2' }}</strong>
            </article>
            <article class="sri__fiscal-item">
              <span>Próxima acción</span>
              <strong>{{ nextActionLabel() }}</strong>
            </article>
          </div>
        </section>
      </div>

      <!-- ATS panel -->
      <section class="sri__card sri__ats">
        <header class="sri__card-header">
          <div>
            <span class="sri__eyebrow">Anexo Transaccional Simplificado</span>
            <h2>ATS — declaraciones mensuales</h2>
          </div>
          <div class="sri__ats-form">
            <label class="sri__field">
              <span>Año</span>
              <input
                type="number"
                min="2000"
                max="2100"
                [ngModel]="atsForm().year"
                (ngModelChange)="updateAtsForm('year', $event)"
                name="atsYear"
              />
            </label>
            <label class="sri__field">
              <span>Mes</span>
              <input
                type="number"
                min="1"
                max="12"
                [ngModel]="atsForm().month"
                (ngModelChange)="updateAtsForm('month', $event)"
                name="atsMonth"
              />
            </label>
            <button
              type="button"
              class="sri__btn"
              [disabled]="atsBusy() === 'new'"
              (click)="generateAtsPeriod()"
            >
              {{ atsBusy() === 'new' ? 'Generando…' : 'Generar / Regenerar' }}
            </button>
          </div>
        </header>

        @if (atsLoading()) {
          <p class="sri__state">Cargando periodos…</p>
        } @else if (!atsPeriods().length) {
          <p class="sri__state">Aún no se ha generado ningún periodo ATS para este negocio.</p>
        } @else {
          <div class="sri__ats-list">
            @for (period of atsPeriods(); track period.id) {
              <article class="sri__ats-item">
                <header class="sri__ats-item-head">
                  <strong>{{ period.fiscalYear }}-{{ period.fiscalMonth | number: '2.0-0' }}</strong>
                  <span class="sri__ats-status sri__ats-status--{{ period.status.toLowerCase() }}">
                    {{ period.status }}
                  </span>
                </header>
                <dl class="sri__ats-stats">
                  <div><dt>Ventas</dt><dd>{{ period.salesCount }} · {{ period.totalSales | currency: 'USD':'symbol':'1.2-2' }}</dd></div>
                  <div><dt>Compras</dt><dd>{{ period.purchasesCount }} · {{ period.totalPurchases | currency: 'USD':'symbol':'1.2-2' }}</dd></div>
                  <div><dt>Retenciones</dt><dd>{{ period.withholdingsCount }} · {{ period.totalWithheld | currency: 'USD':'symbol':'1.2-2' }}</dd></div>
                  @if (period.generatedAt) {
                    <div><dt>Generado</dt><dd>{{ period.generatedAt | date: 'short' }}</dd></div>
                  }
                  @if (period.submittedAt) {
                    <div><dt>Enviado</dt><dd>{{ period.submittedAt | date: 'short' }}</dd></div>
                  }
                </dl>
                <div class="sri__ats-actions">
                  <button
                    type="button"
                    class="sri__btn sri__btn--ghost"
                    [disabled]="!period.hasXml"
                    (click)="downloadAtsXml(period)"
                  >Descargar XML</button>
                  @if (period.status === 'GENERATED') {
                    <button
                      type="button"
                      class="sri__btn"
                      [disabled]="atsBusy() === period.id"
                      (click)="markAtsSubmitted(period)"
                    >Marcar como enviado</button>
                  }
                  @if (period.status === 'SUBMITTED') {
                    <button
                      type="button"
                      class="sri__btn"
                      [disabled]="atsBusy() === period.id"
                      (click)="closeAtsPeriod(period)"
                    >Cerrar periodo</button>
                  }
                </div>
              </article>
            }
          </div>
        }
      </section>

      <!-- Health monitor -->
      <section class="sri__card sri__health">
        <header class="sri__card-header">
          <div>
            <span class="sri__eyebrow">Operación SRI</span>
            <h2>Salud y reaper</h2>
          </div>
          <div class="sri__health-actions">
            <button
              type="button"
              class="sri__btn sri__btn--ghost"
              [disabled]="healthLoading()"
              (click)="loadSriHealth()"
            >{{ healthLoading() ? 'Actualizando…' : 'Actualizar' }}</button>
            <button
              type="button"
              class="sri__btn"
              [disabled]="healthReaping()"
              (click)="reapSriHealth()"
            >{{ healthReaping() ? 'Procesando…' : 'Ejecutar reaper' }}</button>
          </div>
        </header>

        @if (healthLoading() && !health()) {
          <p class="sri__state">Cargando salud del módulo…</p>
        } @else if (!health()) {
          <p class="sri__state">Sin información disponible.</p>
        } @else {
          <div class="sri__health-grid">
            <article class="sri__health-tile">
              <span>Autorizados sin RIDE</span>
              <strong>{{ health()!.authorizedAwaitingPdf }}</strong>
            </article>
            <article class="sri__health-tile">
              <span>Correos pendientes de reintento</span>
              <strong>{{ health()!.emailRetryPending }}</strong>
            </article>
            <article class="sri__health-tile">
              <span>RIDE con error</span>
              <strong>{{ health()!.emailPdfFailed }}</strong>
            </article>
            <article class="sri__health-tile">
              <span>Sin correo de cliente</span>
              <strong>{{ health()!.emailNoCustomerEmail }}</strong>
            </article>
            <article class="sri__health-tile">
              <span>Correos enviados</span>
              <strong>{{ health()!.emailDelivered }}</strong>
            </article>
          </div>

          <div class="sri__health-statuses">
            @for (entry of healthStatusEntries(); track entry.status) {
              <span class="sri__health-chip sri__health-chip--{{ entry.status.toLowerCase() }}">
                {{ entry.status }} · {{ entry.count }}
              </span>
            }
          </div>

          @if (health()!.stuckDocuments.length) {
            <div class="sri__health-stuck">
              <h3>Documentos atascados (>{{ health()!.stuckThresholdMinutes }} min)</h3>
              <ul>
                @for (doc of health()!.stuckDocuments; track doc.id) {
                  <li>
                    <strong>#{{ doc.id }}</strong>
                    @if (doc.documentCode) { · {{ documentTypeLabel(doc.documentCode) }} }
                    @if (doc.sequentialNumber) { · {{ doc.sequentialNumber }} }
                    · {{ documentStatusLabel(doc.status || '') }}
                    @if (doc.lastStatusAt) { · {{ doc.lastStatusAt | date: 'short' }} }
                    @if (doc.lastProviderMessage) { <em>{{ doc.lastProviderMessage }}</em> }
                  </li>
                }
              </ul>
            </div>
          } @else {
            <p class="sri__state sri__state--ok">Sin documentos atascados en la ventana actual.</p>
          }
        }
      </section>

      <!-- Documents table -->
      <section class="sri__card">
        <header class="sri__card-header">
          <div>
            <span class="sri__eyebrow">Documentos</span>
            <h2>Comprobantes emitidos</h2>
          </div>
          <div class="sri__filter-row">
            <button
              type="button"
              class="sri__filter-btn"
              [class.sri__filter-btn--active]="statusFilter() === ''"
              (click)="statusFilter.set('')"
            >Todos</button>
            <button
              type="button"
              class="sri__filter-btn"
              [class.sri__filter-btn--active]="statusFilter() === 'READY_TO_SEND'"
              (click)="statusFilter.set('READY_TO_SEND')"
            >Para enviar</button>
            <button
              type="button"
              class="sri__filter-btn sri__filter-btn--success"
              [class.sri__filter-btn--active]="statusFilter() === 'AUTHORIZED'"
              (click)="statusFilter.set('AUTHORIZED')"
            >Autorizados</button>
            <button
              type="button"
              class="sri__filter-btn sri__filter-btn--warn"
              [class.sri__filter-btn--active]="statusFilter() === 'REJECTED'"
              (click)="statusFilter.set('REJECTED')"
            >Con observación</button>
          </div>
        </header>

        @if (documentsLoading()) {
          <p class="sri__state">Consultando documentos electrónicos...</p>
        } @else if (!filteredDocuments().length) {
          <p class="sri__state">No hay documentos para el filtro seleccionado.</p>
        } @else {
          <div class="sri__table">
            <div class="sri__thead">
              <span>Tipo</span>
              <span>Estado</span>
              <span>Número</span>
              <span>Subtotal</span>
              <span>IVA 15%</span>
              <span>Total</span>
              <span>Recepción</span>
              <span>Acciones</span>
            </div>
            @for (doc of filteredDocuments(); track doc.id) {
              <div
                class="sri__trow"
                [class.sri__trow--selected]="selectedDocumentId() === doc.id"
                [class.sri__trow--authorized]="doc.status === 'AUTHORIZED'"
                [class.sri__trow--rejected]="doc.status === 'REJECTED'"
                (click)="selectDocument(doc.id)"
              >
                <span class="sri__doc-type">{{ documentTypeLabel(doc.documentCode) }}</span>
                <span>
                  <span class="sri__status-pill" [attr.data-status]="doc.status">
                    {{ documentStatusLabel(doc.status) }}
                  </span>
                  <span
                    class="sri__email-badge"
                    [class.sri__email-badge--sent]="doc.rideEmailStatus === 'SENT'"
                    [class.sri__email-badge--error]="emailHasError(doc.rideEmailStatus)"
                    [title]="emailStatusLabel(doc)"
                  >
                    {{ emailStatusIcon(doc.rideEmailStatus) }}
                  </span>
                </span>
                <span class="sri__seq-num">{{ doc.sequentialNumber || 'Pendiente' }}</span>
                <span>{{ ivaSubtotal(doc.total) | currency: 'USD':'symbol':'1.2-2' }}</span>
                <span class="sri__iva-col">{{ ivaAmount(doc.total) | currency: 'USD':'symbol':'1.2-2' }}</span>
                <span>{{ doc.total | currency: 'USD':'symbol':'1.2-2' }}</span>
                <span class="sri__reception">{{ doc.receptionStatus || '—' }}</span>
                <div class="sri__actions">
                  @if (doc.status === 'PENDING_VALIDATION' || doc.status === 'REJECTED') {
                    <button type="button" class="sri__action-btn" (click)="validate(doc.id); $event.stopPropagation()">
                      Validar
                    </button>
                  }
                  @if (doc.status === 'READY_TO_SEND') {
                    <button type="button" class="sri__action-btn sri__action-btn--primary" (click)="emit(doc.id); $event.stopPropagation()">
                      Emitir
                    </button>
                  }
                  @if (doc.status === 'SENT') {
                    <button type="button" class="sri__action-btn" (click)="poll(doc.id); $event.stopPropagation()">
                      Consultar
                    </button>
                  }
                  <button type="button" class="sri__action-btn" (click)="selectDocument(doc.id); $event.stopPropagation()">
                    Detalle
                  </button>
                  @if (doc.status === 'AUTHORIZED') {
                    <button type="button" class="sri__action-btn" (click)="downloadRide(doc.id); $event.stopPropagation()">
                      RIDE
                    </button>
                    <button type="button" class="sri__action-btn" (click)="regenerateRide(doc.id); $event.stopPropagation()">
                      Regenerar
                    </button>
                  }
                </div>
              </div>
            }
          </div>
        }
      </section>

      <!-- Selected document detail -->
      @if (selectedDocument(); as doc) {
        <section class="sri__card sri__card--detail">
          <header class="sri__card-header">
            <div>
              <span class="sri__eyebrow">Seguimiento</span>
              <h2>Documento #{{ doc.id }} — {{ documentTypeLabel(doc.documentCode) }}</h2>
            </div>
            <button type="button" class="sri__close" (click)="selectedDocumentId.set(null)" aria-label="Cerrar">✕</button>
          </header>

          <!-- IVA breakdown for selected doc -->
          <div class="sri__iva-breakdown">
            <div class="sri__iva-row">
              <span>Subtotal (sin IVA)</span>
              <strong>{{ ivaSubtotal(doc.total) | currency: 'USD':'symbol':'1.2-2' }}</strong>
            </div>
            <div class="sri__iva-row sri__iva-row--tax">
              <span>IVA 15%</span>
              <strong>{{ ivaAmount(doc.total) | currency: 'USD':'symbol':'1.2-2' }}</strong>
            </div>
            <div class="sri__iva-row sri__iva-row--total">
              <span>Total con IVA</span>
              <strong>{{ doc.total | currency: 'USD':'symbol':'1.2-2' }}</strong>
            </div>
          </div>

          <!-- Detail cards -->
          <div class="sri__detail-grid">
            <article class="sri__detail-card">
              <h3>Estado del comprobante</h3>
              <div class="sri__detail-row">
                <span>Estado</span>
                <span class="sri__status-pill" [attr.data-status]="doc.status">{{ documentStatusLabel(doc.status) }}</span>
              </div>
              <div class="sri__detail-row">
                <span>Clave de acceso</span>
                <code class="sri__access-key">{{ doc.accessKey || 'Pendiente de generación' }}</code>
              </div>
              @if (doc.authorizationCode) {
                <div class="sri__detail-row">
                  <span>Código de autorización</span>
                  <code class="sri__auth-code">{{ doc.authorizationCode }}</code>
                </div>
              }
              @if (doc.providerMessage || doc.validationErrors) {
                <div class="sri__detail-row sri__detail-row--message">
                  <span>Mensaje SRI</span>
                  <span>{{ doc.providerMessage || doc.validationErrors }}</span>
                </div>
              }
              <div class="sri__detail-row sri__detail-row--message">
                <span>Correo RIDE</span>
                <span>{{ emailStatusLabel(doc) }}</span>
              </div>
            </article>

            <article class="sri__detail-card">
              <h3>Autorización manual</h3>
              <p class="sri__detail-help">Registra un código de autorización obtenido directamente del portal SRI.</p>
              <input
                [(ngModel)]="manualAuthorizationCode"
                class="sri__field"
                placeholder="Código de autorización SRI"
              />
              <button
                type="button"
                class="sri__btn sri__btn--full"
                [disabled]="!manualAuthorizationCode.trim()"
                (click)="authorize(doc.id)"
              >
                Guardar autorización
              </button>
            </article>
          </div>

          <!-- Transmission history -->
          @if (transmissionsLoading()) {
            <p class="sri__state">Cargando historial de transmisiones...</p>
          } @else if (transmissions().length) {
            <div>
              <h3 class="sri__section-label">Historial de transmisiones</h3>
              <div class="sri__table sri__table--transmissions">
                <div class="sri__thead sri__thead--transmissions">
                  <span>Paso</span>
                  <span>Respuesta</span>
                  <span>HTTP</span>
                  <span>Resultado</span>
                  <span>Fecha</span>
                  <span>Canal</span>
                </div>
                @for (tx of transmissions(); track tx.id) {
                  <div class="sri__trow sri__trow--transmission" [class.sri__trow--ok]="tx.success" [class.sri__trow--fail]="!tx.success">
                    <span>{{ tx.phase }}</span>
                    <span>{{ tx.providerStatus || tx.providerMessage || '—' }}</span>
                    <span>{{ tx.httpStatus || '—' }}</span>
                    <span class="sri__tx-result" [class.sri__tx-result--ok]="tx.success">{{ tx.success ? 'OK' : 'Error' }}</span>
                    <span>{{ tx.attemptedAt | date: 'dd/MM HH:mm' }}</span>
                    <span>{{ endpointLabel(tx.endpointUrl) }}</span>
                  </div>
                }
              </div>
            </div>
          } @else {
            <p class="sri__state">Aún no hay movimientos de transmisión registrados.</p>
          }
        </section>
      } @else {
        <p class="sri__state sri__state--hint">Haz clic en una fila para ver el detalle, historial de transmisiones y desglose de IVA del documento.</p>
      }
    </section>
  `,
  styles: `
    :host { display: contents; }

    .sri { display: grid; gap: 24px; padding: 32px; animation: pageEnter var(--dur-normal, 260ms) var(--ease-out, ease) both; }

    /* Header */
    .sri__header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 24px;
      flex-wrap: wrap;
    }

    .sri__eyebrow {
      display: block;
      font-size: 10px;
      letter-spacing: 0.22em;
      text-transform: uppercase;
      font-weight: 600;
      color: var(--aurora-dim);
      margin-bottom: 8px;
    }

    .sri__header h1 {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 28px;
      font-weight: 400;
      color: var(--text-strong);
      margin: 0 0 6px;
      letter-spacing: -0.01em;
    }

    .sri__header p { font-size: 13px; font-weight: 400; color: var(--text-muted); margin: 0; line-height: 1.6; }

    .sri__header-actions { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }
    .sri__ts { font-size: 11px; color: var(--text-faint); }

    /* Buttons */
    .sri__btn {
      padding: 9px 18px;
      background: transparent;
      border: 1px solid var(--aurora-border);
      color: var(--aurora-dim);
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      border-radius: 4px;
      cursor: pointer;
      transition: background var(--dur-fast) ease, border-color var(--dur-fast) ease, box-shadow var(--dur-fast) ease;
    }

    .sri__btn:hover:not(:disabled) {
      background: var(--aurora-ghost);
      border-color: var(--aurora);
      box-shadow: var(--shadow-sm);
    }

    .sri__btn:disabled { opacity: 0.35; cursor: not-allowed; }
    .sri__btn--full { width: 100%; margin-top: 8px; }
    .sri__btn--ghost {
      padding: 6px 12px;
      font-size: 10.5px;
    }

    /* Sequence editor modal */
    .sri__modal-backdrop {
      position: fixed; inset: 0;
      background: rgba(15, 28, 40, 0.45);
      z-index: 900;
    }
    .sri__modal {
      position: fixed;
      top: 50%; left: 50%;
      transform: translate(-50%, -50%);
      width: min(460px, calc(100vw - 32px));
      background: var(--surface);
      border: 1px solid var(--aurora-border);
      border-radius: 6px;
      padding: 24px;
      box-shadow: var(--shadow-lg, 0 20px 60px rgba(0,0,0,0.25));
      z-index: 901;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }
    .sri__modal h3 { margin: 0; font-family: var(--font-display, inherit); color: var(--aurora); font-size: 18px; }
    .sri__modal-hint { margin: 0; font-size: 12px; color: var(--text-faint); line-height: 1.5; }
    .sri__field { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: var(--text-muted); }
    .sri__field input[type="text"],
    .sri__field input[type="number"] {
      padding: 8px 10px;
      border: 1px solid var(--aurora-border);
      border-radius: 4px;
      background: var(--surface-muted, #fff);
      font: inherit;
    }
    .sri__field--inline { flex-direction: row; align-items: center; gap: 8px; }
    .sri__modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 6px; }

    /* IVA Banner */
    .sri__iva-banner {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 12px 18px;
      background: var(--ok-bg);
      border: 1px solid var(--ok-border);
      border-radius: 5px;
      font-size: 12.5px;
      color: var(--ok);
      flex-wrap: wrap;
    }

    .sri__iva-rate {
      font-weight: 700;
      font-size: 13px;
      letter-spacing: 0.06em;
      background: var(--ok);
      color: #fff;
      padding: 3px 10px;
      border-radius: 3px;
      flex-shrink: 0;
    }

    .sri__iva-total {
      margin-left: auto;
      font-weight: 600;
      flex-shrink: 0;
    }

    /* RIDE Studio */
    .sri__studio {
      display: grid;
      grid-template-columns: minmax(320px, 0.9fr) minmax(420px, 1.4fr);
      gap: 14px;
      align-items: stretch;
    }

    .sri__studio-panel,
    .sri__preview-panel {
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 18px;
      box-shadow: var(--shadow-xs);
    }

    .sri__expand-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      font-size: 12px;
      font-weight: 600;
    }
    .sri__expand-btn svg { width: 14px; height: 14px; }

    /* Reforzar legibilidad mínima de tablas dentro del RIDE preview */
    .sri__preview-panel app-ride-canvas table { font-size: 11px; }
    .sri__preview-panel app-ride-canvas th { font-size: 10.5px; font-weight: 700; }

    /* Overlay de vista previa ampliada */
    .sri__preview-overlay {
      position: fixed;
      inset: 0;
      background: rgba(7, 9, 14, 0.65);
      backdrop-filter: blur(6px);
      -webkit-backdrop-filter: blur(6px);
      z-index: 100;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      animation: fadeIn 180ms ease;
    }
    .sri__preview-overlay__inner {
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 14px;
      max-width: 1100px;
      width: 100%;
      max-height: 92vh;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      box-shadow: var(--shadow-lg);
    }
    .sri__preview-overlay__head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 16px 20px;
      border-bottom: 1px solid var(--line);
    }
    .sri__preview-overlay__head h3 {
      margin: 0;
      font-size: 16px;
      color: var(--text-strong);
    }
    .sri__preview-overlay__body {
      overflow: auto;
      padding: 20px 24px;
      flex: 1;
    }
    .sri__preview-overlay__body app-ride-canvas {
      display: block;
      transform-origin: top center;
      transform: scale(1.15);
      margin-top: 24px;
    }
    @keyframes fadeIn {
      from { opacity: 0; }
      to   { opacity: 1; }
    }

    .sri__template-gallery {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 8px;
      margin: 14px 0;
    }

    .sri__template-choice {
      min-height: 92px;
      text-align: left;
      border: 1px solid var(--line);
      border-radius: 6px;
      background: var(--surface);
      color: var(--text);
      padding: 12px;
      cursor: pointer;
      transition: border-color var(--dur-fast) ease, box-shadow var(--dur-fast) ease, transform var(--dur-fast) ease;
    }

    .sri__template-choice:hover { transform: translateY(-1px); border-color: var(--aurora-border); }
    .sri__template-choice--active { border-color: var(--aurora); box-shadow: 0 0 0 2px var(--aurora-ghost); }
    .sri__template-choice strong { display: block; font-size: 13px; margin-bottom: 6px; color: var(--text-strong); }
    .sri__template-choice span { display: block; font-size: 11px; line-height: 1.45; color: var(--text-muted); }

    .sri__studio-controls {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 10px;
    }

    .sri__studio-controls label,
    .sri__studio-footer {
      display: grid;
      gap: 6px;
      font-size: 11px;
      color: var(--text-faint);
      text-transform: uppercase;
      letter-spacing: 0.12em;
    }

    .sri__studio-footer { margin-top: 10px; }
    .sri__color { width: 100%; height: 39px; border: 1px solid var(--line-strong); border-radius: 4px; background: var(--surface); padding: 3px; }

    .sri__ride-hint {
      margin: 4px 0 0;
      font-size: 11px;
      color: var(--text-muted);
      letter-spacing: 0;
      text-transform: none;
    }
    .sri__ride-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .sri__ride-save-state {
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      padding: 3px 8px;
      border-radius: 999px;
      background: var(--surface-strong);
      color: var(--text-muted);
    }
    .sri__ride-save-state[data-state="saving"] { background: var(--info-bg, #e8f0ff); color: var(--info, #1f4ea1); }
    .sri__ride-save-state[data-state="saved"] { background: var(--ok-bg); color: var(--ok); }
    .sri__ride-save-state[data-state="pending"] { background: var(--warn-bg, #fff4e0); color: var(--warn, #a76b00); }
    .sri__ride-save-state[data-state="error"] { background: var(--err-bg, #fde0e0); color: var(--err, #b3261e); }

    .sri__logo-cropper {
      margin-top: 12px;
      display: grid;
      gap: 10px;
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 12px;
      background: var(--surface);
    }

    .sri__cropper-head {
      display: grid;
      gap: 3px;
    }

    .sri__cropper-head strong {
      font-size: 12px;
      color: var(--text-strong);
    }

    .sri__cropper-head span {
      font-size: 11px;
      color: var(--text-muted);
    }

    .sri__logo-cropper image-cropper {
      display: block;
      height: 220px;
      max-height: 220px;
      border: 1px solid var(--line-strong);
      border-radius: 6px;
      background: #ffffff;
      overflow: hidden;
    }

    .sri__cropper-actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      flex-wrap: wrap;
    }

    .sri__preview-panel { min-width: 0; }
    .sri__preview-panel app-ride-canvas { display: block; margin-top: 12px; }

    /* KPIs */
    .sri__kpis {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 10px;
    }

    .sri__kpi {
      padding: 18px 20px;
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 5px;
      display: grid;
      gap: 5px;
      transition: box-shadow var(--dur-fast) ease, border-color var(--dur-fast) ease;
      animation: slideUp var(--dur-normal, 260ms) var(--ease-out, ease) both;
    }

    .sri__kpi:hover { box-shadow: var(--shadow-sm); border-color: var(--line-strong); }

    .sri__kpi--success { border-left: 3px solid var(--ok-border); }
    .sri__kpi--warn { border-left: 3px solid var(--warn-border); }

    .sri__kpi > span { font-size: 10px; letter-spacing: 0.18em; text-transform: uppercase; color: var(--text-faint); }

    .sri__kpi > strong {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 28px;
      font-weight: 400;
      color: var(--text-strong);
      letter-spacing: -0.02em;
    }

    .sri__kpi > small { font-size: 11px; color: var(--text-muted); }

    /* Layout */
    .sri__layout {
      display: grid;
      grid-template-columns: 1fr 280px;
      gap: 12px;
    }

    /* Card */
    .sri__card {
      padding: 20px;
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 6px;
      display: grid;
      gap: 16px;
      animation: scaleIn var(--dur-normal, 260ms) var(--ease-out, ease) both;
    }

    .sri__card--detail {
      border-color: var(--aurora-border);
      background: var(--bg-panel);
    }

    .sri__card-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;
    }

    .sri__card h2 { font-size: 14px; font-weight: 600; color: var(--text); margin: 0; }

    /* Filter buttons */
    .sri__filter-row { display: flex; gap: 6px; flex-wrap: wrap; }

    .sri__filter-btn {
      padding: 5px 12px;
      background: transparent;
      border: 1px solid var(--line-strong);
      border-radius: 3px;
      font-size: 10.5px;
      font-weight: 600;
      color: var(--text-muted);
      cursor: pointer;
      transition: all var(--dur-fast) ease;
    }

    .sri__filter-btn:hover { background: var(--surface); color: var(--text); }
    .sri__filter-btn--active { background: var(--surface-strong); border-color: var(--line-strong); color: var(--text); }
    .sri__filter-btn--success.sri__filter-btn--active { background: var(--ok-bg); border-color: var(--ok-border); color: var(--ok); }
    .sri__filter-btn--warn.sri__filter-btn--active { background: var(--warn-bg); border-color: var(--warn-border); color: var(--warn); }

    /* Sequences */
    .sri__sequence-list { display: grid; gap: 6px; }

    .sri__sequence-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 14px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 4px;
      transition: background var(--dur-fast) ease;
    }

    .sri__sequence-item:hover { background: var(--surface-strong); }

    .sri__sequence-code {
      font-size: 12px;
      font-weight: 700;
      color: var(--text);
      letter-spacing: 0.04em;
      flex-shrink: 0;
    }

    .sri__sequence-meta { display: grid; gap: 2px; flex: 1; min-width: 0; }
    .sri__sequence-meta span { font-size: 11px; color: var(--text-muted); }
    .sri__sequence-meta small { font-size: 10px; color: var(--text-faint); }

    .sri__seq-badge {
      padding: 2px 8px;
      border-radius: 2px;
      font-size: 9px;
      font-weight: 700;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      background: var(--surface-strong);
      color: var(--text-faint);
      flex-shrink: 0;
    }

    .sri__seq-badge--active { background: var(--ok-bg); color: var(--ok); }

    /* Fiscal summary */
    .sri__fiscal-grid { display: grid; gap: 8px; }

    .sri__fiscal-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 14px;
      background: var(--surface);
      border-radius: 4px;
      border: 1px solid var(--line);
    }

    .sri__fiscal-item span { font-size: 12px; color: var(--text-muted); }
    .sri__fiscal-item strong { font-size: 14px; font-weight: 600; color: var(--text); }
    .sri__fiscal-item--green strong { color: var(--ok); }

    /* ATS panel */
    .sri__ats .sri__card-header { gap: 12px; flex-wrap: wrap; }
    .sri__ats-form { display: flex; gap: 8px; align-items: flex-end; flex-wrap: wrap; }
    .sri__ats-form .sri__field { width: 110px; }
    .sri__ats-form .sri__field input { width: 100%; min-width: 5ch; padding-right: 6px; }
    .sri__ats-list { display: grid; gap: 8px; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); }

    .sri__ats-item {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 12px 14px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .sri__ats-item-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .sri__ats-item-head strong { font-size: 14px; font-weight: 600; color: var(--text); }

    .sri__ats-status {
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      padding: 2px 8px;
      border-radius: 999px;
      background: var(--surface-strong);
      color: var(--text-muted);
    }
    .sri__ats-status--draft { background: var(--surface-strong); color: var(--text-muted); }
    .sri__ats-status--generated { background: var(--info-bg, #e8f0ff); color: var(--info, #1f4ea1); }
    .sri__ats-status--submitted { background: var(--ok-bg); color: var(--ok); }
    .sri__ats-status--closed { background: var(--surface-strong); color: var(--text); }

    .sri__ats-stats { display: grid; gap: 4px; margin: 0; }
    .sri__ats-stats > div { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-muted); }
    .sri__ats-stats dt { font-weight: 500; }
    .sri__ats-stats dd { margin: 0; color: var(--text); font-weight: 500; }

    .sri__ats-actions { display: flex; gap: 6px; flex-wrap: wrap; }

    /* Health monitor */
    .sri__health .sri__card-header { gap: 12px; flex-wrap: wrap; }
    .sri__health-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .sri__health-grid {
      display: grid;
      gap: 10px;
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      margin-top: 10px;
    }
    .sri__health-tile {
      background: var(--surface-strong, rgba(35, 102, 65, 0.06));
      border: 1px solid var(--border-soft, rgba(35, 102, 65, 0.18));
      border-radius: 8px;
      padding: 10px 12px;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .sri__health-tile span { font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.08em; }
    .sri__health-tile strong { font-size: 22px; color: var(--text); font-weight: 600; }
    .sri__health-statuses { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 12px; }
    .sri__health-chip {
      display: inline-flex;
      align-items: center;
      padding: 4px 10px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.04em;
      background: var(--surface-strong);
      color: var(--text-muted);
    }
    .sri__health-chip--authorized { background: var(--ok-bg); color: var(--ok); }
    .sri__health-chip--rejected { background: var(--danger-bg, #fde8e8); color: var(--danger, #b42318); }
    .sri__health-chip--cancelled { background: var(--surface-muted); color: var(--text-faint); }
    .sri__health-chip--sent,
    .sri__health-chip--ready_to_send { background: var(--info-bg, #e8f0ff); color: var(--info, #1f4ea1); }
    .sri__health-chip--pending_validation,
    .sri__health-chip--draft { background: var(--warn-bg, #fff4d6); color: var(--warn, #92590f); }
    .sri__health-stuck { margin-top: 14px; }
    .sri__health-stuck h3 { margin: 0 0 6px; font-size: 13px; font-weight: 600; color: var(--text); }
    .sri__health-stuck ul { list-style: none; margin: 0; padding: 0; display: grid; gap: 4px; }
    .sri__health-stuck li { font-size: 12px; color: var(--text); padding: 6px 8px; background: var(--surface-muted); border-radius: 6px; }
    .sri__health-stuck em { color: var(--text-muted); font-style: italic; }
    .sri__state--ok { color: var(--ok, #2f8f4d); }

    /* Table */
    .sri__table { display: grid; gap: 3px; }

    .sri__thead {
      display: grid;
      grid-template-columns: 0.8fr 0.9fr 0.9fr 0.9fr 0.8fr 0.9fr 0.8fr 1.2fr;
      gap: 10px;
      align-items: center;
      padding: 9px 14px;
      font-size: 10px;
      text-transform: uppercase;
      letter-spacing: 0.16em;
      font-weight: 600;
      color: var(--text-faint);
      background: var(--surface-muted);
      border-radius: 4px;
    }

    .sri__thead--transmissions {
      grid-template-columns: 0.8fr 1.2fr 0.5fr 0.6fr 0.8fr 0.9fr;
    }

    .sri__trow {
      display: grid;
      grid-template-columns: 0.8fr 0.9fr 0.9fr 0.9fr 0.8fr 0.9fr 0.8fr 1.2fr;
      gap: 10px;
      align-items: center;
      padding: 10px 14px;
      background: var(--surface);
      border-radius: 4px;
      font-size: 12.5px;
      color: var(--text);
      cursor: pointer;
      transition: background var(--dur-fast) ease, border-color var(--dur-fast) ease;
      border: 1px solid transparent;
    }

    .sri__trow:hover { background: var(--surface-strong); border-color: var(--line); }
    .sri__trow--selected { border-color: var(--aurora-border); background: var(--aurora-ghost); }
    .sri__trow--authorized { border-left: 3px solid var(--ok-border); }
    .sri__trow--rejected { border-left: 3px solid var(--danger-border); }

    .sri__trow--transmission {
      grid-template-columns: 0.8fr 1.2fr 0.5fr 0.6fr 0.8fr 0.9fr;
      cursor: default;
      border: 1px solid transparent;
    }

    .sri__trow--ok { border-left: 2px solid var(--ok-border); }
    .sri__trow--fail { border-left: 2px solid var(--danger-border); }

    .sri__doc-type { font-weight: 700; font-size: 11px; letter-spacing: 0.06em; }
    .sri__seq-num { font-family: monospace; font-size: 11px; color: var(--text-muted); }
    .sri__iva-col { color: var(--ok); font-weight: 600; }
    .sri__reception { font-size: 11px; color: var(--text-faint); }

    /* Status pill */
    .sri__status-pill {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 3px;
      font-size: 10px;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      background: var(--surface-strong);
      color: var(--text-muted);
    }

    .sri__status-pill[data-status='AUTHORIZED'] { background: var(--ok-bg); color: var(--ok); }
    .sri__status-pill[data-status='READY_TO_SEND'] { background: var(--info-bg); color: var(--info); }
    .sri__status-pill[data-status='SENT'] { background: var(--warn-bg); color: var(--warn); }
    .sri__status-pill[data-status='REJECTED'] { background: var(--danger-bg); color: var(--danger); }

    .sri__email-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 24px;
      height: 24px;
      margin-left: 6px;
      border-radius: 4px;
      background: var(--surface-strong);
      color: var(--text-faint);
      font-size: 13px;
      vertical-align: middle;
      border: 1px solid var(--line);
    }

    .sri__email-badge--sent { background: var(--ok-bg); color: var(--ok); border-color: var(--ok-border); }
    .sri__email-badge--error { background: var(--danger-bg); color: var(--danger); border-color: var(--danger-border); }

    /* Actions */
    .sri__actions { display: flex; gap: 5px; flex-wrap: wrap; }

    .sri__action-btn {
      padding: 4px 10px;
      background: var(--surface);
      border: 1px solid var(--line-strong);
      border-radius: 3px;
      font-size: 10.5px;
      font-weight: 600;
      color: var(--text-muted);
      cursor: pointer;
      transition: all var(--dur-fast) ease;
      white-space: nowrap;
    }

    .sri__action-btn:hover { background: var(--surface-strong); color: var(--text); border-color: var(--line-strong); }
    .sri__action-btn--primary { border-color: var(--aurora-border); color: var(--aurora); background: var(--aurora-ghost); }
    .sri__action-btn--primary:hover { background: var(--aurora-ghost); border-color: var(--aurora); }

    /* IVA Breakdown */
    .sri__iva-breakdown {
      display: grid;
      gap: 3px;
      padding: 16px;
      background: var(--surface);
      border-radius: 5px;
      border: 1px solid var(--line);
    }

    .sri__iva-row {
      display: flex;
      justify-content: space-between;
      padding: 6px 8px;
      font-size: 13px;
      color: var(--text-muted);
      border-radius: 3px;
    }

    .sri__iva-row strong { font-weight: 600; color: var(--text); }
    .sri__iva-row--tax strong { color: var(--ok); }
    .sri__iva-row--total {
      border-top: 1px solid var(--line);
      margin-top: 4px;
      padding-top: 10px;
      font-weight: 600;
    }
    .sri__iva-row--total strong { font-family: 'Cormorant Garamond', Georgia, serif; font-size: 18px; color: var(--text-strong); }

    /* Detail */
    .sri__detail-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 12px;
    }

    .sri__detail-card {
      padding: 16px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 5px;
      display: grid;
      gap: 10px;
    }

    .sri__detail-card h3 { font-size: 13px; font-weight: 600; color: var(--text); }
    .sri__detail-help { font-size: 12px; color: var(--text-faint); margin: 0; }

    .sri__detail-row {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      gap: 12px;
      font-size: 12px;
      color: var(--text-muted);
    }

    .sri__detail-row--message { flex-direction: column; gap: 4px; }
    .sri__detail-row--message span:last-child { color: var(--text); background: var(--surface-strong); padding: 8px; border-radius: 3px; font-size: 11px; line-height: 1.5; }

    .sri__access-key,
    .sri__auth-code {
      font-family: monospace;
      font-size: 10px;
      color: var(--text);
      background: var(--surface-strong);
      padding: 2px 6px;
      border-radius: 2px;
      word-break: break-all;
      text-align: right;
    }

    .sri__section-label {
      font-size: 10px;
      letter-spacing: 0.2em;
      text-transform: uppercase;
      color: var(--text-faint);
      font-weight: 600;
      margin: 0 0 10px;
    }

    .sri__tx-result { font-weight: 700; font-size: 10px; }
    .sri__tx-result--ok { color: var(--ok); }

    /* Field */
    .sri__field {
      width: 100%;
      background: var(--bg-panel);
      border: 1px solid var(--line-strong);
      border-radius: 4px;
      color: var(--text);
      padding: 9px 12px;
      font-size: 13px;
      outline: none;
      transition: border-color var(--dur-fast) ease, box-shadow var(--dur-fast) ease;
    }

    .sri__field:focus {
      border-color: var(--aurora-border);
      box-shadow: 0 0 0 3px var(--aurora-ghost);
    }

    .sri__field::placeholder { color: var(--text-faint); }

    /* State */
    .sri__state {
      margin: 0;
      padding: 12px 16px;
      border-radius: 4px;
      border: 1px solid var(--line-subtle);
      background: var(--surface-muted);
      font-size: 12.5px;
      color: var(--text-muted);
    }

    .sri__state--hint { text-align: center; padding: 20px; }

    .sri__close {
      background: none;
      border: 1px solid var(--line-strong);
      border-radius: 3px;
      color: var(--text-faint);
      width: 28px;
      height: 28px;
      cursor: pointer;
      font-size: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all var(--dur-fast) ease;
      flex-shrink: 0;
    }

    .sri__close:hover { background: var(--danger-bg); border-color: var(--danger-border); color: var(--danger); }

    @media (max-width: 1200px) {
      .sri { padding: 20px 16px; }
      .sri__studio { grid-template-columns: 1fr; }
      .sri__layout { grid-template-columns: 1fr; }
      .sri__thead, .sri__trow {
        grid-template-columns: 0.8fr 0.9fr 1fr 1fr 0.8fr;
      }
      .sri__thead span:nth-child(6),
      .sri__thead span:nth-child(7),
      .sri__trow > span:nth-child(6),
      .sri__trow > span:nth-child(7) { display: none; }
    }

    @media (max-width: 768px) {
      .sri__header { flex-direction: column; }
      .sri__header-actions { align-items: flex-start; }
      .sri__template-gallery,
      .sri__studio-controls { grid-template-columns: 1fr; }
      .sri__thead, .sri__trow { grid-template-columns: 1fr 1fr 1fr; }
      .sri__thead span:nth-child(n+4),
      .sri__trow > span:nth-child(n+4) { display: none; }
    }
  `,
})
export class Sri {
  private readonly erpApi = inject(ErpApi);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly fileDownload = inject(FileDownloadService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly documents = signal<SriDocumentDto[]>([]);
  protected readonly sequences = signal<SequenceDto[]>([]);
  protected readonly sequenceEditor = signal<UpsertSequencePayload & { documentCode: string } | null>(null);
  protected readonly sequenceSaving = signal(false);
  protected readonly transmissions = signal<SriTransmissionDto[]>([]);
  protected readonly feedback = signal<UiFeedback | null>(null);
  protected readonly documentsLoading = signal(false);
  protected readonly transmissionsLoading = signal(false);
  protected readonly selectedDocumentId = signal<number | null>(null);
  protected readonly statusFilter = signal('');
  protected readonly exporting = signal<ExportFormat | null>(null);
  protected readonly rideTemplate = signal<RideTemplateDto | null>(null);
  protected readonly rideSaving = signal(false);
  protected readonly rideBase = signal('Classic');
  protected readonly rideAutoSaveState = signal<'idle' | 'pending' | 'saving' | 'saved' | 'error'>('idle');
  private readonly rideDefaultLayout: RideLayoutState = {
    headerColor: '#123a5a',
    accentColor: '#0f766e',
    borderColor: '#d9e2ec',
    fontFamily: "'DM Sans', system-ui, sans-serif",
    logoUrl: '',
    footerNote: 'Documento generado por InsightVision',
  };
  protected readonly rideLayout = signal<RideLayoutState>({ ...this.rideDefaultLayout });
  protected readonly previewExpanded = signal(false);

  protected togglePreviewExpanded(open: boolean): void {
    this.previewExpanded.set(open);
  }
  protected readonly rideTemplateBases = [
    { code: 'Classic', name: 'Clásico legal', description: 'Orden corporativo para auditoría y archivo.' },
    { code: 'Modern', name: 'Moderno limpio', description: 'Lectura rápida con totales protagonistas.' },
    { code: 'Minimal', name: 'Minimal', description: 'Datos esenciales con una lectura sobria.' },
  ];
  protected readonly logoCropperEvent = signal<Event | null>(null);
  protected readonly logoCroppedBlob = signal<Blob | null>(null);
  protected readonly logoPreviewUrl = signal('');
  protected manualAuthorizationCode = '';
  private readonly ridePreviewChanges = new Subject<void>();

  protected readonly atsPeriods = signal<AtsPeriodDto[]>([]);
  protected readonly atsLoading = signal(false);
  protected readonly atsBusy = signal<number | 'new' | null>(null);
  protected readonly atsForm = signal<{ year: number; month: number }>({
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
  });

  protected readonly health = signal<SriHealthSnapshotDto | null>(null);
  protected readonly healthLoading = signal(false);
  protected readonly healthReaping = signal(false);

  protected readonly healthStatusEntries = computed(() => {
    const snapshot = this.health();
    if (!snapshot) return [] as Array<{ status: string; count: number }>;
    return Object.entries(snapshot.documentsByStatus)
      .map(([status, count]) => ({ status, count: Number(count) }))
      .sort((a, b) => b.count - a.count);
  });

  protected readonly selectedDocument = computed(() =>
    this.documents().find((d) => d.id === this.selectedDocumentId()) ?? null,
  );

  protected readonly filteredDocuments = computed(() => {
    const filter = this.statusFilter();
    if (!filter) return this.documents();
    return this.documents().filter((d) => d.status === filter);
  });

  protected readonly totalBilled = computed(() =>
    this.documents().reduce((sum, d) => sum + (d.total || 0), 0),
  );

  protected readonly totalAuthorized = computed(() =>
    this.documents()
      .filter((d) => d.status === 'AUTHORIZED')
      .reduce((sum, d) => sum + (d.total || 0), 0),
  );

  protected readonly totalIvaCollected = computed(() =>
    this.ivaAmount(this.totalAuthorized()),
  );

  protected readonly rideCanvasLayout = computed<RideCanvasLayout>(() => ({
    ...this.rideLayout(),
    logoUrl: this.logoPreviewUrl() || this.rideLayout().logoUrl,
  }));

  protected readonly ridePreviewData = computed<RideCanvasData>(() =>
    this.buildRidePreviewData(this.selectedDocument() ?? this.documents()[0] ?? null),
  );

  constructor() {
    this.ridePreviewChanges
      .pipe(debounceTime(1200), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.saveRideStudio(false));
    this.reload();
    this.loadRideStudio();
  }

  protected loading(): boolean {
    return this.documentsLoading() || this.transmissionsLoading();
  }

  /** Subtotal without IVA from an IVA-inclusive total */
  protected ivaSubtotal(total: number): number {
    return total / (1 + IVA_RATE);
  }

  /** IVA amount from an IVA-inclusive total */
  protected ivaAmount(total: number): number {
    return total - this.ivaSubtotal(total);
  }

  private buildRidePreviewData(document: SriDocumentDto | null): RideCanvasData {
    const total = document?.total ?? 11.5;
    const subtotal = document?.subtotal ?? this.ivaSubtotal(total);
    const tax = document?.tax ?? this.ivaAmount(total);
    const accessKey = document?.accessKey || this.sampleAccessKey();
    const number = this.buildDocumentNumber(document);

    return {
      legal: {
        ruc: accessKey.length >= 23 ? accessKey.slice(10, 23) : '9999999999999',
        razonSocial: 'NEGOCIO DE EJEMPLO',
        nombreComercial: 'InsightVision',
        dirMatriz: 'Dirección matriz',
        dirEstablecimiento: 'Dirección establecimiento',
        obligadoContabilidad: 'NO',
        contribuyenteEspecial: 'No aplica',
        contribuyenteRimpe: 'CONTRIBUYENTE RÉGIMEN RIMPE',
        agenteRetencion: 'No aplica',
        ambiente: accessKey.charAt(23) === '2' ? '2' : '1',
        ambienteLabel: accessKey.charAt(23) === '2' ? 'Producción' : 'Pruebas',
        tipoEmision: '1',
      },
      documento: {
        version: '1.1.0',
        codDoc: document?.documentCode || '01',
        tipo: this.documentTypeLabel(document?.documentCode || '01'),
        estab: number.slice(0, 3),
        ptoEmi: number.slice(4, 7),
        secuencial: number.slice(8),
        numero: number,
        fechaEmision: this.formatDate(document?.issueDate),
        autorizacion: document?.authorizationCode || accessKey,
        fechaAutorizacion: this.formatDateTime(document?.authorizedAt),
        estado: document?.status || 'PREVIEW',
      },
      cliente: {
        nombre: document?.buyerName || 'Consumidor final',
        identificacion: document?.buyerIdentification || '9999999999999',
        tipoIdentificacion: document?.buyerIdentification ? '04/05' : '07',
        direccion: 'Dirección del comprador',
        email: 'cliente@correo.com',
      },
      detalles: this.buildPreviewDetails(subtotal),
      totales: {
        subtotal: this.formatMoney(subtotal),
        descuento: this.formatMoney(0),
        iva15: this.formatMoney(tax),
        impuestosTotal: this.formatMoney(tax),
        propina: this.formatMoney(0),
        total: this.formatMoney(total),
        moneda: 'DOLAR',
        pagos: [{
          formaPago: '20 - Transferencia, depósito u otro pago bancario',
          total: this.formatMoney(total),
          plazo: '0',
          unidadTiempo: 'dias',
        }],
      },
      seguridad: {
        claveAcceso: accessKey,
      },
      leyendas: ['IVA 15% código 4', 'Factura SRI versión 1.1.0', 'QR y clave de acceso protegidos'],
      infoAdicional: [
        { nombre: 'Estado', valor: this.documentStatusLabel(document?.status || 'PENDING_VALIDATION') },
        { nombre: 'Correo RIDE', valor: document ? this.emailStatusLabel(document) : 'Vista previa de diseno.' },
      ],
    };
  }

  private buildPreviewDetails(subtotal: number): RideCanvasData['detalles'] {
    if (subtotal <= 0) {
      return [{
        codigo: 'SERV-001',
        codigoAuxiliar: 'S-AUX-001',
        descripcion: 'Servicio de ejemplo',
        cantidad: '1.00',
        precioUnitario: '0.00',
        descuento: '0.00',
        subtotal: '0.00',
        ivaCodigo: '2',
        ivaCodigoPorcentaje: '4',
        ivaTarifa: '15.00',
        ivaBase: '0.00',
        ivaValor: '0.00',
      }];
    }
    const first = Number((subtotal * 0.62).toFixed(2));
    const second = Number((subtotal - first).toFixed(2));
    return [
      {
        codigo: 'PROD-001',
        codigoAuxiliar: 'BAR-001',
        descripcion: 'Producto principal',
        cantidad: '1.00',
        precioUnitario: this.formatMoney(first),
        descuento: '0.00',
        subtotal: this.formatMoney(first),
        ivaCodigo: '2',
        ivaCodigoPorcentaje: '4',
        ivaTarifa: '15.00',
        ivaBase: this.formatMoney(first),
        ivaValor: this.formatMoney(this.ivaAmount(first * (1 + IVA_RATE))),
      },
      {
        codigo: 'SERV-002',
        codigoAuxiliar: 'SVC-002',
        descripcion: 'Servicio complementario',
        cantidad: '1.00',
        precioUnitario: this.formatMoney(second),
        descuento: '0.00',
        subtotal: this.formatMoney(second),
        ivaCodigo: '2',
        ivaCodigoPorcentaje: '4',
        ivaTarifa: '15.00',
        ivaBase: this.formatMoney(second),
        ivaValor: this.formatMoney(this.ivaAmount(second * (1 + IVA_RATE))),
      },
    ];
  }

  private buildDocumentNumber(document: SriDocumentDto | null): string {
    const sequential = document?.sequentialNumber || '000000001';
    if (sequential.includes('-')) {
      return sequential;
    }
    return `001-001-${sequential.padStart(9, '0')}`;
  }

  private sampleAccessKey(): string {
    return '1904202601999999999999910010010000000011234567810';
  }

  private formatMoney(value: number): string {
    return (Number.isFinite(value) ? value : 0).toFixed(2);
  }

  private formatDate(value?: string): string {
    if (!value) {
      return '19/04/2026';
    }
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return value;
    }
    return parsed.toLocaleDateString('es-EC', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  private formatDateTime(value?: string): string {
    if (!value) {
      return '19/04/2026 00:00:00';
    }
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return value;
    }
    return parsed.toLocaleString('es-EC', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  }

  protected documentStatusLabel(status: string): string {
    return sriStatusLabel(status);
  }

  protected documentTypeLabel(code: string): string {
    if (!code) return 'Comprobante SRI';
    const numericLabels: Record<string, string> = {
      '01': 'Factura',
      '03': 'Liquidación de compra',
      '04': 'Nota de crédito',
      '05': 'Nota de débito',
      '06': 'Guía de remisión',
      '07': 'Comprobante de retención',
    };
    return numericLabels[code] ?? sriDocTypeLabel(code);
  }

  protected nextActionLabel(): string {
    if (this.countStatus('REJECTED') > 0) return 'Corregir';
    if (this.countStatus('READY_TO_SEND') > 0) return 'Emitir';
    if (this.countStatus('SENT') > 0) return 'Consultar';
    return 'En orden';
  }

  protected endpointLabel(endpointUrl: string): string {
    if (endpointUrl?.includes('autorizacion')) return 'Autorización';
    if (endpointUrl?.includes('recepcion')) return 'Recepción';
    return 'Servicio';
  }

  protected emailStatusIcon(status?: string): string {
    if (status === 'SENT') return '✅';
    if (this.emailHasError(status)) return '❌';
    return '📧';
  }

  protected emailHasError(status?: string): boolean {
    return status === 'FAILED' || status === 'PDF_FAILED';
  }

  protected emailStatusLabel(doc: SriDocumentDto): string {
    const labels: Record<string, string> = {
      SENT: 'Correo enviado al cliente.',
      NO_CUSTOMER_EMAIL: 'PDF generado; el cliente no tiene correo registrado.',
      RETRY_PENDING: `Envio pendiente de reintento (${doc.rideEmailAttempts ?? 0}/3).`,
      FAILED: 'Error de correo: ',
      PDF_FAILED: 'No se pudo generar el PDF RIDE: ',
      DISABLED: 'Envio de correos RIDE deshabilitado.',
      SKIPPED: 'No hay SMTP configurado para enviar el RIDE.',
    };
    const status = doc.rideEmailStatus || 'PENDING';
    if (status === 'FAILED' || status === 'PDF_FAILED') {
      return `${labels[status]}${doc.rideEmailError || 'revisar configuración.'}`;
    }
    return labels[status] ?? 'Correo aún no enviado.';
  }

  protected countStatus(status: string): number {
    return this.documents().filter((d) => d.status === status).length;
  }

  protected exportDocuments(format: ExportFormat): void {
    this.exporting.set(format);
    this.feedback.set(null);

    this.erpApi
      .exportSriDocuments(format, this.statusFilter() || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.fileDownload.download(response, `comprobantes-sri.${format}`);
          this.feedback.set(this.httpFeedback.success(`Comprobantes exportados en ${format.toUpperCase()}.`));
          this.exporting.set(null);
        },
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudieron exportar los comprobantes SRI.'));
          this.exporting.set(null);
        },
      });
  }

  protected reload(): void {
    this.documentsLoading.set(true);

    this.erpApi
      .getSriDocuments()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (documents) => {
          this.documents.set(documents ?? []);
          this.documentsLoading.set(false);
          const selected = this.selectedDocumentId();
          if (selected) this.selectDocument(selected);
        },
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudieron cargar los documentos electrónicos.'));
          this.documentsLoading.set(false);
        },
      });

    this.erpApi
      .getSequences()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (sequences) => this.sequences.set(sequences ?? []),
        error: (error) => this.feedback.set(this.httpFeedback.fromError(error, 'No se pudieron cargar las numeraciones.')),
      });

    this.loadAtsPeriods();
    this.loadSriHealth();
  }

  protected loadSriHealth(): void {
    this.healthLoading.set(true);
    this.erpApi
      .getSriHealth()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (snapshot) => {
          this.health.set(snapshot);
          this.healthLoading.set(false);
        },
        error: (error) => {
          this.healthLoading.set(false);
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo cargar la salud SRI.'));
        },
      });
  }

  protected reapSriHealth(): void {
    this.healthReaping.set(true);
    this.erpApi
      .reapSriHealth()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.healthReaping.set(false);
          const requeued = result.authorizedRequeuedForPdf;
          const flagged = result.stuckDocumentsFlagged;
          const message = requeued === 0 && flagged === 0
            ? 'No se encontraron documentos pendientes ni atascados.'
            : `RIDE re-encolados: ${requeued} · documentos atascados marcados: ${flagged}.`;
          this.feedback.set(this.httpFeedback.success(message));
          this.loadSriHealth();
        },
        error: (error) => {
          this.healthReaping.set(false);
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo ejecutar la limpieza SRI.'));
        },
      });
  }

  protected loadAtsPeriods(): void {
    this.atsLoading.set(true);
    this.erpApi
      .listAtsPeriods()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (periods) => {
          this.atsPeriods.set(periods ?? []);
          this.atsLoading.set(false);
        },
        error: (error) => {
          this.atsLoading.set(false);
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudieron cargar los periodos ATS.'));
        },
      });
  }

  protected updateAtsForm(field: 'year' | 'month', value: number): void {
    this.atsForm.update((form) => ({ ...form, [field]: Number(value) || form[field] }));
  }

  protected generateAtsPeriod(): void {
    const { year, month } = this.atsForm();
    if (!year || year < 2000 || year > 2100) {
      this.feedback.set(this.httpFeedback.warning('Año fiscal inválido.'));
      return;
    }
    if (!month || month < 1 || month > 12) {
      this.feedback.set(this.httpFeedback.warning('Mes fiscal inválido (1-12).'));
      return;
    }
    this.atsBusy.set('new');
    this.erpApi
      .generateAtsPeriod({ fiscalYear: year, fiscalMonth: month })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (period) => {
          this.atsBusy.set(null);
          this.feedback.set(this.httpFeedback.success(
            `ATS ${period.fiscalYear}-${String(period.fiscalMonth).padStart(2, '0')} generado.`
          ));
          this.loadAtsPeriods();
        },
        error: (error) => {
          this.atsBusy.set(null);
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo generar el ATS.'));
        },
      });
  }

  protected markAtsSubmitted(period: AtsPeriodDto): void {
    this.atsBusy.set(period.id);
    this.erpApi
      .markAtsSubmitted(period.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.atsBusy.set(null);
          this.feedback.set(this.httpFeedback.success('ATS marcado como enviado al SRI.'));
          this.loadAtsPeriods();
        },
        error: (error) => {
          this.atsBusy.set(null);
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo marcar el ATS como enviado.'));
        },
      });
  }

  protected closeAtsPeriod(period: AtsPeriodDto): void {
    this.atsBusy.set(period.id);
    this.erpApi
      .closeAtsPeriod(period.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.atsBusy.set(null);
          this.feedback.set(this.httpFeedback.success('Periodo ATS cerrado.'));
          this.loadAtsPeriods();
        },
        error: (error) => {
          this.atsBusy.set(null);
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo cerrar el periodo ATS.'));
        },
      });
  }

  protected downloadAtsXml(period: AtsPeriodDto): void {
    const fileName = `ats_${period.fiscalYear}_${String(period.fiscalMonth).padStart(2, '0')}.xml`;
    this.erpApi
      .downloadAtsXml(period.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.fileDownload.download(response, fileName),
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo descargar el XML.'));
        },
      });
  }

  protected openSequenceEditor(seq: SequenceDto): void {
    this.sequenceEditor.set({
      id: seq.id,
      documentCode: seq.documentCode,
      documentTypeId: seq.documentTypeId,
      establishmentCode: seq.establishmentCode,
      emissionPointCode: seq.emissionPointCode,
      currentNumber: seq.currentNumber,
      incrementStep: seq.incrementStep,
      active: seq.active,
    });
  }

  protected closeSequenceEditor(): void {
    if (this.sequenceSaving()) return;
    this.sequenceEditor.set(null);
  }

  protected saveSequenceEditor(): void {
    const draft = this.sequenceEditor();
    if (!draft) return;
    const establishment = (draft.establishmentCode || '').trim();
    const emission = (draft.emissionPointCode || '').trim();
    if (!/^\d{1,3}$/.test(establishment) || !/^\d{1,3}$/.test(emission)) {
      this.feedback.set(this.httpFeedback.warning('Establecimiento y punto de emisión deben ser dígitos (máx. 3).'));
      return;
    }
    if (draft.currentNumber == null || draft.currentNumber < 0) {
      this.feedback.set(this.httpFeedback.warning('El número actual no puede ser negativo.'));
      return;
    }
    if (!draft.incrementStep || draft.incrementStep < 1) {
      this.feedback.set(this.httpFeedback.warning('El incremento debe ser mayor o igual a 1.'));
      return;
    }
    const { documentCode: _code, ...payload } = draft;
    const padded: UpsertSequencePayload = {
      ...payload,
      establishmentCode: establishment.padStart(3, '0'),
      emissionPointCode: emission.padStart(3, '0'),
      currentNumber: Number(draft.currentNumber),
      incrementStep: Number(draft.incrementStep),
    };

    this.sequenceSaving.set(true);
    this.erpApi
      .updateSriSequence(padded)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.sequences.update((list) => list.map((s) => (s.id === updated.id ? updated : s)));
          this.sequenceSaving.set(false);
          this.sequenceEditor.set(null);
          this.feedback.set(this.httpFeedback.success(`Secuencia ${updated.documentCode} actualizada.`));
        },
        error: (error) => {
          this.sequenceSaving.set(false);
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo actualizar la secuencia.'));
        },
      });
  }

  protected loadRideStudio(): void {
    this.erpApi
      .getRideTemplate()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (template) => {
          this.rideTemplate.set(template);
          const layout = template?.layout ?? {};
          this.rideLayout.set({
            ...this.rideLayout(),
            ...Object.fromEntries(Object.entries(layout).map(([key, value]) => [key, String(value ?? '')])),
          } as RideLayoutState);
          this.rideBase.set(template?.templateCode && RIDE_TEMPLATE_BODIES[template.templateCode] ? template.templateCode : 'Classic');
        },
        error: (error) => this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo cargar el RIDE Studio.')),
      });
  }

  protected updateRideLayout(key: keyof RideLayoutState, value: string): void {
    this.rideLayout.set({ ...this.rideLayout(), [key]: value });
    this.rideAutoSaveState.set('pending');
    this.ridePreviewChanges.next();
  }

  protected applyRideBase(code: string): void {
    this.rideBase.set(code);
    this.rideAutoSaveState.set('pending');
    this.ridePreviewChanges.next();
  }

  protected resetRideStudio(): void {
    this.rideLayout.set({ ...this.rideDefaultLayout });
    this.rideBase.set('Classic');
    this.logoPreviewUrl.set('');
    this.rideAutoSaveState.set('pending');
    this.ridePreviewChanges.next();
    this.feedback.set(this.httpFeedback.success('Diseño restablecido a los valores por defecto.'));
  }

  protected rideAutoSaveLabel(): string {
    return {
      idle: 'Listo',
      pending: 'Cambios sin guardar',
      saving: 'Guardando…',
      saved: 'Cambios guardados',
      error: 'Error al guardar',
    }[this.rideAutoSaveState()] ?? '';
  }

  protected openLogoCropper(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.logoCroppedBlob.set(null);
    this.logoCropperEvent.set(event);
    this.previewLogoFile(file);
    input.value = '';
  }

  protected onLogoCropped(event: ImageCroppedEvent): void {
    this.logoCroppedBlob.set(event.blob ?? null);
    if (event.blob) {
      this.previewLogoFile(event.blob);
    }
  }

  protected logoLoadFailed(): void {
    this.logoCroppedBlob.set(null);
    this.logoCropperEvent.set(null);
    this.logoPreviewUrl.set('');
    this.feedback.set(this.httpFeedback.warning('No se pudo leer la imagen del logo.'));
  }

  protected cancelLogoCrop(): void {
    this.logoCroppedBlob.set(null);
    this.logoCropperEvent.set(null);
    this.logoPreviewUrl.set('');
  }

  protected uploadCroppedRideLogo(): void {
    const blob = this.logoCroppedBlob();
    if (!blob) {
      this.feedback.set(this.httpFeedback.warning('Recorta el logo antes de subirlo.'));
      return;
    }
    const file = new File([blob], 'ride-logo.png', { type: blob.type || 'image/png' });
    this.erpApi
      .uploadRideLogo(file)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (asset) => {
          this.logoPreviewUrl.set('');
          this.updateRideLayout('logoUrl', asset.publicUrl || asset.path);
          this.cancelLogoCrop();
          this.feedback.set(this.httpFeedback.success('Logo guardado para el RIDE.'));
        },
        error: (error) => this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo subir el logo.')),
      });
  }

  private previewLogoFile(file: Blob): void {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        this.logoPreviewUrl.set(reader.result);
      }
    };
    reader.onerror = () => this.feedback.set(this.httpFeedback.warning('No se pudo generar la vista previa del logo.'));
    reader.readAsDataURL(file);
  }

  protected saveRideStudio(showToast: boolean): void {
    if (this.rideSaving()) {
      this.rideAutoSaveState.set('pending');
      this.ridePreviewChanges.next();
      return;
    }
    this.rideSaving.set(true);
    this.rideAutoSaveState.set('saving');
    const code = this.rideBase();
    this.erpApi
      .saveRideTemplate({
        templateCode: code,
        templateName: `InsightVision ${code}`,
        layout: this.rideLayout(),
        htmlContent: RIDE_TEMPLATE_BODIES[code] ?? RIDE_TEMPLATE_BODIES['Classic'],
        active: true,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (template) => {
          this.rideTemplate.set(template);
          this.rideSaving.set(false);
          this.rideAutoSaveState.set('saved');
          if (showToast) this.feedback.set(this.httpFeedback.success('Plantilla RIDE guardada.'));
        },
        error: (error) => {
          this.rideSaving.set(false);
          this.rideAutoSaveState.set('error');
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo guardar la plantilla RIDE.'));
        },
      });
  }

  protected downloadRide(documentId: number): void {
    this.erpApi
      .downloadRidePdf(documentId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.fileDownload.download(response, `ride-${documentId}.pdf`),
        error: (error) => this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo descargar el RIDE.')),
      });
  }

  protected regenerateRide(documentId: number): void {
    this.erpApi
      .regenerateRide(documentId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.feedback.set(this.httpFeedback.success(`RIDE regenerado. Estado correo: ${result.emailStatus}.`));
          this.reload();
        },
        error: (error) => this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo regenerar el RIDE.')),
      });
  }

  protected selectDocument(documentId: number): void {
    this.selectedDocumentId.set(documentId);
    this.transmissionsLoading.set(true);
    this.erpApi
      .getSriTransmissions(documentId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (transmissions) => {
          this.transmissions.set(transmissions ?? []);
          this.transmissionsLoading.set(false);
        },
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo cargar el historial.'));
          this.transmissionsLoading.set(false);
        },
      });
  }

  protected validate(documentId: number): void {
    this.erpApi
      .validateSriDocument(documentId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.feedback.set(this.httpFeedback.success(`Documento #${documentId} validado.`)); this.reload(); },
        error: (error) => this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo validar.')),
      });
  }

  protected emit(documentId: number): void {
    this.erpApi
      .emitSriDocument(documentId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.feedback.set(this.httpFeedback.success(`Documento #${documentId} enviado.`)); this.reload(); },
        error: (error) => this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo emitir.')),
      });
  }

  protected poll(documentId: number): void {
    this.erpApi
      .pollSriDocumentAuthorization(documentId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.feedback.set(this.httpFeedback.success(`Estado actualizado para #${documentId}.`)); this.reload(); },
        error: (error) => this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo consultar el estado.')),
      });
  }

  protected authorize(documentId: number): void {
    const code = this.manualAuthorizationCode.trim();
    if (!code) {
      this.feedback.set(this.httpFeedback.warning('Ingresa el código de autorización primero.'));
      return;
    }
    this.erpApi
      .authorizeSriDocument(documentId, code)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.feedback.set(this.httpFeedback.success(`Autorización registrada para #${documentId}.`));
          this.manualAuthorizationCode = '';
          this.reload();
        },
        error: (error) => this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo registrar la autorización.')),
      });
  }
}
