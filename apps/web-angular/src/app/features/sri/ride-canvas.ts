import { AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild, signal } from '@angular/core';

export type RideCanvasLayout = {
  headerColor: string;
  accentColor: string;
  borderColor: string;
  fontFamily: string;
  logoUrl: string;
  footerNote: string;
};

export type RideCanvasData = {
  legal: {
    ruc: string;
    razonSocial: string;
    nombreComercial: string;
    dirMatriz: string;
    dirEstablecimiento: string;
    obligadoContabilidad: string;
    contribuyenteEspecial: string;
    contribuyenteRimpe: string;
    agenteRetencion: string;
    ambiente: string;
    ambienteLabel: string;
    tipoEmision: string;
  };
  documento: {
    version: string;
    codDoc: string;
    tipo: string;
    estab: string;
    ptoEmi: string;
    secuencial: string;
    numero: string;
    fechaEmision: string;
    autorizacion: string;
    fechaAutorizacion: string;
    estado: string;
  };
  cliente: {
    nombre: string;
    identificacion: string;
    tipoIdentificacion: string;
    direccion: string;
    email: string;
  };
  detalles: Array<{
    codigo: string;
    codigoAuxiliar: string;
    descripcion: string;
    cantidad: string;
    precioUnitario: string;
    descuento: string;
    subtotal: string;
    ivaCodigo: string;
    ivaCodigoPorcentaje: string;
    ivaTarifa: string;
    ivaBase: string;
    ivaValor: string;
  }>;
  totales: {
    subtotal: string;
    descuento: string;
    iva15: string;
    impuestosTotal: string;
    propina: string;
    total: string;
    moneda: string;
    pagos: Array<{
      formaPago: string;
      total: string;
      plazo: string;
      unidadTiempo: string;
    }>;
  };
  seguridad: {
    claveAcceso: string;
  };
  leyendas: string[];
  infoAdicional: Array<{ nombre: string; valor: string }>;
};

@Component({
  selector: 'app-ride-canvas',
  standalone: true,
  template: `
    <section class="preview-frame canvas-wrapper" #viewport>
      <div class="canvas-stage" [style.width.px]="scaledWidth()" [style.height.px]="scaledHeight()">
        <article
          #page
          [class]="'a4-sheet a4-paper ride-a4 ride-a4--' + normalizedVariant()"
          [style.--sri-primary]="layout.headerColor"
          [style.--sri-accent]="layout.accentColor"
          [style.--sri-border]="layout.borderColor"
          [style.--sri-font]="layout.fontFamily"
          [style.transform]="'translateX(-50%) scale(' + scale() + ')'"
        >
          <header class="ride-header">
            <div class="ride-brand">
              <div class="ride-logo-box">
                @if (layout.logoUrl) {
                  <img [src]="layout.logoUrl" alt="Logo del negocio" />
                } @else {
                  <span>InsightVision</span>
                }
              </div>
              <div class="ride-brand-copy">
                <p class="ride-overline">Representación impresa del comprobante electrónico</p>
                <h1>{{ data.legal.razonSocial }}</h1>
                <strong>{{ data.legal.nombreComercial }}</strong>
                <span>Dirección matriz: {{ data.legal.dirMatriz }}</span>
                <span>Dirección establecimiento: {{ data.legal.dirEstablecimiento }}</span>
              </div>
            </div>

            <aside class="sri-card ride-legal-card">
              <div class="ride-legal-card__top">
                <span class="ride-doc-type">{{ data.documento.tipo }}</span>
                <div class="ride-legal-grid">
                  <span>RUC</span><strong>{{ data.legal.ruc }}</strong>
                  <span>No.</span><strong>{{ data.documento.numero }}</strong>
                  <span>Ambiente</span><strong>{{ data.legal.ambienteLabel }}</strong>
                  <span>Tipo emisión</span><strong>{{ data.legal.tipoEmision }}</strong>
                  <span>Obligado contabilidad</span><strong>{{ data.legal.obligadoContabilidad }}</strong>
                </div>
              </div>
              <div class="ride-security">
                <div>
                  <span class="ride-label">Clave de acceso</span>
                  <strong class="ride-access-key">{{ data.seguridad.claveAcceso }}</strong>
                  <small>QR actualizado desde la clave de acceso</small>
                </div>
                <svg class="ride-qr" [attr.viewBox]="qrViewBox()" role="img" aria-label="Código QR de la clave de acceso">
                  <rect class="ride-qr__bg" x="-3" y="-3" width="31" height="31"></rect>
                  <path class="ride-qr__path" [attr.d]="qrPath()"></path>
                </svg>
              </div>
            </aside>
          </header>

          <div class="ride-legend-row">
            @for (legend of data.leyendas; track legend) {
              <span>{{ legend }}</span>
            }
          </div>

          <section class="ride-schema-grid">
            <article class="ride-schema-card">
              <div class="ride-schema-title"><span>infoTributaria</span><strong>Datos del emisor</strong></div>
              <div class="ride-schema-fields">
                <span>ambiente</span><strong>{{ data.legal.ambiente }}</strong>
                <span>tipoEmision</span><strong>{{ data.legal.tipoEmision }}</strong>
                <span>razonSocial</span><strong>{{ data.legal.razonSocial }}</strong>
                <span>nombreComercial</span><strong>{{ data.legal.nombreComercial }}</strong>
                <span>ruc</span><strong>{{ data.legal.ruc }}</strong>
                <span>codDoc</span><strong>{{ data.documento.codDoc }}</strong>
                <span>estab / ptoEmi</span><strong>{{ data.documento.estab }}-{{ data.documento.ptoEmi }}</strong>
                <span>secuencial</span><strong>{{ data.documento.secuencial }}</strong>
                <span>dirMatriz</span><strong>{{ data.legal.dirMatriz }}</strong>
              </div>
            </article>

            <article class="ride-schema-card">
              <div class="ride-schema-title"><span>infoFactura</span><strong>Cliente y totales</strong></div>
              <div class="ride-schema-fields">
                <span>fechaEmision</span><strong>{{ data.documento.fechaEmision }}</strong>
                <span>dirEstablecimiento</span><strong>{{ data.legal.dirEstablecimiento }}</strong>
                <span>contribuyenteEspecial</span><strong>{{ data.legal.contribuyenteEspecial }}</strong>
                <span>obligadoContabilidad</span><strong>{{ data.legal.obligadoContabilidad }}</strong>
                <span>tipoIdentificación</span><strong>{{ data.cliente.tipoIdentificacion }}</strong>
                <span>razón social comprador</span><strong>{{ data.cliente.nombre }}</strong>
                <span>identificación comprador</span><strong>{{ data.cliente.identificacion }}</strong>
                <span>importeTotal</span><strong>{{ data.totales.total }}</strong>
              </div>
            </article>
          </section>

          <section class="ride-section">
            <div class="ride-section-title">
              <span>detalles</span>
              <strong>Productos / servicios con impuesto por línea</strong>
            </div>
            <div class="ride-detail-table">
              <div class="ride-detail-table__head">
                <span>Cód. principal</span>
                <span>Cód. auxiliar</span>
                <span>Descripción</span>
                <span>Cant.</span>
                <span>Unitario</span>
                <span>Desc.</span>
                <span>Total sin impuesto</span>
                <span>IVA cód.</span>
                <span>Base / valor</span>
              </div>
              @for (item of data.detalles; track item.codigo + item.descripcion) {
                <div class="ride-detail-table__row">
                  <span>{{ item.codigo }}</span>
                  <span>{{ item.codigoAuxiliar }}</span>
                  <strong>{{ item.descripcion }}</strong>
                  <span>{{ item.cantidad }}</span>
                  <span>{{ item.precioUnitario }}</span>
                  <span>{{ item.descuento }}</span>
                  <span>{{ item.subtotal }}</span>
                  <span>{{ item.ivaCodigo }}/{{ item.ivaCodigoPorcentaje }} · {{ item.ivaTarifa }}%</span>
                  <span>{{ item.ivaBase }} / {{ item.ivaValor }}</span>
                </div>
              }
            </div>
          </section>

          <section class="ride-compliance-grid">
            <article class="ride-schema-card">
              <div class="ride-schema-title"><span>totalConImpuestos</span><strong>Resumen IVA</strong></div>
              <div class="ride-schema-fields">
                <span>código</span><strong>2</strong>
                <span>codigoPorcentaje</span><strong>4</strong>
                <span>baseImponible</span><strong>{{ data.totales.subtotal }}</strong>
                <span>tarifa</span><strong>15.00</strong>
                <span>valor</span><strong>{{ data.totales.iva15 }}</strong>
              </div>
            </article>

            <article class="ride-schema-card">
              <div class="ride-schema-title"><span>pagos</span><strong>Forma de pago SRI</strong></div>
              @for (payment of data.totales.pagos; track payment.formaPago) {
                <div class="ride-schema-fields ride-schema-fields--payment">
                  <span>formaPago</span><strong>{{ payment.formaPago }}</strong>
                  <span>total</span><strong>{{ payment.total }}</strong>
                  <span>plazo</span><strong>{{ payment.plazo }}</strong>
                  <span>unidadTiempo</span><strong>{{ payment.unidadTiempo }}</strong>
                </div>
              }
            </article>

            <aside class="ride-totals">
              <div><span>totalSinImpuestos</span><strong>{{ data.totales.subtotal }}</strong></div>
              <div><span>totalDescuento</span><strong>{{ data.totales.descuento }}</strong></div>
              <div class="ride-totals__tax"><span>IVA 15% código 4</span><strong>{{ data.totales.iva15 }}</strong></div>
              <div><span>propina</span><strong>{{ data.totales.propina }}</strong></div>
              <div class="total-box ride-totals__grand">
                <span>Total a pagar</span>
                <strong>{{ data.totales.total }}</strong>
                <small>{{ data.totales.moneda }}</small>
              </div>
            </aside>
          </section>

          <section class="ride-bottom">
            <div class="ride-additional">
              <span class="ride-label">infoAdicional</span>
              @for (field of data.infoAdicional; track field.nombre) {
                <p><strong>{{ field.nombre }}:</strong> {{ field.valor }}</p>
              }
              <p class="ride-footer-note">{{ layout.footerNote }}</p>
            </div>
          </section>
        </article>
      </div>
    </section>
  `,
  styles: [`
    @font-face { font-family: 'Montserrat'; src: url('/fonts/Montserrat-Regular.ttf') format('truetype'); font-weight: 400 900; font-style: normal; font-display: swap; }
    @font-face { font-family: 'Inter'; src: url('/fonts/Inter-Regular.ttf') format('truetype'); font-weight: 400 900; font-style: normal; font-display: swap; }

    :host {
      display: block;
      min-width: 0;
    }

    .preview-frame,
    .canvas-wrapper {
      height: min(82vh, 880px);
      min-height: 620px;
      background: #f1f5f9;
      overflow: hidden;
      display: flex;
      justify-content: center;
      align-items: flex-start;
      padding: 2rem 1rem 0;
      border: 1px solid #d8e0ea;
      border-radius: 8px;
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78);
    }

    .canvas-stage {
      position: relative;
      flex: 0 0 auto;
    }

    .a4-sheet,
    .a4-paper {
      position: absolute;
      top: 0;
      left: 50%;
      width: 210mm;
      height: 297mm;
      background: white;
      border-radius: 4px;
      transform-origin: top center;
      box-shadow: 0 22px 52px rgba(15, 23, 42, 0.14), 0 6px 18px rgba(15, 23, 42, 0.06);
      transition: transform 0.3s ease;
      border: none;
    }

    .ride-a4 {
      position: relative;
      color: #1d2430;
      font-family: var(--sri-font), Inter, sans-serif;
      font-size: 10.2px;
      line-height: 1.34;
      padding: 10mm 11mm 9mm;
      overflow: hidden;
      background: #ffffff;
    }

    .ride-a4::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 4mm;
      background: var(--sri-primary);
    }

    .ride-header {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 72mm;
      gap: 7mm;
      align-items: flex-start;
      padding: 0 0 7mm;
      border-bottom: 1px solid #e2e8f0;
    }

    .ride-brand {
      display: grid;
      grid-template-columns: 50mm minmax(0, 1fr);
      gap: 8mm;
      align-items: start;
      min-width: 0;
    }

    .ride-logo-box {
      width: 50mm;
      height: 21mm;
      display: flex;
      align-items: flex-start;
      justify-content: flex-start;
      overflow: hidden;
      background: #ffffff;
      border-radius: 4px;
    }

    .ride-logo-box img {
      max-width: 50mm;
      max-height: 21mm;
      width: 100%;
      height: 100%;
      object-fit: contain;
      object-position: left top;
      display: block;
      align-self: flex-start;
    }

    .ride-logo-box span {
      color: var(--sri-primary);
      font-family: Montserrat, sans-serif;
      font-size: 16px;
      font-weight: 900;
      letter-spacing: 0;
      align-self: center;
    }

    .ride-brand-copy {
      display: grid;
      gap: 2.5px;
      min-width: 0;
    }

    .ride-overline,
    .ride-label {
      margin: 0;
      color: #64748b;
      font-size: 7.5pt;
      font-weight: 800;
      letter-spacing: 0;
      text-transform: uppercase;
    }

    .ride-brand-copy h1 {
      margin: 0;
      max-width: 100%;
      color: #111827;
      font-family: Montserrat, sans-serif;
      font-size: 22px;
      line-height: 1.12;
      font-weight: 900;
      letter-spacing: 0;
      text-transform: uppercase;
      overflow-wrap: anywhere;
    }

    .ride-brand-copy strong {
      color: var(--sri-primary);
      font-size: 11px;
      font-weight: 900;
    }

    .ride-brand-copy span {
      color: #64748b;
      font-size: 7.8pt;
      overflow-wrap: anywhere;
    }

    .sri-card,
    .ride-legal-card {
      position: relative;
      width: 72mm;
      min-width: 0;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-left: 6px solid var(--sri-primary);
      border-radius: 8px;
      padding: 13px 14px;
      box-shadow: 0 12px 26px rgba(15, 23, 42, 0.06);
    }

    .ride-legal-card__top,
    .ride-security {
      position: relative;
      z-index: 1;
    }

    .ride-doc-type {
      display: block;
      margin-bottom: 8px;
      color: var(--sri-primary);
      font-family: Montserrat, sans-serif;
      font-size: 17px;
      font-weight: 900;
      text-transform: uppercase;
    }

    .ride-legal-grid,
    .ride-facts {
      display: grid;
      grid-template-columns: auto minmax(0, 1fr);
      gap: 3px 9px;
    }

    .ride-legal-grid span,
    .ride-facts span {
      color: #64748b;
      font-size: 7.4pt;
      text-transform: uppercase;
    }

    .ride-legal-grid strong,
    .ride-facts strong {
      color: #111827;
      text-align: right;
      overflow-wrap: anywhere;
    }

    .ride-security {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 24mm;
      gap: 9px;
      align-items: end;
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px solid #e2e8f0;
    }

    .ride-access-key {
      display: block;
      margin-top: 4px;
      font-family: 'Courier New', monospace;
      font-size: 8.5px;
      line-height: 1.35;
      color: #111827;
      overflow-wrap: anywhere;
    }

    .ride-security small {
      display: block;
      margin-top: 3px;
      color: #64748b;
      font-size: 7.2px;
      font-weight: 800;
    }

    .ride-qr {
      width: 24mm;
      height: 24mm;
      display: block;
      background: #ffffff;
      border: 1px solid #d7dbe1;
      padding: 3px;
      border-radius: 4px;
    }

    .ride-qr__bg {
      fill: #ffffff;
    }

    .ride-qr__path {
      fill: #111827;
      shape-rendering: crispEdges;
    }

    .ride-legend-row {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 5mm;
    }

    .ride-legend-row span {
      color: var(--sri-primary);
      background: #ffffff;
      border: 1px solid color-mix(in srgb, var(--sri-primary) 24%, #ffffff);
      border-radius: 999px;
      padding: 3px 8px;
      font-size: 8.4px;
      font-weight: 900;
      text-transform: uppercase;
    }

    .ride-schema-grid,
    .ride-compliance-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 5mm;
      margin-top: 5mm;
      align-items: stretch;
    }

    .ride-compliance-grid {
      grid-template-columns: minmax(0, 0.95fr) minmax(0, 0.95fr) 56mm;
      align-items: start;
    }

    .ride-schema-card {
      min-width: 0;
      padding: 4mm;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
    }

    .ride-schema-title,
    .ride-section-title {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      gap: 8px;
      margin-bottom: 3mm;
      border-bottom: 1px solid #e2e8f0;
      padding-bottom: 2mm;
    }

    .ride-schema-title span,
    .ride-section-title span {
      color: var(--sri-primary);
      font-family: Montserrat, sans-serif;
      font-size: 8px;
      font-weight: 900;
      letter-spacing: 0;
      text-transform: none;
    }

    .ride-schema-title strong,
    .ride-section-title strong {
      color: #64748b;
      font-size: 7.6px;
      font-weight: 900;
      text-transform: uppercase;
    }

    .ride-schema-fields {
      display: grid;
      grid-template-columns: 30mm minmax(0, 1fr);
      gap: 2px 7px;
      min-width: 0;
    }

    .ride-schema-fields--payment {
      padding-top: 1mm;
    }

    .ride-schema-fields span {
      color: #64748b;
      font-size: 7.5px;
      font-weight: 800;
      overflow-wrap: anywhere;
    }

    .ride-schema-fields strong {
      color: #111827;
      font-size: 8px;
      text-align: right;
      overflow-wrap: anywhere;
    }

    .ride-detail-table {
      display: grid;
      border: 1px solid #e2e8f0;
      border-radius: 7px;
      overflow: hidden;
      box-shadow: 0 6px 18px rgba(15, 23, 42, 0.035);
    }

    .ride-detail-table__head,
    .ride-detail-table__row {
      display: grid;
      grid-template-columns: 18mm 18mm minmax(0, 1.25fr) 12mm 17mm 14mm 20mm 20mm 22mm;
      align-items: center;
    }

    .ride-detail-table__head {
      background: var(--sri-primary);
      color: #fff;
      font-family: Montserrat, sans-serif;
      font-size: 7.1px;
      font-weight: 900;
      text-transform: uppercase;
    }

    .ride-detail-table__head span,
    .ride-detail-table__row span,
    .ride-detail-table__row strong {
      min-width: 0;
      padding: 5px 5px;
      overflow-wrap: anywhere;
    }

    .ride-detail-table__row {
      background: #fff;
      border-bottom: 1px solid #e2e8f0;
      color: #334155;
      font-size: 8px;
    }

    .ride-detail-table__row:nth-child(odd) {
      background: #fbfdff;
    }

    .ride-detail-table__row strong {
      color: #111827;
      font-weight: 900;
    }

    .ride-section {
      margin-top: 6.5mm;
    }

    .ride-section--client {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 72mm;
      gap: 8mm;
      align-items: start;
      padding: 5mm 6mm;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-left: 4px solid var(--sri-primary);
      border-radius: 8px;
    }

    .ride-section h2 {
      margin: 2px 0 0;
      color: #111827;
      font-family: Montserrat, sans-serif;
      font-size: 16px;
      line-height: 1.2;
      font-weight: 900;
      overflow-wrap: anywhere;
    }

    .ride-table {
      border-collapse: collapse;
      width: 100%;
      overflow: hidden;
      border-radius: 6px;
      border: 1px solid #e2e8f0;
      box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
    }

    .ride-table__head,
    .ride-table__row {
      display: grid;
      grid-template-columns: 22mm minmax(0, 1fr) 15mm 19mm 19mm 21mm;
      align-items: center;
      gap: 0;
    }

    .ride-table__head {
      background: var(--sri-primary);
      color: white;
      border-radius: 6px 6px 0 0;
      font-family: Montserrat, sans-serif;
      font-size: 8.6px;
      font-weight: 900;
      text-transform: uppercase;
    }

    .ride-table__head span,
    .ride-table__row span,
    .ride-table__row strong {
      padding: 7px 8px;
      min-width: 0;
      overflow-wrap: anywhere;
    }

    .ride-table__row {
      color: #334155;
      border-bottom: 1px solid #e2e8f0;
      background: #ffffff;
    }

    .ride-table__row:nth-child(odd) {
      background: #fbfdff;
    }

    .ride-table__row strong {
      color: #111827;
      font-weight: 800;
    }

    .ride-bottom {
      display: grid;
      grid-template-columns: minmax(0, 1fr);
      gap: 9mm;
      align-items: end;
      margin-top: 5mm;
    }

    .ride-additional {
      color: #475569;
      border-top: 1px solid #e2e8f0;
      padding-top: 4mm;
    }

    .ride-additional p {
      margin: 4px 0;
    }

    .ride-footer-note {
      margin-top: 8mm !important;
      color: #64748b;
      font-size: 9.5px;
    }

    .ride-totals {
      float: right;
      min-width: 0;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      overflow: hidden;
      background: #ffffff;
      box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
    }

    .ride-totals div {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      padding: 5px 8px;
      border-bottom: 1px solid #e2e8f0;
      color: #475569;
    }

    .ride-totals div strong {
      color: #111827;
      font-weight: 900;
    }

    .ride-totals__tax {
      color: var(--sri-primary) !important;
      font-weight: 900;
    }

    .ride-totals__tax strong {
      color: var(--sri-primary) !important;
    }

    .total-box,
    .ride-totals__grand {
      display: grid !important;
      grid-template-columns: 1fr auto;
      align-items: end;
      background: var(--sri-primary);
      color: white !important;
      border-bottom: 0 !important;
      padding: 9px 10px !important;
      border-radius: 0;
    }

    .ride-totals__grand span {
      font-size: 9.4px;
      font-weight: 900;
      text-transform: uppercase;
    }

    .total-box strong,
    .ride-totals__grand strong {
      color: white !important;
      font-family: Montserrat, sans-serif;
      font-size: 18pt;
      line-height: 1;
      font-weight: 900;
    }

    .ride-totals__grand small {
      grid-column: 1 / -1;
      color: rgba(255, 255, 255, 0.78);
      font-size: 8.5px;
      text-align: right;
    }

    .ride-a4--modern .sri-card,
    .ride-a4--modern .ride-legal-card {
      background: color-mix(in srgb, var(--sri-primary) 8%, #ffffff);
    }

    .ride-a4--minimal .ride-logo-box {
      background: transparent;
    }
  `],
})
export class RideCanvas implements AfterViewInit, OnDestroy {
  @Input({ required: true }) layout!: RideCanvasLayout;
  @Input({ required: true }) data!: RideCanvasData;
  @Input() variant = 'Classic';

  @ViewChild('viewport', { static: true }) private readonly viewportRef!: ElementRef<HTMLElement>;
  @ViewChild('page', { static: true }) private readonly pageRef!: ElementRef<HTMLElement>;

  protected readonly scale = signal(1);
  protected readonly scaledWidth = signal(0);
  protected readonly scaledHeight = signal(0);

  private readonly qrSize = 25;
  private resizeObserver?: ResizeObserver;
  private qrCacheKey = '';
  private qrPathCache = '';

  ngAfterViewInit(): void {
    this.resizeObserver = new ResizeObserver(() => this.fitPage());
    this.resizeObserver.observe(this.viewportRef.nativeElement);
    queueMicrotask(() => this.fitPage());
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
  }

  protected normalizedVariant(): string {
    return (this.variant || 'Classic').toLowerCase();
  }

  protected qrViewBox(): string {
    return `-3 -3 ${this.qrSize + 6} ${this.qrSize + 6}`;
  }

  protected qrPath(): string {
    const value = this.data?.seguridad?.claveAcceso || 'INSIGHTVISION';
    if (value === this.qrCacheKey && this.qrPathCache) {
      return this.qrPathCache;
    }
    const matrix = this.buildQrMatrix(value);
    this.qrCacheKey = value;
    this.qrPathCache = this.matrixToSvgPath(matrix);
    return this.qrPathCache;
  }

  private fitPage(): void {
    const viewport = this.viewportRef.nativeElement;
    const page = this.pageRef.nativeElement;
    const pageWidth = page.offsetWidth || 794;
    const pageHeight = page.offsetHeight || 1123;
    const availableHeight = Math.max(1, viewport.clientHeight - 32);
    const wrapperHeightScale = Math.max(0.28, availableHeight / pageHeight);
    const widthScale = Math.max(0.28, (viewport.clientWidth - 32) / pageWidth);
    const nextScale = Math.min(wrapperHeightScale, widthScale, 1);
    this.scale.set(nextScale);
    this.scaledWidth.set(pageWidth * nextScale);
    this.scaledHeight.set(pageHeight * nextScale);
  }

  private buildQrMatrix(rawValue: string): boolean[][] {
    const value = /^\d+$/.test(rawValue) ? rawValue : this.toNumericFallback(rawValue);
    const size = this.qrSize;
    const modules: Array<Array<boolean | null>> = Array.from({ length: size }, () => Array(size).fill(null));
    const reserved: boolean[][] = Array.from({ length: size }, () => Array(size).fill(false));
    const set = (row: number, col: number, dark: boolean, reserve = true): void => {
      if (row < 0 || row >= size || col < 0 || col >= size) return;
      modules[row][col] = dark;
      if (reserve) reserved[row][col] = true;
    };

    this.drawFinder(set, 0, 0);
    this.drawFinder(set, 0, size - 7);
    this.drawFinder(set, size - 7, 0);
    this.drawAlignment(set, 18, 18);
    for (let i = 8; i < size - 8; i += 1) {
      set(6, i, i % 2 === 0);
      set(i, 6, i % 2 === 0);
    }
    set(size - 8, 8, true);
    this.reserveFormatAreas(reserved);

    const data = this.encodeNumericQrData(value.slice(0, 77));
    const codewords = [...data, ...this.reedSolomonRemainder(data, 16)];
    const bits = codewords.flatMap((byte) => this.intBits(byte, 8));
    let bitIndex = 0;
    let upward = true;
    for (let col = size - 1; col >= 1; col -= 2) {
      if (col === 6) col -= 1;
      for (let i = 0; i < size; i += 1) {
        const row = upward ? size - 1 - i : i;
        for (let offset = 0; offset < 2; offset += 1) {
          const currentCol = col - offset;
          if (reserved[row][currentCol]) continue;
          let bit = bitIndex < bits.length ? bits[bitIndex] : false;
          bitIndex += 1;
          if ((row + currentCol) % 2 === 0) bit = !bit;
          modules[row][currentCol] = bit;
        }
      }
      upward = !upward;
    }

    this.drawFormatBits(set, size);
    return modules.map((row) => row.map((cell) => cell === true));
  }

  private drawFinder(set: (row: number, col: number, dark: boolean, reserve?: boolean) => void, row: number, col: number): void {
    for (let y = -1; y <= 7; y += 1) {
      for (let x = -1; x <= 7; x += 1) {
        const distance = Math.max(Math.abs(x - 3), Math.abs(y - 3));
        set(row + y, col + x, distance === 3 || distance <= 1);
      }
    }
  }

  private drawAlignment(set: (row: number, col: number, dark: boolean, reserve?: boolean) => void, row: number, col: number): void {
    for (let y = -2; y <= 2; y += 1) {
      for (let x = -2; x <= 2; x += 1) {
        const distance = Math.max(Math.abs(x), Math.abs(y));
        set(row + y, col + x, distance !== 1);
      }
    }
  }

  private reserveFormatAreas(reserved: boolean[][]): void {
    const size = this.qrSize;
    for (let i = 0; i < 9; i += 1) {
      if (i !== 6) {
        reserved[8][i] = true;
        reserved[i][8] = true;
      }
      reserved[8][size - 1 - i] = true;
      reserved[size - 1 - i][8] = true;
    }
  }

  private drawFormatBits(set: (row: number, col: number, dark: boolean, reserve?: boolean) => void, size: number): void {
    const bits = this.formatBitsForMaskZero();
    const bit = (index: number) => ((bits >>> index) & 1) !== 0;
    for (let i = 0; i <= 5; i += 1) set(i, 8, bit(i));
    set(7, 8, bit(6));
    set(8, 8, bit(7));
    set(8, 7, bit(8));
    for (let i = 9; i < 15; i += 1) set(8, 14 - i, bit(i));
    for (let i = 0; i < 8; i += 1) set(8, size - 1 - i, bit(i));
    for (let i = 8; i < 15; i += 1) set(size - 15 + i, 8, bit(i));
    set(size - 8, 8, true);
  }

  private formatBitsForMaskZero(): number {
    const data = 0; // EC level M + mask pattern 0
    let remainder = data << 10;
    for (let i = 14; i >= 10; i -= 1) {
      if (((remainder >>> i) & 1) !== 0) {
        remainder ^= 0x537 << (i - 10);
      }
    }
    return ((data << 10) | remainder) ^ 0x5412;
  }

  private encodeNumericQrData(value: string): number[] {
    const bits: boolean[] = [];
    this.appendBits(bits, 0x1, 4);
    this.appendBits(bits, value.length, 10);
    for (let index = 0; index < value.length; index += 3) {
      const chunk = value.slice(index, index + 3);
      this.appendBits(bits, Number(chunk), chunk.length === 3 ? 10 : chunk.length === 2 ? 7 : 4);
    }
    const dataCapacityBits = 28 * 8;
    this.appendBits(bits, 0, Math.min(4, dataCapacityBits - bits.length));
    while (bits.length % 8 !== 0) this.appendBits(bits, 0, 1);
    const bytes: number[] = [];
    for (let i = 0; i < bits.length; i += 8) {
      bytes.push(bits.slice(i, i + 8).reduce((valueByte, bit) => (valueByte << 1) | (bit ? 1 : 0), 0));
    }
    for (let pad = 0xec; bytes.length < 28; pad = pad === 0xec ? 0x11 : 0xec) {
      bytes.push(pad);
    }
    return bytes;
  }

  private appendBits(target: boolean[], value: number, length: number): void {
    for (let i = length - 1; i >= 0; i -= 1) {
      target.push(((value >>> i) & 1) !== 0);
    }
  }

  private intBits(value: number, length: number): boolean[] {
    const bits: boolean[] = [];
    this.appendBits(bits, value, length);
    return bits;
  }

  private reedSolomonRemainder(data: number[], degree: number): number[] {
    const generator = this.reedSolomonGenerator(degree);
    const result = Array(degree).fill(0);
    for (const byte of data) {
      const factor = byte ^ result.shift()!;
      result.push(0);
      for (let i = 0; i < degree; i += 1) {
        result[i] ^= this.gfMultiply(generator[i + 1], factor);
      }
    }
    return result;
  }

  private reedSolomonGenerator(degree: number): number[] {
    let result = [1];
    for (let i = 0; i < degree; i += 1) {
      result = this.polyMultiply(result, [1, this.gfPow(i)]);
    }
    return result;
  }

  private polyMultiply(left: number[], right: number[]): number[] {
    const result = Array(left.length + right.length - 1).fill(0);
    for (let i = 0; i < left.length; i += 1) {
      for (let j = 0; j < right.length; j += 1) {
        result[i + j] ^= this.gfMultiply(left[i], right[j]);
      }
    }
    return result;
  }

  private gfMultiply(left: number, right: number): number {
    let result = 0;
    for (let i = 7; i >= 0; i -= 1) {
      result = (result << 1) ^ ((result >>> 7) * 0x11d);
      if (((right >>> i) & 1) !== 0) result ^= left;
    }
    return result & 0xff;
  }

  private gfPow(power: number): number {
    let value = 1;
    for (let i = 0; i < power; i += 1) {
      value = this.gfMultiply(value, 2);
    }
    return value;
  }

  private matrixToSvgPath(matrix: boolean[][]): string {
    const commands: string[] = [];
    for (let row = 0; row < matrix.length; row += 1) {
      for (let col = 0; col < matrix[row].length; col += 1) {
        if (matrix[row][col]) commands.push(`M${col},${row}h1v1h-1z`);
      }
    }
    return commands.join('');
  }

  private toNumericFallback(value: string): string {
    return Array.from(value)
      .map((char) => char.charCodeAt(0).toString().padStart(3, '0'))
      .join('')
      .slice(0, 77);
  }
}
