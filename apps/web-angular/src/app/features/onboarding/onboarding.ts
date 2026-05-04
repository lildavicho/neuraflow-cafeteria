import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NgTemplateOutlet } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, finalize, forkJoin, switchMap } from 'rxjs';
import {
  ErpApi,
  OnboardingCatalogDto,
  OnboardingCatalogItemDto,
  OnboardingJson,
  OnboardingSessionDto,
} from '../../core/services/erp-api';

type StepKey =
  | 'business-discovery'
  | 'legal-entity'
  | 'tax-profile'
  | 'certificate-connectivity'
  | 'emission-points'
  | 'accounting-template'
  | 'sales-pos'
  | 'purchases'
  | 'inventory'
  | 'crm-hr-bi'
  | 'brand-studio'
  | 'publish-review';

type StepDef = {
  key: StepKey;
  short: string;
  title: string;
  description: string;
  moduleImpact: string[];
};

type CatalogKey = 'documentTypes' | 'paymentMethods' | 'verticalTemplates' | 'fiscalFlags';

const STEPS: StepDef[] = [
  { key: 'business-discovery', short: 'Negocio', title: 'Cuéntanos tu negocio', description: 'Elige a qué se dedica, cómo vende y qué herramientas necesita para empezar.', moduleImpact: ['Tipo de negocio', 'Módulos sugeridos', 'Flujo de venta'] },
  { key: 'legal-entity', short: 'RUC', title: 'Datos del RUC', description: 'Ingresa los mismos datos que constan en el SRI para que el sistema facture sin rechazos.', moduleImpact: ['RUC', 'Razón social', 'Dirección matriz'] },
  { key: 'tax-profile', short: 'SRI', title: 'Tu situación en el SRI', description: 'Responde preguntas sencillas sobre RIMPE, retenciones y actividades especiales.', moduleImpact: ['Leyendas fiscales', 'Retenciones', 'ATS'] },
  { key: 'certificate-connectivity', short: 'Firma', title: 'Sube tu firma electrónica', description: 'Carga el archivo .p12 y su clave. La clave se cifra y nunca se muestra al usuario.', moduleImpact: ['Firma .p12', 'Clave cifrada', 'Ambiente SRI'] },
  { key: 'emission-points', short: 'Locales', title: 'Locales y numeración', description: 'Configura desde qué local y caja se emitirán los comprobantes.', moduleImpact: ['Local', 'Caja', 'Secuenciales'] },
  { key: 'accounting-template', short: 'Cuentas', title: 'Cómo quieres llevar las cuentas', description: 'Selecciona una base contable simple para ventas, compras, bancos e impuestos.', moduleImpact: ['Contabilidad', 'Impuestos', 'Reportes'] },
  { key: 'sales-pos', short: 'Ventas', title: 'Cómo vendes y cobras', description: 'Activa caja, crédito, formas de pago y reglas de venta del negocio.', moduleImpact: ['Ventas', 'Caja', 'Cobros'] },
  { key: 'purchases', short: 'Compras', title: 'Cómo compras a proveedores', description: 'Define si registrarás facturas de proveedores, autorizaciones y aprobaciones.', moduleImpact: ['Compras', 'Proveedores', 'ATS'] },
  { key: 'inventory', short: 'Stock', title: 'Productos, servicios e inventario', description: 'Indica si manejas stock, bodegas, lotes, códigos de barra o solo servicios.', moduleImpact: ['Productos', 'Bodegas', 'Kardex'] },
  { key: 'crm-hr-bi', short: 'Extras', title: 'Herramientas extra', description: 'Elige si también quieres CRM, asistencia, reportes o envío automático de correos.', moduleImpact: ['Clientes', 'Equipo', 'Reportes'] },
  { key: 'brand-studio', short: 'Diseño', title: 'Diseño de comprobantes', description: 'Sube logo, colores o un modelo de comprobante para adaptar el comprobante impreso legal.', moduleImpact: ['Logo', 'PDF', 'Correo'] },
  { key: 'publish-review', short: 'Activar', title: 'Revisar y activar', description: 'Confirma que todo esté listo para automatizar ventas, firma, SRI, PDF y correo.', moduleImpact: ['Activación', 'Auditoría', 'Automatización'] },
];

const STEP_PAYLOAD_KEY: Record<StepKey, keyof OnboardingSessionDto> = {
  'business-discovery': 'businessProfile',
  'legal-entity': 'legalEntity',
  'tax-profile': 'taxProfile',
  'certificate-connectivity': 'certificateStatus',
  'emission-points': 'emissionSetup',
  'accounting-template': 'accountingSetup',
  'sales-pos': 'salesSetup',
  purchases: 'purchasesSetup',
  inventory: 'inventorySetup',
  'crm-hr-bi': 'growthSetup',
  'brand-studio': 'brandSetup',
  'publish-review': 'permissionsSetup',
};

@Component({
  selector: 'app-onboarding',
  imports: [FormsModule, NgTemplateOutlet],
  templateUrl: './onboarding.html',
  styleUrls: ['./onboarding.css'],
})
export class Onboarding {
  private readonly api = inject(ErpApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly autosave$ = new Subject<void>();

  readonly steps = STEPS;
  readonly dimensionOptions = ['BRANCH', 'BUSINESS_LINE', 'WAREHOUSE', 'COST_CENTER', 'SALESPERSON', 'PROJECT'];
  readonly growthModules = [
    { key: 'crmEnabled', title: 'CRM comercial', detail: 'Pipeline, segmentos, scoring y seguimiento.' },
    { key: 'hrEnabled', title: 'Equipo y asistencia', detail: 'Control básico de asistencia, aprobadores y permisos.' },
    { key: 'biEnabled', title: 'Reportes gerenciales', detail: 'Indicadores, tableros por área y alertas del negocio.' },
    { key: 'emailRelayEnabled', title: 'Correo automático', detail: 'Envío centralizado de XML y comprobante impreso.' },
  ];

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly publishing = signal(false);
  readonly feedback = signal('');
  readonly saveState = signal('Sin cambios pendientes');
  readonly session = signal<OnboardingSessionDto | null>(null);
  readonly catalog = signal<OnboardingCatalogDto | null>(null);
  readonly activeStepKey = signal<StepKey>('business-discovery');
  readonly draft = signal<OnboardingJson>({});
  readonly logoPreview = signal('');
  readonly logoFile = signal<File | null>(null);
  readonly logoUploading = signal(false);
  readonly certificateFile = signal<File | null>(null);
  readonly certificatePassword = signal('');
  readonly certificateUploading = signal(false);
  readonly rideReferenceFile = signal<File | null>(null);
  readonly rideReferenceUploading = signal(false);

  readonly activeStep = computed(() => this.steps.find((step) => step.key === this.activeStepKey()) ?? this.steps[0]);
  readonly activeStepIndex = computed(() => this.steps.findIndex((step) => step.key === this.activeStepKey()));
  readonly brandColor = computed(() => {
    const fromDraft = this.activeStepKey() === 'brand-studio' ? this.asString(this.draft()['primaryColor']) : '';
    const fromSession = this.asString(this.session()?.brandSetup?.['primaryColor']);
    return fromDraft || fromSession || '#4F46E5';
  });
  readonly logoSrc = computed(() => {
    const fromDraft = this.activeStepKey() === 'brand-studio' ? this.asString(this.draft()['logoDataUrl']) : '';
    const fromSession = this.asString(this.session()?.brandSetup?.['logoDataUrl'] ?? this.session()?.brandSetup?.['logoUrl']);
    return this.logoPreview() || fromDraft || fromSession;
  });

  constructor() {
    this.autosave$
      .pipe(debounceTime(450), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.saveActiveStep(false));

    forkJoin({ session: this.api.getOnboardingSession(), catalog: this.api.getOnboardingCatalog() })
      .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ session, catalog }) => {
          this.catalog.set(catalog);
          this.applySession(session, true);
        },
        error: () => this.feedback.set('No se pudo cargar la configuración. Revisa tu sesión, permisos o backend.'),
      });
  }

  setActiveStep(step: StepKey): void {
    if (step === this.activeStepKey()) return;
    this.saveActiveStep(false);
    this.activeStepKey.set(step);
    this.hydrateDraft();
    this.feedback.set('');
  }

  setField(key: string, value: unknown): void {
    this.draft.update((current) => ({ ...current, [key]: value }));
    this.saveState.set('Cambios pendientes');
    this.autosave$.next();
  }

  field(key: string): string {
    return this.asString(this.draft()[key]);
  }

  boolField(key: string): boolean {
    const value = this.draft()[key];
    return value === true || value === 'true';
  }

  arrayHas(key: string, value: string): boolean {
    return this.arrayField(key).includes(value);
  }

  toggleArrayValue(key: string, value: string): void {
    const values = new Set(this.arrayField(key));
    values.has(value) ? values.delete(value) : values.add(value);
    this.setField(key, Array.from(values));
  }

  catalogItems(key: CatalogKey): OnboardingCatalogItemDto[] {
    const items = [...(this.catalog()?.[key] ?? [])];
    if (key === 'documentTypes') {
      return items.sort((left, right) => Number(left.itemCode) - Number(right.itemCode));
    }
    if (key === 'paymentMethods') {
      return items.sort((left, right) => Number(left.itemCode) - Number(right.itemCode));
    }
    return items;
  }

  stepCompleted(step: StepKey): boolean {
    return this.session()?.checklist?.[step] === true;
  }

  fiscalHelp(code: string): string {
    const descriptions: Record<string, string> = {
      retention_agent: 'Solo marca si el SRI te emitió una resolución.',
      special_taxpayer: 'Aparece en tu RUC o en una notificación del SRI.',
      rimpe: 'Marca si en tu RUC consta Régimen RIMPE.',
      large_taxpayer: 'Solo si tienes resolución de gran contribuyente.',
      habitual_exporter: 'Si vendes al exterior con frecuencia.',
      construction_materials: 'Ferretería o materiales con control especial.',
      commercial_transport: 'Transporte comercial con datos adicionales.',
      fuel_sales: 'Combustibles: el comprobante puede requerir placa.',
      tourism_special_vat: 'Actividades turísticas con tarifa especial.',
    };
    return descriptions[code] ?? 'Si no estás seguro, déjalo apagado y revísalo luego.';
  }

  sectorHint(item: OnboardingCatalogItemDto): string {
    const group = this.asString(item.payload?.['group']);
    if (item.itemCode === 'other_custom') return 'Escribe abajo tu actividad exacta.';
    return group ? `Categoría: ${group}` : 'Actividad del negocio.';
  }

  saveActiveStep(manual: boolean): void {
    if (!this.session() || this.saving()) return;
    this.saving.set(true);
    this.saveState.set('Guardando...');
    this.api.updateOnboardingStep(this.activeStepKey(), this.draft())
      .pipe(finalize(() => this.saving.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (session) => {
          this.applySession(session, false);
          this.saveState.set('Guardado');
          if (manual) this.feedback.set('Paso guardado correctamente.');
        },
        error: (error) => {
          this.saveState.set('Error al guardar');
          this.feedback.set(this.resolveErrorMessage(error, 'No se pudo guardar este paso. Revisa conexion o permisos.'));
        },
      });
  }

  publish(): void {
    if (this.publishing()) return;
    this.publishing.set(true);
    this.saveState.set('Publicando...');
    this.api.updateOnboardingStep(this.activeStepKey(), this.draft())
      .pipe(
        switchMap(() => this.api.publishOnboarding()),
        finalize(() => this.publishing.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (session) => {
          this.applySession(session, true);
          this.feedback.set('Negocio activado. El flujo queda listo para operar.');
          this.saveState.set('Publicado');
        },
        error: (error) => {
          this.feedback.set(this.resolveErrorMessage(error, 'No se pudo publicar. Completa la lista critica y vuelve a intentar.'));
          this.saveState.set('Publicación rechazada');
        },
      });
  }

  validateRuc(): void {
    const ruc = this.field('ruc');
    if (!ruc) return;
    this.api.validateOnboardingRuc(ruc)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ message }) => this.feedback.set(message));
  }

  previewLogo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!this.validLogoFile(file)) {
      input.value = '';
      return;
    }
    this.logoFile.set(file);
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = String(reader.result ?? '');
      this.logoPreview.set(dataUrl);
      this.setField('logoDataUrl', dataUrl);
    };
    reader.readAsDataURL(file);
  }

  uploadLogo(): void {
    const file = this.logoFile();
    if (!file) {
      this.feedback.set('Selecciona primero el logo del negocio.');
      return;
    }
    this.logoUploading.set(true);
    this.api.uploadRideLogo(file)
      .pipe(finalize(() => this.logoUploading.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (asset) => {
          this.setField('logoUrl', asset.publicUrl || asset.path);
          this.setField('logoStoragePath', asset.path);
          this.feedback.set('Logo guardado para los comprobantes del negocio.');
        },
        error: () => this.feedback.set('No se pudo subir el logo. Revisa que sea PNG, JPG o WebP y que no sea pesado.'),
      });
  }

  dimensionLabel(value: string): string {
    const labels: Record<string, string> = {
      BRANCH: 'Local o sucursal',
      BUSINESS_LINE: 'Línea de negocio',
      WAREHOUSE: 'Bodega',
      COST_CENTER: 'Centro de costo',
      SALESPERSON: 'Vendedor',
      PROJECT: 'Proyecto',
    };
    return labels[value] ?? value;
  }

  private validLogoFile(file: File): boolean {
    const allowed = ['image/png', 'image/jpeg', 'image/webp'];
    const extensionOk = /\.(png|jpe?g|webp)$/i.test(file.name);
    if (!allowed.includes(file.type) && !extensionOk) {
      this.feedback.set('El logo debe ser PNG, JPG o WebP.');
      return false;
    }
    if (file.size > 1_500_000) {
      this.feedback.set('El logo debe pesar máximo 1.5MB. Usa una imagen optimizada.');
      return false;
    }
    return true;
  }

  selectCertificate(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.certificateFile.set(input.files?.[0] ?? null);
  }

  uploadCertificate(): void {
    const file = this.certificateFile();
    if (!file) {
      this.feedback.set('Selecciona el archivo .p12 de tu firma electrónica.');
      return;
    }
    if (!this.certificatePassword()) {
      this.feedback.set('Escribe la clave de la firma electrónica.');
      return;
    }
    this.certificateUploading.set(true);
    this.api.uploadOnboardingSriCertificate({
      file,
      password: this.certificatePassword(),
      alias: this.field('certificateAlias'),
      environment: this.field('environment') || '1',
    })
      .pipe(finalize(() => this.certificateUploading.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (session) => {
          this.certificatePassword.set('');
          this.applySession(session, true);
          this.feedback.set('Firma electrónica guardada de forma segura para este negocio.');
        },
        error: (error) => this.feedback.set(this.resolveErrorMessage(error, 'No se pudo guardar la firma. Revisa que el .p12 y la clave sean correctos.')),
      });
  }

  selectRideReference(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.rideReferenceFile.set(input.files?.[0] ?? null);
  }

  uploadRideReference(): void {
    const file = this.rideReferenceFile();
    if (!file) {
      this.feedback.set('Selecciona el modelo de comprobante que quieres usar como referencia.');
      return;
    }
    this.rideReferenceUploading.set(true);
    this.api.uploadOnboardingRideReference(file)
      .pipe(finalize(() => this.rideReferenceUploading.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (session) => {
          this.applySession(session, true);
          this.feedback.set('Modelo de comprobante guardado. Se usará como referencia de diseño.');
        },
        error: (error) => this.feedback.set(this.resolveErrorMessage(error, 'No se pudo subir el modelo. Usa PDF, imagen o documento liviano.')),
      });
  }

  legalValue(key: string): string {
    const draftValue = this.activeStepKey() === 'legal-entity' ? this.asString(this.draft()[key]) : '';
    const sessionValue = this.asString(this.session()?.legalEntity?.[key]);
    return draftValue || sessionValue;
  }

  initials(): string {
    const name = this.legalValue('tradeName') || this.session()?.tenantName || 'IV';
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part.charAt(0).toUpperCase()).join('') || 'IV';
  }

  checklistRows(): Array<{ key: string; label: string; done: boolean }> {
    const checklist = this.session()?.checklist ?? {};
    return [
      { key: 'business-discovery', label: 'Actividad del negocio definida', done: checklist['business-discovery'] === true },
      { key: 'legal-entity', label: 'Identidad legal guardada', done: checklist['legal-entity'] === true },
      { key: 'ruc-format', label: 'RUC con 13 digitos', done: checklist['ruc-format'] === true },
      { key: 'tax-profile', label: 'Perfil fiscal SRI revisado', done: checklist['tax-profile'] === true },
      { key: 'certificate-connectivity', label: 'Firma electronica guardada', done: checklist['certificate-connectivity'] === true },
      { key: 'sri-sequences', label: 'Establecimiento, punto y secuencia listos', done: checklist['sri-sequences'] === true },
      { key: 'invoice-document-enabled', label: 'Factura 01 habilitada', done: checklist['invoice-document-enabled'] === true },
      { key: 'accounting-template', label: 'Cuentas base definidas', done: checklist['accounting-template'] === true },
      { key: 'payment-methods', label: 'Formas de pago activas', done: checklist['payment-methods'] === true },
      { key: 'ride-legal-safe-mode', label: 'Datos legales del comprobante protegidos', done: checklist['ride-legal-safe-mode'] === true },
    ];
  }

  helpItems(): string[] {
    const map: Record<StepKey, string[]> = {
      'business-discovery': ['Actividad del negocio', 'Si vende productos, servicios o ambos', 'Número de locales o cajas'],
      'legal-entity': ['RUC', 'Razón social del SRI', 'Dirección matriz', 'Correo para comprobantes'],
      'tax-profile': ['RUC actualizado', 'Si eres RIMPE', 'Resoluciones del SRI si existen'],
      'certificate-connectivity': ['Archivo .p12', 'Clave de la firma', 'RUC igual al certificado'],
      'emission-points': ['Código de establecimiento', 'Caja o punto de emisión', 'Primer número de factura'],
      'accounting-template': ['Si llevas contabilidad', 'Bancos principales', 'Si vendes a crédito'],
      'sales-pos': ['Formas de pago', 'Si usas caja', 'Si das crédito a clientes'],
      purchases: ['Si registras facturas de proveedor', 'Si haces retenciones', 'Si apruebas compras'],
      inventory: ['Si manejas stock', 'Bodegas', 'Códigos de barra o lotes'],
      'crm-hr-bi': ['Clientes frecuentes', 'Equipo de trabajo', 'Reportes que quieres ver'],
      'brand-studio': ['Logo PNG, JPG o WebP', 'Ideal 600x300 px, fondo transparente', 'Modelo PDF, imagen o Word si ya tienes uno'],
      'publish-review': ['Lista completa', 'Firma subida', 'Datos legales correctos'],
    };
    return map[this.activeStepKey()];
  }

  private applySession(session: OnboardingSessionDto, hydrate: boolean): void {
    this.session.set(session);
    const step = this.isStep(session.currentStep) ? session.currentStep : 'business-discovery';
    if (hydrate) {
      this.activeStepKey.set(step);
      this.hydrateDraft();
    }
  }

  private hydrateDraft(): void {
    const session = this.session();
    if (!session) {
      this.draft.set({});
      return;
    }
    const payload = session[STEP_PAYLOAD_KEY[this.activeStepKey()]];
    this.draft.set(this.safeObject(payload));
    if (this.activeStepKey() === 'brand-studio') {
      this.logoPreview.set(this.asString(this.draft()['logoDataUrl'] ?? this.draft()['logoUrl']));
    }
  }

  private arrayField(key: string): string[] {
    const value = this.draft()[key];
    if (Array.isArray(value)) return value.map((item) => String(item));
    if (typeof value === 'string' && value) return value.split(',').map((item) => item.trim()).filter(Boolean);
    return [];
  }

  private safeObject(value: unknown): OnboardingJson {
    return value && typeof value === 'object' && !Array.isArray(value) ? { ...(value as OnboardingJson) } : {};
  }

  private isStep(value: string): value is StepKey {
    return this.steps.some((step) => step.key === value);
  }

  private asString(value: unknown): string {
    return value === null || value === undefined ? '' : String(value);
  }

  private resolveErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      const apiMessage = error.error?.message || error.error?.error || error.error?.detail;
      if (typeof apiMessage === 'string' && apiMessage.trim()) {
        return apiMessage;
      }
    }
    if (error instanceof Error && error.message.trim()) {
      return error.message;
    }
    return fallback;
  }
}
