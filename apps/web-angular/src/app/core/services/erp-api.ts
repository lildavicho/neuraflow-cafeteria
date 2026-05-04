import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, shareReplay, throwError } from 'rxjs';
import { API_BASE_URL } from '../api-base';

export type ProductDto = {
  id: number;
  name: string;
  code?: string;
  price: number;
  purchasePrice?: number;
  stock?: number;
  reservedStock?: number;
  availableStock?: number;
  minStock?: number;
  status?: string;
  categoryName?: string;
  unit?: string;
  lowStock?: boolean;
};

export type StockMovementDto = {
  id: number;
  productId: number;
  productName: string;
  movementType: string;
  quantity: number;
  stockBefore: number;
  stockAfter: number;
  reason?: string;
  referenceType?: string;
  referenceId?: number;
  userName?: string;
  createdAt: string;
};

export type OrderItemDto = {
  id?: number;
  productId: number;
  productName: string;
  productCode?: string;
  quantity: number;
  unitPrice: number;
  lineTotal?: number;
};

export type OrderDocumentType = 'NOTA_VENTA' | 'FACTURA';

export type OrderDto = {
  id: number;
  status: string;
  transactionStatus: string;
  paymentMethod?: string;
  paymentReference?: string;
  paymentBreakdownJson?: string;
  paymentStatus?: string;
  inventoryStatus?: string;
  documentType?: OrderDocumentType;
  subtotal: number;
  tax: number;
  total: number;
  notes?: string;
  customerName?: string;
  customerEmail?: string;
  customerIdentification?: string;
  customerIdentificationType?: string;
  customerAddress?: string;
  customerPhone?: string;
  userName?: string;
  userEmail?: string;
  createdAt: string;
  paidAt?: string;
  cancelledAt?: string;
  refundedAt?: string;
  items: OrderItemDto[];
};

export type PageDto<T> = {
  content: T[];
  totalElements: number;
  totalPages?: number;
  number?: number;
  size?: number;
  page?: {
    totalElements?: number;
    totalPages?: number;
    number?: number;
    size?: number;
  };
};

export type DashboardSnapshot = {
  ventasHoy: number;
  ventasAyer: number;
  gananciasHoy: number;
  gananciasAyer: number;
  ordenesHoy: number;
  ticketPromedio: number;
  comparacionVentas: {
    actual: number;
    anterior: number;
    deltaMonto: number;
    deltaPorcentaje: number;
    tendencia: string;
  };
  comparacionGanancias: {
    actual: number;
    anterior: number;
    deltaMonto: number;
    deltaPorcentaje: number;
    tendencia: string;
  };
  resumenNegocio: {
    status: string;
    title: string;
    detail: string;
  };
  historicoVentas: Array<{
    date: string;
    value: number;
  }>;
  historicoGanancias: Array<{
    date: string;
    value: number;
  }>;
  productosTop: Array<{
    id: number;
    name: string;
    quantity: number;
    revenue: number;
    profit: number;
  }>;
  productosBajoStock: Array<{
    id: number;
    name: string;
    availableStock: number;
    minStock: number;
  }>;
  stockCriticoCount: number;
  cuentasPorCobrarAbiertas: number;
  cuentasPorPagarAbiertas: number;
  documentosSriPendientes: number;
  documentosSriRecientes: Array<{
    id: number;
    status: string;
    documentCode: string;
    sequentialNumber?: string;
    authorizationCode?: string;
    total: number;
    issueDate: string;
    lastStatusAt?: string;
  }>;
  pagosTransferenciaPendientes: Array<{
    id: number;
    userName?: string;
    userEmail?: string;
    total: number;
    paymentReference?: string;
    createdAt: string;
  }>;
  insights: Array<{
    title: string;
    explanation: string;
    impact: string;
    actionRecommended: string;
    tone: string;
    category: string;
  }>;
  alertas: Array<{
    level: string;
    title: string;
    detail: string;
  }>;
  generatedAt: string;
};

export type PurchaseDto = {
  id: number;
  supplierName: string;
  supplierIdentification?: string;
  supplierEmail?: string;
  externalDocumentNumber?: string;
  status: string;
  subtotal: number;
  tax: number;
  total: number;
  dueDate?: string;
  receivedAt?: string;
  paidAt?: string;
  createdAt: string;
  notes?: string;
  payableId?: number;
  items: Array<{
    id: number;
    productId: number;
    productName: string;
    quantity: number;
    unitCost: number;
    lineSubtotal: number;
    taxAmount: number;
    lineTotal: number;
  }>;
};

export type AccountDto = {
  id: number;
  accountCode: string;
  accountName: string;
  accountType: string;
  accountNature: string;
  treeLevel: number;
  allowPosting: boolean;
  active: boolean;
};

export type TaxRuleDto = {
  id: number;
  taxCode: string;
  taxName: string;
  taxScope: string;
  computationType: string;
  rate: number;
  includedInPrice: boolean;
  active: boolean;
};

export type JournalEntryDto = {
  id: number;
  entryNumber: string;
  entryDate: string;
  status: string;
  sourceModule: string;
  sourceType: string;
  sourceId: number;
  description: string;
  externalReference?: string;
  postedAt?: string;
  lines: Array<{
    lineNumber: number;
    accountId: number;
    taxRuleId?: number;
    debit: number;
    credit: number;
    description?: string;
  }>;
};

export type ReceivableDto = {
  id: number;
  customerId?: number;
  sourceDocumentType: string;
  sourceDocumentId: number;
  issueDate: string;
  dueDate?: string;
  totalAmount: number;
  paidAmount: number;
  balance: number;
  status: string;
};

export type PayableDto = {
  id: number;
  supplierId?: number;
  sourceDocumentType: string;
  sourceDocumentId: number;
  issueDate: string;
  dueDate?: string;
  totalAmount: number;
  paidAmount: number;
  balance: number;
  status: string;
};

export type SequenceDto = {
  id: number;
  documentTypeId: number;
  documentCode: string;
  establishmentCode: string;
  emissionPointCode: string;
  currentNumber: number;
  incrementStep: number;
  active: boolean;
};

export type UpsertSequencePayload = {
  id?: number;
  documentTypeId: number;
  establishmentCode: string;
  emissionPointCode: string;
  currentNumber: number;
  incrementStep: number;
  active: boolean;
};

export type AtsPeriodDto = {
  id: number;
  fiscalYear: number;
  fiscalMonth: number;
  status: string;
  salesCount: number;
  purchasesCount: number;
  withholdingsCount: number;
  totalSales: number;
  totalPurchases: number;
  totalWithheld: number;
  generatedAt?: string;
  submittedAt?: string;
  hasXml: boolean;
};

export type AtsGeneratePayload = {
  fiscalYear: number;
  fiscalMonth: number;
};

export type SriHealthStuckDoc = {
  id: number;
  documentCode?: string | null;
  sequentialNumber?: string | null;
  status?: string | null;
  lastStatusAt?: string | null;
  lastProviderMessage?: string | null;
};

export type SriHealthSnapshotDto = {
  tenantId: number;
  computedAt: string;
  documentsByStatus: Record<string, number>;
  stuckDocuments: SriHealthStuckDoc[];
  authorizedAwaitingPdf: number;
  emailRetryPending: number;
  emailPdfFailed: number;
  emailNoCustomerEmail: number;
  emailDelivered: number;
  stuckThresholdMinutes: number;
};

export type SriHealthReapResultDto = {
  tenantId: number;
  authorizedRequeuedForPdf: number;
  stuckDocumentsFlagged: number;
  executedAt: string;
};

export type SriDocumentDto = {
  id: number;
  documentCode: string;
  status: string;
  issueDate: string;
  buyerIdentification?: string;
  buyerName?: string;
  sequentialNumber?: string;
  accessKey?: string;
  authorizationCode?: string;
  subtotal: number;
  tax: number;
  total: number;
  validationErrors?: string;
  receptionStatus?: string;
  authorizationStatus?: string;
  providerMessage?: string;
  ridePdfUrl?: string;
  rideEmailStatus?: string;
  rideEmailError?: string;
  rideEmailAttempts?: number;
  rideEmailSentAt?: string;
  sentAt?: string;
  authorizedAt?: string;
  lastStatusAt?: string;
  createdAt: string;
};

export type SriTransmissionDto = {
  id: number;
  phase: string;
  endpointUrl: string;
  providerStatus?: string;
  providerMessage?: string;
  httpStatus?: number;
  success: boolean;
  attemptedAt: string;
  completedAt?: string;
};

export type RideTemplateDto = {
  id?: number;
  templateCode: string;
  templateName: string;
  layout: Record<string, unknown>;
  htmlContent: string;
  custom: boolean;
  active: boolean;
  updatedAt?: string;
};

export type RideTemplatePayload = {
  templateCode?: string;
  templateName?: string;
  layout?: Record<string, unknown>;
  htmlContent?: string;
  active?: boolean;
};

export type RideAssetDto = {
  path: string;
  publicUrl?: string;
};

export type RideAutomationDto = {
  documentId: number;
  emailStatus: string;
  message?: string;
};

export type VisionFootfallDto = {
  dataOrigin: string;
  degraded: boolean;
  totalPeople: number;
  warningMessage?: string;
  series: Array<{ bucketStart: string; peopleCount: number }>;
};

export type VisionConversionDto = {
  dataOrigin: string;
  degraded: boolean;
  totalPeople: number;
  closedSales: number;
  conversionRate: number;
  averageTicket: number;
  warningMessage?: string;
};

export type VisionPeakHoursDto = {
  dataOrigin: string;
  degraded: boolean;
  peakHours: Array<{
    rank: number;
    bucketStart: string;
    peopleCount: number;
    conversionRate: number;
  }>;
  warningMessage?: string;
};

export type VisionInsightsDto = {
  dataOrigin: string;
  degraded: boolean;
  insights: string[];
  warningMessage?: string;
};

export type VisionPredictionsDto = {
  dataOrigin: string;
  degraded: boolean;
  predictions: Array<{
    bucketStart: string;
    expectedVisitors: number;
    expectedConversionRate: number;
    confidenceBand?: string;
  }>;
  insights: string[];
  warningMessage?: string;
};

export type SalesReportDto = {
  from: string;
  to: string;
  totalSales: number;
  totalProfit: number;
  totalOrders: number;
  avgTicket: number;
  dailySales: Array<{ date: string; sales: number; profit: number; orders: number }>;
  topProducts: Array<{ id: number; name: string; quantity: number; revenue: number; profit: number }>;
  salesByPaymentMethod: Array<{ method: string; count: number; total: number }>;
  comparisonPreviousPeriod: {
    salesDelta: number;
    profitDelta: number;
    ordersDelta: number;
  };
};

export type ExportFormat = 'xlsx' | 'csv' | 'pdf';

export type BusinessInsightDto = {
  id: string;
  category: string;
  tone: 'success' | 'warning' | 'danger' | 'info';
  title: string;
  explanation: string;
  impact: string;
  actionRecommended: string;
  generatedAt: string;
};

export type CashRegisterDto = {
  id?: number;
  active?: boolean;
  status?: string;
  userName?: string;
  openedAt?: string;
  closedAt?: string;
  openingAmount?: number;
  closingAmount?: number;
  cashSalesTotal?: number;
  cardSalesTotal?: number;
  transferSalesTotal?: number;
  totalSalesAmount?: number;
  totalOrders?: number;
  expectedCash?: number;
  actualCash?: number;
  difference?: number;
  notes?: string;
};

export type TenantCommercialPlanDto = {
  tenantCode: string;
  commercialPlan: string;
  enabledModules: string[];
  availablePlans: string[];
};

export type AdminTenantDto = {
  tenantCode: string;
  legalName: string;
  tradeName?: string;
  active: boolean;
  commercialPlan: string;
  enabledModules: string[];
  userCount: number;
};

export type ModuleCatalogDto = {
  moduleKey: string;
  label: string;
};

export type AdminTenantCatalogDto = {
  tenants: AdminTenantDto[];
  availableModules: ModuleCatalogDto[];
  availablePlans: string[];
};

export type OnboardingJson = Record<string, unknown>;

export type OnboardingWarningDto = {
  step?: string;
  title?: string;
  detail?: string;
};

export type OnboardingSessionDto = {
  id: number;
  tenantId: number;
  tenantCode: string;
  tenantName: string;
  status: string;
  currentStep: string;
  progressPercent: number;
  verticalTemplate?: string;
  businessProfile: OnboardingJson;
  legalEntity: OnboardingJson;
  taxProfile: OnboardingJson;
  certificateStatus: OnboardingJson;
  emissionSetup: OnboardingJson;
  accountingSetup: OnboardingJson;
  salesSetup: OnboardingJson;
  purchasesSetup: OnboardingJson;
  inventorySetup: OnboardingJson;
  growthSetup: OnboardingJson;
  brandSetup: OnboardingJson;
  permissionsSetup: OnboardingJson;
  checklist: OnboardingJson;
  warnings: OnboardingWarningDto[];
  publishedAt?: string;
  updatedAt?: string;
};

export type OnboardingCatalogItemDto = {
  catalogType: string;
  itemCode: string;
  label: string;
  payload: OnboardingJson;
};

export type OnboardingCatalogDto = {
  documentTypes: OnboardingCatalogItemDto[];
  paymentMethods: OnboardingCatalogItemDto[];
  verticalTemplates: OnboardingCatalogItemDto[];
  fiscalFlags: OnboardingCatalogItemDto[];
};

export type BranchDto = {
  id: number;
  code: string;
  name: string;
  address?: string;
  city?: string;
  province?: string;
  phone?: string;
  email?: string;
  sriEstablishmentCode?: string;
  isMain: boolean;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type CreateBranchPayload = {
  code: string;
  name: string;
  address?: string;
  city?: string;
  province?: string;
  phone?: string;
  email?: string;
  sriEstablishmentCode?: string;
  isMain?: boolean;
};

export type WarehouseDto = {
  id: number;
  branchId: number;
  branchName?: string;
  code: string;
  name: string;
  type: 'STORE' | 'WAREHOUSE' | 'TRANSIT';
  address?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type CreateWarehousePayload = {
  branchId: number;
  code: string;
  name: string;
  type?: 'STORE' | 'WAREHOUSE' | 'TRANSIT';
  address?: string;
};

export type PartyDto = {
  id: number;
  partyType: 'PERSON' | 'ORGANIZATION';
  displayName: string;
  email?: string;
  phone?: string;
  primaryIdentifierType?: string;
  primaryIdentifierValue?: string;
  roles: string[];
  active: boolean;
  createdAt?: string;
};

export type PartyDetailDto = {
  id: number;
  partyType: 'PERSON' | 'ORGANIZATION';
  displayName: string;
  legalName?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  mobile?: string;
  website?: string;
  notes?: string;
  active: boolean;
  identifiers: Array<{ id: number; type: string; value: string; isPrimary: boolean }>;
  addresses: Array<{ id: number; type: string; street?: string; city?: string; province?: string; postalCode?: string; country: string; isPrimary: boolean }>;
  roles: string[];
  createdAt?: string;
  updatedAt?: string;
};

export type CreatePartyPayload = {
  partyType?: 'PERSON' | 'ORGANIZATION';
  displayName: string;
  legalName?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  mobile?: string;
  notes?: string;
  identifierType?: string;
  identifierValue?: string;
  street?: string;
  city?: string;
  province?: string;
  roles?: string[];
};

export type PartyIdentifierDto = {
  id: number;
  type: string;
  value: string;
  isPrimary: boolean;
};

export type PartyAddressDto = {
  id: number;
  type: string;
  street?: string;
  city?: string;
  province?: string;
  postalCode?: string;
  country: string;
  isPrimary: boolean;
};

export type PartyIdentifierPayload = {
  type: string;
  value: string;
  isPrimary?: boolean;
};

export type PartyAddressPayload = {
  type?: string;
  street?: string;
  city?: string;
  province?: string;
  postalCode?: string;
  country?: string;
  isPrimary?: boolean;
};

export type ErpSequenceDto = {
  id: number;
  branchId?: number;
  branchName?: string;
  code: string;
  name: string;
  prefix?: string;
  suffix?: string;
  padLength: number;
  currentNumber: number;
  incrementStep: number;
  resetPolicy?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type CreateErpSequencePayload = {
  code: string;
  name: string;
  branchId?: number;
  prefix?: string;
  suffix?: string;
  padLength?: number;
  incrementStep?: number;
  resetPolicy?: string;
};

export type ApprovalInstanceDto = {
  id: number;
  entityType: string;
  entityId: number;
  currentStep: number;
  status: string;
  definitionName?: string;
  requestedBy?: number;
  createdAt?: string;
  updatedAt?: string;
};

export type ApprovalDefinitionDto = {
  id: number;
  code: string;
  name: string;
  entityType: string;
  active: boolean;
};

export type ApprovalCountsDto = {
  pending: number;
};

export type UserPermissionsDto = {
  role: string;
  permissions: string[];
  platformAdmin: boolean;
};

@Injectable({ providedIn: 'root' })
export class ErpApi {
  private readonly http = inject(HttpClient);
  private readonly responseCache = new Map<string, { expiresAt: number; value$: Observable<unknown> }>();

  getPublicProducts(): Observable<ProductDto[]> {
    return this.http.get<ProductDto[]>(`${API_BASE_URL}/products/public`);
  }

  getProducts(page = 0, size = 50): Observable<PageDto<ProductDto>> {
    return this.http.get<PageDto<ProductDto>>(`${API_BASE_URL}/products`, {
      params: this.buildParams({ page: String(page), size: String(size) }),
    }).pipe(map((response) => this.normalizePage(response)));
  }

  getLowStockProducts(size = 50): Observable<ProductDto[]> {
    return this.http.get<ProductDto[]>(`${API_BASE_URL}/products/low-stock`, {
      params: this.buildParams({ size: String(size) }),
    });
  }

  createProduct(payload: {
    name: string; code?: string; price: number; purchasePrice?: number;
    categoryName?: string; unit?: string; minStock?: number; status?: string;
  }): Observable<ProductDto> {
    return this.http.post<ProductDto>(`${API_BASE_URL}/products`, payload);
  }

  updateProduct(id: number, payload: {
    name?: string; code?: string; price?: number; purchasePrice?: number;
    categoryName?: string; unit?: string; minStock?: number; status?: string;
  }): Observable<ProductDto> {
    return this.http.put<ProductDto>(`${API_BASE_URL}/products/${id}`, payload);
  }

  updateStock(productId: number, quantity: number, type: string, reason?: string): Observable<ProductDto> {
    let params = new HttpParams().set('quantity', quantity).set('type', type);
    if (reason) {
      params = params.set('reason', reason);
    }
    return this.http.post<ProductDto>(`${API_BASE_URL}/products/${productId}/update-stock`, null, { params });
  }

  getStockMovements(size = 50): Observable<StockMovementDto[]> {
    return this.http.get<StockMovementDto[]>(`${API_BASE_URL}/inventory/movements`, {
      params: this.buildParams({ size: String(size) }),
    });
  }

  exportInventory(format: ExportFormat, filters?: { search?: string; category?: string }): Observable<HttpResponse<Blob>> {
    return this.http.get(`${API_BASE_URL}/inventory/export`, {
      params: this.buildParams({
        format,
        search: filters?.search ?? '',
        category: filters?.category ?? '',
      }),
      observe: 'response',
      responseType: 'blob',
    });
  }

  previewOrderTotals(items: Array<{ productId: number; quantity: number; unitPrice: number }>): Observable<{
    subtotal: number;
    tax: number;
    total: number;
  }> {
    return this.http.post<{ subtotal: number; tax: number; total: number }>(`${API_BASE_URL}/orders/prices`, {
      items,
    });
  }

  createOrder(payload: {
    items: Array<{ productId: number; quantity: number; unitPrice: number; discountAmount?: number }>;
    paymentMethod: 'CASH' | 'CARD' | 'TRANSFER';
    notes?: string;
    documentType?: OrderDocumentType;
    customerName?: string;
    customerEmail?: string;
    customerIdentification?: string;
    customerAddress?: string;
    customerPhone?: string;
  }): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/orders`, payload);
  }

  listOrders(transactionStatus?: string, page = 0, size = 20): Observable<PageDto<OrderDto>> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (transactionStatus) {
      params = params.set('transactionStatus', transactionStatus);
    }
    return this.http.get<PageDto<OrderDto>>(`${API_BASE_URL}/orders`, { params })
      .pipe(map((response) => this.normalizePage(response)));
  }

  payOrder(
    orderId: number,
    method: 'CASH' | 'CARD' | 'TRANSFER',
    reference?: string,
    paymentBreakdownJson?: string,
  ): Observable<OrderDto> {
    return this.http.patch<OrderDto>(`${API_BASE_URL}/orders/${orderId}/pay`, {
      method,
      reference,
      paymentBreakdownJson,
    });
  }

  confirmTransfer(orderId: number, paymentReference?: string): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/orders/${orderId}/confirm-payment`, { paymentReference });
  }

  cancelOrder(orderId: number, reason?: string): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/orders/${orderId}/cancel`, { reason });
  }

  refundOrder(orderId: number, reason?: string): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/orders/${orderId}/refund`, { reason });
  }

  getDashboardSnapshot(forceRefresh = false): Observable<DashboardSnapshot> {
    return this.cachedGet<DashboardSnapshot>(
      `${API_BASE_URL}/dashboard/snapshot`,
      undefined,
      30_000,
      forceRefresh,
    );
  }

  getSalesReport(from: string, to: string, forceRefresh = false): Observable<SalesReportDto> {
    return this.cachedGet<SalesReportDto>(
      `${API_BASE_URL}/erp/reports/sales`,
      { from, to },
      60_000,
      forceRefresh,
    );
  }

  exportSalesReport(format: ExportFormat, from: string, to: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`${API_BASE_URL}/erp/reports/export`, {
      params: this.buildParams({ format, from, to }),
      observe: 'response',
      responseType: 'blob',
    });
  }

  getBusinessInsights(): Observable<BusinessInsightDto[]> {
    return this.cachedGet<BusinessInsightDto[]>(`${API_BASE_URL}/erp/insights`, undefined, 60_000);
  }

  getPurchases(): Observable<PurchaseDto[]> {
    return this.http.get<PurchaseDto[]>(`${API_BASE_URL}/erp/purchases`);
  }

  exportPurchases(
    format: ExportFormat,
    filters?: { status?: string; search?: string; from?: string; to?: string },
  ): Observable<HttpResponse<Blob>> {
    return this.http.get(`${API_BASE_URL}/erp/purchases/export`, {
      params: this.buildParams({
        format,
        status: filters?.status ?? '',
        search: filters?.search ?? '',
        from: filters?.from ?? '',
        to: filters?.to ?? '',
      }),
      observe: 'response',
      responseType: 'blob',
    });
  }

  createPurchase(payload: unknown): Observable<PurchaseDto> {
    return this.http.post<PurchaseDto>(`${API_BASE_URL}/erp/purchases`, payload);
  }

  receivePurchase(purchaseId: number): Observable<PurchaseDto> {
    return this.http.post<PurchaseDto>(`${API_BASE_URL}/erp/purchases/${purchaseId}/receive`, {});
  }

  payPurchase(purchaseId: number, payload: unknown): Observable<PurchaseDto> {
    return this.http.post<PurchaseDto>(`${API_BASE_URL}/erp/purchases/${purchaseId}/pay`, payload);
  }

  getAccounts(): Observable<AccountDto[]> {
    return this.http.get<AccountDto[]>(`${API_BASE_URL}/erp/accounting/accounts`);
  }

  getTaxRules(): Observable<TaxRuleDto[]> {
    return this.http.get<TaxRuleDto[]>(`${API_BASE_URL}/erp/accounting/tax-rules`);
  }

  getSequences(): Observable<SequenceDto[]> {
    return this.http.get<SequenceDto[]>(`${API_BASE_URL}/erp/accounting/sequences`);
  }

  createSriSequence(payload: UpsertSequencePayload): Observable<SequenceDto> {
    return this.http.post<SequenceDto>(`${API_BASE_URL}/erp/accounting/sequences`, payload);
  }

  updateSriSequence(payload: UpsertSequencePayload): Observable<SequenceDto> {
    return this.http.put<SequenceDto>(`${API_BASE_URL}/erp/accounting/sequences`, payload);
  }

  listAtsPeriods(): Observable<AtsPeriodDto[]> {
    return this.http.get<AtsPeriodDto[]>(`${API_BASE_URL}/erp/sri/ats`);
  }

  generateAtsPeriod(payload: AtsGeneratePayload): Observable<AtsPeriodDto> {
    return this.http.post<AtsPeriodDto>(`${API_BASE_URL}/erp/sri/ats/generate`, payload);
  }

  markAtsSubmitted(id: number): Observable<AtsPeriodDto> {
    return this.http.post<AtsPeriodDto>(`${API_BASE_URL}/erp/sri/ats/${id}/submit`, {});
  }

  closeAtsPeriod(id: number): Observable<AtsPeriodDto> {
    return this.http.post<AtsPeriodDto>(`${API_BASE_URL}/erp/sri/ats/${id}/close`, {});
  }

  downloadAtsXml(id: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${API_BASE_URL}/erp/sri/ats/${id}/xml`, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  getSriHealth(): Observable<SriHealthSnapshotDto> {
    return this.cachedGet<SriHealthSnapshotDto>(`${API_BASE_URL}/erp/sri/health`, undefined, 30_000);
  }

  reapSriHealth(): Observable<SriHealthReapResultDto> {
    return this.http.post<SriHealthReapResultDto>(`${API_BASE_URL}/erp/sri/health/reap`, {});
  }

  getJournalEntries(): Observable<JournalEntryDto[]> {
    return this.http.get<JournalEntryDto[]>(`${API_BASE_URL}/erp/accounting/journal-entries`);
  }

  exportAccounting(format: ExportFormat): Observable<HttpResponse<Blob>> {
    return this.http.get(`${API_BASE_URL}/erp/accounting/export`, {
      params: this.buildParams({ format }),
      observe: 'response',
      responseType: 'blob',
    });
  }

  getReceivables(): Observable<ReceivableDto[]> {
    return this.http.get<ReceivableDto[]>(`${API_BASE_URL}/erp/accounting/receivables`);
  }

  createManualReceivable(payload: unknown): Observable<ReceivableDto> {
    return this.http.post<ReceivableDto>(`${API_BASE_URL}/erp/accounting/receivables/manual`, payload);
  }

  applyReceivablePayment(receivableId: number, payload: unknown): Observable<ReceivableDto> {
    return this.http.post<ReceivableDto>(`${API_BASE_URL}/erp/accounting/receivables/${receivableId}/payments`, payload);
  }

  getPayables(): Observable<PayableDto[]> {
    return this.http.get<PayableDto[]>(`${API_BASE_URL}/erp/accounting/payables`);
  }

  applyPayablePayment(payableId: number, payload: unknown): Observable<PayableDto> {
    return this.http.post<PayableDto>(`${API_BASE_URL}/erp/accounting/payables/${payableId}/payments`, payload);
  }

  getSriDocuments(): Observable<SriDocumentDto[]> {
    return this.http.get<SriDocumentDto[]>(`${API_BASE_URL}/erp/sri/documents`);
  }

  getCustomerDocuments(): Observable<SriDocumentDto[]> {
    return this.http.get<SriDocumentDto[]>(`${API_BASE_URL}/erp/customers/documents`);
  }

  exportSriDocuments(format: ExportFormat, status?: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`${API_BASE_URL}/erp/sri/export`, {
      params: this.buildParams({ format, status: status ?? '' }),
      observe: 'response',
      responseType: 'blob',
    });
  }

  validateSriDocument(documentId: number): Observable<SriDocumentDto> {
    return this.http.post<SriDocumentDto>(`${API_BASE_URL}/erp/sri/documents/${documentId}/validate`, {});
  }

  authorizeSriDocument(documentId: number, authorizationCode: string): Observable<SriDocumentDto> {
    const params = new HttpParams().set('authorizationCode', authorizationCode);
    return this.http.post<SriDocumentDto>(`${API_BASE_URL}/erp/sri/documents/${documentId}/authorize`, null, {
      params,
    });
  }

  emitSriDocument(documentId: number): Observable<SriDocumentDto> {
    return this.http.post<SriDocumentDto>(`${API_BASE_URL}/erp/sri/documents/${documentId}/emit`, {});
  }

  pollSriDocumentAuthorization(documentId: number): Observable<SriDocumentDto> {
    return this.http.post<SriDocumentDto>(
      `${API_BASE_URL}/erp/sri/documents/${documentId}/poll-authorization`,
      {},
    );
  }

  getSriTransmissions(documentId: number): Observable<SriTransmissionDto[]> {
    return this.http.get<SriTransmissionDto[]>(`${API_BASE_URL}/erp/sri/documents/${documentId}/transmissions`);
  }

  getRidePreview(): Observable<string> {
    return this.http.get(`${API_BASE_URL}/erp/sri/ride/preview`, { responseType: 'text' });
  }

  getRideDocumentPreview(documentId: number): Observable<string> {
    return this.http.get(`${API_BASE_URL}/erp/sri/ride/documents/${documentId}/preview`, { responseType: 'text' });
  }

  downloadRidePdf(documentId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${API_BASE_URL}/erp/sri/ride/documents/${documentId}/pdf`, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  regenerateRide(documentId: number): Observable<RideAutomationDto> {
    return this.http.post<RideAutomationDto>(`${API_BASE_URL}/erp/sri/ride/documents/${documentId}/regenerate`, {});
  }

  getRideTemplate(): Observable<RideTemplateDto> {
    return this.http.get<RideTemplateDto>(`${API_BASE_URL}/erp/sri/ride/template`);
  }

  saveRideTemplate(payload: RideTemplatePayload): Observable<RideTemplateDto> {
    return this.http.put<RideTemplateDto>(`${API_BASE_URL}/erp/sri/ride/template`, payload);
  }

  uploadRideLogo(file: File): Observable<RideAssetDto> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<RideAssetDto>(`${API_BASE_URL}/erp/sri/ride/assets/logo`, formData);
  }

  getVisionFootfall(params?: Record<string, string>, forceRefresh = false): Observable<VisionFootfallDto> {
    return this.cachedGet<VisionFootfallDto>(
      `${API_BASE_URL}/erp/vision-ai/afluencia`,
      params,
      60_000,
      forceRefresh,
    );
  }

  getVisionConversion(params?: Record<string, string>, forceRefresh = false): Observable<VisionConversionDto> {
    return this.cachedGet<VisionConversionDto>(
      `${API_BASE_URL}/erp/vision-ai/conversion`,
      params,
      60_000,
      forceRefresh,
    );
  }

  getVisionPeakHours(params?: Record<string, string>, forceRefresh = false): Observable<VisionPeakHoursDto> {
    return this.cachedGet<VisionPeakHoursDto>(
      `${API_BASE_URL}/erp/vision-ai/horas-pico`,
      params,
      60_000,
      forceRefresh,
    );
  }

  getVisionInsights(params?: Record<string, string>, forceRefresh = false): Observable<VisionInsightsDto> {
    return this.cachedGet<VisionInsightsDto>(
      `${API_BASE_URL}/erp/vision-ai/insights`,
      params,
      60_000,
      forceRefresh,
    );
  }

  getVisionPredictions(params?: Record<string, string>, forceRefresh = false): Observable<VisionPredictionsDto> {
    return this.cachedGet<VisionPredictionsDto>(
      `${API_BASE_URL}/erp/vision-ai/predicciones`,
      params,
      60_000,
      forceRefresh,
    );
  }

  // ── Cash Register ──
  openCashRegister(openingAmount: number): Observable<CashRegisterDto> {
    return this.http.post<CashRegisterDto>(`${API_BASE_URL}/cash-register/open`, { openingAmount });
  }

  closeCashRegister(id: number, actualCash: number, notes?: string): Observable<CashRegisterDto> {
    return this.http.post<CashRegisterDto>(`${API_BASE_URL}/cash-register/${id}/close`, { actualCash, notes });
  }

  getActiveCashRegister(): Observable<CashRegisterDto> {
    return this.http.get<CashRegisterDto>(`${API_BASE_URL}/cash-register/active`);
  }

  getCashRegisterReport(id: number): Observable<CashRegisterDto> {
    return this.http.get<CashRegisterDto>(`${API_BASE_URL}/cash-register/${id}/report`);
  }

  getTenantAdminCatalog(): Observable<AdminTenantCatalogDto> {
    return this.http.get<AdminTenantCatalogDto>(`${API_BASE_URL}/erp/admin/tenants`);
  }

  createTenant(payload: {
    tenantCode: string;
    legalName: string;
    tradeName?: string;
    countryCode?: string;
    timeZone?: string;
    currencyCode?: string;
  }): Observable<AdminTenantDto> {
    return this.http.post<AdminTenantDto>(`${API_BASE_URL}/erp/admin/tenants`, payload);
  }

  updateTenantPlan(tenantCode: string, commercialPlan: string): Observable<TenantCommercialPlanDto> {
    return this.http.put<TenantCommercialPlanDto>(`${API_BASE_URL}/erp/admin/tenants/${tenantCode}/plan`, {
      commercialPlan,
    });
  }

  updateTenantModules(tenantCode: string, enabledModules: string[]): Observable<TenantCommercialPlanDto> {
    return this.http.put<TenantCommercialPlanDto>(`${API_BASE_URL}/erp/admin/tenants/${tenantCode}/modules`, {
      enabledModules,
    });
  }

  // ── Branches (Sucursales) ──────────────────────────────────────────

  getOnboardingSession(): Observable<OnboardingSessionDto> {
    return this.http.get<OnboardingSessionDto>(`${API_BASE_URL}/erp/onboarding/session`);
  }

  updateOnboardingStep(step: string, payload: OnboardingJson): Observable<OnboardingSessionDto> {
    return this.http.patch<OnboardingSessionDto>(`${API_BASE_URL}/erp/onboarding/steps/${step}`, payload);
  }

  publishOnboarding(): Observable<OnboardingSessionDto> {
    return this.http.post<OnboardingSessionDto>(`${API_BASE_URL}/erp/onboarding/publish`, {});
  }

  getOnboardingCatalog(): Observable<OnboardingCatalogDto> {
    return this.cachedGet<OnboardingCatalogDto>(`${API_BASE_URL}/erp/onboarding/catalog`, undefined, 120_000);
  }

  validateOnboardingRuc(ruc: string): Observable<{ valid: boolean; message: string }> {
    return this.http.get<{ valid: boolean; message: string }>(`${API_BASE_URL}/erp/onboarding/ruc/validate`, {
      params: this.buildParams({ ruc }),
    });
  }

  uploadOnboardingSriCertificate(payload: {
    file: File;
    password: string;
    alias?: string;
    environment?: string;
  }): Observable<OnboardingSessionDto> {
    const formData = new FormData();
    formData.append('file', payload.file);
    formData.append('password', payload.password);
    if (payload.alias) formData.append('alias', payload.alias);
    if (payload.environment) formData.append('environment', payload.environment);
    return this.http.post<OnboardingSessionDto>(`${API_BASE_URL}/erp/onboarding/sri-certificate`, formData);
  }

  uploadOnboardingRideReference(file: File): Observable<OnboardingSessionDto> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<OnboardingSessionDto>(`${API_BASE_URL}/erp/onboarding/ride-reference`, formData);
  }

  getBranches(page = 0, size = 20, activeOnly = true): Observable<PageDto<BranchDto>> {
    return this.http.get<PageDto<BranchDto>>(`${API_BASE_URL}/erp/branches`, {
      params: this.buildParams({ page: String(page), size: String(size), activeOnly: String(activeOnly) }),
    }).pipe(map((response) => this.normalizePage(response)));
  }

  getAllBranches(): Observable<BranchDto[]> {
    return this.cachedGet<BranchDto[]>(`${API_BASE_URL}/erp/branches/all`, undefined, 60_000);
  }

  getBranch(id: number): Observable<BranchDto> {
    return this.http.get<BranchDto>(`${API_BASE_URL}/erp/branches/${id}`);
  }

  createBranch(payload: CreateBranchPayload): Observable<BranchDto> {
    return this.http.post<BranchDto>(`${API_BASE_URL}/erp/branches`, payload);
  }

  updateBranch(id: number, payload: Partial<CreateBranchPayload> & { active?: boolean }): Observable<BranchDto> {
    return this.http.put<BranchDto>(`${API_BASE_URL}/erp/branches/${id}`, payload);
  }

  deactivateBranch(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/erp/branches/${id}`);
  }

  // ── Warehouses (Bodegas) ───────────────────────────────────────────

  getWarehouses(branchId?: number, page = 0, size = 20): Observable<PageDto<WarehouseDto>> {
    const params: Record<string, string> = { page: String(page), size: String(size) };
    if (branchId) params['branchId'] = String(branchId);
    return this.http.get<PageDto<WarehouseDto>>(`${API_BASE_URL}/erp/warehouses`, {
      params: this.buildParams(params),
    }).pipe(map((response) => this.normalizePage(response)));
  }

  getAllWarehouses(branchId?: number): Observable<WarehouseDto[]> {
    const params: Record<string, string> = {};
    if (branchId) params['branchId'] = String(branchId);
    return this.cachedGet<WarehouseDto[]>(`${API_BASE_URL}/erp/warehouses/all`, params, 60_000);
  }

  getWarehouse(id: number): Observable<WarehouseDto> {
    return this.http.get<WarehouseDto>(`${API_BASE_URL}/erp/warehouses/${id}`);
  }

  createWarehouse(payload: CreateWarehousePayload): Observable<WarehouseDto> {
    return this.http.post<WarehouseDto>(`${API_BASE_URL}/erp/warehouses`, payload);
  }

  updateWarehouse(id: number, payload: Partial<CreateWarehousePayload> & { active?: boolean }): Observable<WarehouseDto> {
    return this.http.put<WarehouseDto>(`${API_BASE_URL}/erp/warehouses/${id}`, payload);
  }

  deactivateWarehouse(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/erp/warehouses/${id}`);
  }

  // ── Parties (Personas / Organizaciones) ────────────────────────────

  getParties(search?: string, role?: string, page = 0, size = 20): Observable<PageDto<PartyDto>> {
    const params: Record<string, string> = { page: String(page), size: String(size) };
    if (search) params['search'] = search;
    if (role) params['role'] = role;
    return this.http.get<PageDto<PartyDto>>(`${API_BASE_URL}/erp/parties`, {
      params: this.buildParams(params),
    }).pipe(map((response) => this.normalizePage(response)));
  }

  getParty(id: number): Observable<PartyDetailDto> {
    return this.http.get<PartyDetailDto>(`${API_BASE_URL}/erp/parties/${id}`);
  }

  createParty(payload: CreatePartyPayload): Observable<PartyDetailDto> {
    return this.http.post<PartyDetailDto>(`${API_BASE_URL}/erp/parties`, payload);
  }

  updateParty(id: number, payload: Partial<CreatePartyPayload> & { active?: boolean }): Observable<PartyDetailDto> {
    return this.http.put<PartyDetailDto>(`${API_BASE_URL}/erp/parties/${id}`, payload);
  }

  addPartyRole(id: number, role: string): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/erp/parties/${id}/roles`, { role });
  }

  removePartyRole(id: number, role: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/erp/parties/${id}/roles/${role}`);
  }

  getPartyCounts(): Observable<Record<string, number>> {
    return this.cachedGet<Record<string, number>>(`${API_BASE_URL}/erp/parties/counts`, undefined, 30_000);
  }

  findPartyByIdentifier(type: string, value: string): Observable<PartyDetailDto> {
    return this.http.get<PartyDetailDto>(`${API_BASE_URL}/erp/parties/by-identifier`,
      { params: this.buildParams({ type, value }) });
  }

  addPartyIdentifier(partyId: number, payload: PartyIdentifierPayload): Observable<PartyIdentifierDto> {
    return this.http.post<PartyIdentifierDto>(`${API_BASE_URL}/erp/parties/${partyId}/identifiers`, payload);
  }

  updatePartyIdentifier(partyId: number, identifierId: number, payload: PartyIdentifierPayload): Observable<PartyIdentifierDto> {
    return this.http.put<PartyIdentifierDto>(
      `${API_BASE_URL}/erp/parties/${partyId}/identifiers/${identifierId}`, payload);
  }

  removePartyIdentifier(partyId: number, identifierId: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/erp/parties/${partyId}/identifiers/${identifierId}`);
  }

  setPrimaryPartyIdentifier(partyId: number, identifierId: number): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/erp/parties/${partyId}/identifiers/${identifierId}/primary`, {});
  }

  addPartyAddress(partyId: number, payload: PartyAddressPayload): Observable<PartyAddressDto> {
    return this.http.post<PartyAddressDto>(`${API_BASE_URL}/erp/parties/${partyId}/addresses`, payload);
  }

  updatePartyAddress(partyId: number, addressId: number, payload: PartyAddressPayload): Observable<PartyAddressDto> {
    return this.http.put<PartyAddressDto>(
      `${API_BASE_URL}/erp/parties/${partyId}/addresses/${addressId}`, payload);
  }

  removePartyAddress(partyId: number, addressId: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/erp/parties/${partyId}/addresses/${addressId}`);
  }

  setPrimaryPartyAddress(partyId: number, addressId: number): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/erp/parties/${partyId}/addresses/${addressId}/primary`, {});
  }

  // ── Permissions ──────────────────────────────────────────────────

  getMyPermissions(): Observable<UserPermissionsDto> {
    return this.cachedGet<UserPermissionsDto>(`${API_BASE_URL}/erp/permissions/me`, undefined, 60_000);
  }

  // ── Approvals ───────────────────────────────────────────────────

  getApprovalsPending(page = 0, size = 20): Observable<PageDto<ApprovalInstanceDto>> {
    return this.http.get<PageDto<ApprovalInstanceDto>>(
      `${API_BASE_URL}/erp/approvals`, { params: this.buildParams({ page: String(page), size: String(size) }) })
      .pipe(map((response) => this.normalizePage(response)));
  }

  getApproval(id: number): Observable<ApprovalInstanceDto> {
    return this.http.get<ApprovalInstanceDto>(`${API_BASE_URL}/erp/approvals/${id}`);
  }

  approveInstance(id: number, payload: { userId?: number; userName?: string; comment?: string }): Observable<ApprovalInstanceDto> {
    return this.http.post<ApprovalInstanceDto>(`${API_BASE_URL}/erp/approvals/${id}/approve`, payload);
  }

  rejectInstance(id: number, payload: { userId?: number; userName?: string; comment?: string }): Observable<ApprovalInstanceDto> {
    return this.http.post<ApprovalInstanceDto>(`${API_BASE_URL}/erp/approvals/${id}/reject`, payload);
  }

  cancelApproval(id: number): Observable<ApprovalInstanceDto> {
    return this.http.post<ApprovalInstanceDto>(`${API_BASE_URL}/erp/approvals/${id}/cancel`, {});
  }

  getApprovalCounts(): Observable<ApprovalCountsDto> {
    return this.cachedGet<ApprovalCountsDto>(`${API_BASE_URL}/erp/approvals/counts`, undefined, 30_000);
  }

  getApprovalDefinitions(): Observable<ApprovalDefinitionDto[]> {
    return this.http.get<ApprovalDefinitionDto[]>(`${API_BASE_URL}/erp/approvals/definitions`);
  }

  // ── Sequences ───────────────────────────────────────────────────

  getErpSequences(): Observable<ErpSequenceDto[]> {
    return this.http.get<ErpSequenceDto[]>(`${API_BASE_URL}/erp/sequences`);
  }

  getErpSequence(id: number): Observable<ErpSequenceDto> {
    return this.http.get<ErpSequenceDto>(`${API_BASE_URL}/erp/sequences/${id}`);
  }

  createErpSequence(payload: CreateErpSequencePayload): Observable<ErpSequenceDto> {
    return this.http.post<ErpSequenceDto>(`${API_BASE_URL}/erp/sequences`, payload);
  }

  updateErpSequence(id: number, payload: Partial<CreateErpSequencePayload> & { active?: boolean }): Observable<ErpSequenceDto> {
    return this.http.put<ErpSequenceDto>(`${API_BASE_URL}/erp/sequences/${id}`, payload);
  }

  getNextSequenceNumber(code: string, branchId?: number): Observable<{ number: string }> {
    const params: Record<string, string> = { code };
    if (branchId) params['branchId'] = String(branchId);
    return this.http.post<{ number: string }>(`${API_BASE_URL}/erp/sequences/next`, null,
      { params: this.buildParams(params) });
  }

  previewSequenceNumber(code: string, branchId?: number): Observable<{ number: string }> {
    const params: Record<string, string> = { code };
    if (branchId) params['branchId'] = String(branchId);
    return this.http.get<{ number: string }>(`${API_BASE_URL}/erp/sequences/preview`,
      { params: this.buildParams(params) });
  }

  private buildParams(values?: Record<string, string>): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(values ?? {})) {
      if (value) {
        params = params.set(key, value);
      }
    }
    return params;
  }

  private normalizePage<T>(response: PageDto<T>): PageDto<T> {
    const page = response.page;
    if (!page) {
      return response;
    }
    return {
      ...response,
      totalElements: page.totalElements ?? response.totalElements ?? response.content?.length ?? 0,
      totalPages: page.totalPages ?? response.totalPages,
      number: page.number ?? response.number,
      size: page.size ?? response.size,
    };
  }

  private cachedGet<T>(
    url: string,
    values?: Record<string, string>,
    ttlMs = 30_000,
    forceRefresh = false,
  ): Observable<T> {
    const params = this.buildParams(values);
    const key = `${url}?${params.toString()}`;
    const now = Date.now();
    const cached = this.responseCache.get(key);

    if (!forceRefresh && cached && cached.expiresAt > now) {
      return cached.value$ as Observable<T>;
    }

    const value$ = this.http.get<T>(url, { params }).pipe(
      catchError((error) => {
        this.responseCache.delete(key);
        return throwError(() => error);
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    this.responseCache.set(key, { expiresAt: now + ttlMs, value$ });
    return value$;
  }
}
