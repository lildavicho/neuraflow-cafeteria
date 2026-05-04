/**
 * Diccionario central de labels visibles al usuario.
 * Mantenemos los slugs internos (los que viajan al backend) intactos —
 * solo traducimos lo que se muestra al usuario.
 */

export const MODULE_LABELS: Record<string, string> = {
  // Operación
  pos: 'POS',
  inventory: 'Inventario',
  customers: 'Clientes',
  sales: 'Ventas',
  orders: 'Órdenes',
  cash: 'Caja',
  purchases: 'Compras',

  // Análisis
  'basic-reports': 'Reportes',
  'executive-dashboard': 'Dashboard ejecutivo',
  insights: 'Insights',
  accounting: 'Contabilidad',
  taxation: 'Tributación',
  sri: 'SRI - Facturación electrónica',
  'vision-ai': 'Visión AI',
  footfall: 'Afluencia',
  conversion: 'Conversión',
  'peak-hours': 'Horas pico',
  'traffic-insights': 'Tráfico',
  predictions: 'Predicciones',

  // Catálogo extendido (slugs que reportaba la auditoría)
  approvals: 'Aprobaciones',
  bank: 'Banco',
  branches: 'Sucursales',
  budget: 'Presupuesto',
  crm: 'CRM',
  education: 'Educación',
  'fixed-assets': 'Activos fijos',
  hr: 'Talento humano',
  parties: 'Terceros',
  pricing: 'Precios',
  quotations: 'Cotizaciones',
  restaurant: 'Restaurante',
  returns: 'Devoluciones',
  'sales-orders': 'Pedidos de venta',
  'service-desk': 'Mesa de ayuda',
  withholdings: 'Retenciones',
  warehouses: 'Bodegas',
};

/** Convierte un slug en un label amigable. */
export function moduleLabel(slug: string | null | undefined): string {
  if (!slug) return '';
  if (MODULE_LABELS[slug]) return MODULE_LABELS[slug];
  // Fallback: slug humanizado sin guiones (capitalizado).
  return slug
    .replace(/[_-]+/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
    .trim();
}

export const SRI_DOC_TYPE_LABELS: Record<string, string> = {
  SRI_INVOICE: 'Factura',
  SRI_CREDIT_NOTE: 'Nota de crédito',
  SRI_DEBIT_NOTE: 'Nota de débito',
  SRI_WITHHOLDING: 'Comprobante de retención',
  SRI_DELIVERY_NOTE: 'Guía de remisión',
  SALES_INVOICE: 'Factura',
};

export function sriDocTypeLabel(code: string | null | undefined): string {
  if (!code) return '';
  return SRI_DOC_TYPE_LABELS[code] ?? humanize(code);
}

export const SRI_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Borrador',
  PENDING_VALIDATION: 'Pendiente de validación',
  READY_TO_SEND: 'Listo para enviar',
  SENT: 'Enviado',
  AUTHORIZED: 'Autorizado',
  REJECTED: 'Rechazado',
  CANCELLED: 'Anulado',
  GENERATED: 'Generado',
  RECIBIDA: 'Recibida',
  AUTORIZADO: 'Autorizado',
  'NO AUTORIZADO': 'No autorizado',
  PROCESSING: 'En proceso',
};

export function sriStatusLabel(status: string | null | undefined): string {
  if (!status) return '';
  const upper = status.toUpperCase();
  return SRI_STATUS_LABELS[upper] ?? humanize(status);
}

export const TAX_RULE_LABELS: Record<string, string> = {
  IVA_0: 'IVA 0%',
  IVA_GENERAL: 'IVA general 15%',
  IVA_15: 'IVA 15%',
  IVA_12: 'IVA 12%',
  IVA_LEGACY_12: 'IVA legacy 12%',
  // Bug conocido: backend envía "l" minúscula en lugar de "1"
  IVA_LEGACY_l2: 'IVA legacy 12%',
  IVA_5: 'IVA 5%',
  IVA_EXEMPT: 'IVA exento',
  IVA_NON_TAXABLE: 'No objeto de IVA',
};

export function taxRuleLabel(code: string | null | undefined): string {
  if (!code) return '';
  return TAX_RULE_LABELS[code] ?? humanize(code);
}

export const ORDER_DOCUMENT_TYPE_LABELS: Record<string, string> = {
  NOTA_VENTA: 'Nota de venta',
  FACTURA: 'Factura electrónica',
};

export function orderDocumentTypeLabel(value: string | null | undefined): string {
  if (!value) return 'Nota de venta';
  return ORDER_DOCUMENT_TYPE_LABELS[value] ?? humanize(value);
}

/** Convierte un código tipo SOMETHING_LIKE_THIS en "Something like this". */
function humanize(value: string): string {
  return value
    .replace(/[_-]+/g, ' ')
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
}

/** Etiqueta amigable para el código de negocio (oculta "default"). */
export function tenantDisplayLabel(tenantCode: string | null | undefined): string {
  if (!tenantCode) return 'Negocio principal';
  const trimmed = tenantCode.trim();
  if (!trimmed || trimmed.toLowerCase() === 'default') return 'Negocio principal';
  return trimmed;
}
