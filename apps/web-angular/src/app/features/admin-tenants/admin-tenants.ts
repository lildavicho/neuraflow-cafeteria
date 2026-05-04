import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import {
  AdminTenantCatalogDto,
  AdminTenantDto,
  ErpApi,
  TenantCommercialPlanDto,
} from '../../core/services/erp-api';
import { HttpFeedback } from '../../core/services/http-feedback';
import { ToastService } from '../../core/services/toast';
import { UiFeedback } from '../../core/models/ui-feedback';
import { RequestFeedback } from '../shared/components/request-feedback';
import { SessionContext } from '../../core/services/session-context';
import { moduleLabel } from '../../core/labels';

@Component({
  selector: 'app-admin-tenants',
  imports: [FormsModule, RequestFeedback],
  template: `
    <div class="at page">
      <div class="at__head">
        <div>
          <p class="at__eyebrow">Administración</p>
          <h2 class="at__title">Negocios y módulos</h2>
          <p class="at__desc">Asigna planes y accesos por cuenta sin desplegar código.</p>
        </div>
        <button type="button" class="at__btn at__btn--ghost" [disabled]="loading()" (click)="reload()">
          Actualizar
        </button>
      </div>

      @if (feedback()) {
        <app-request-feedback
          [tone]="feedback()!.tone"
          [message]="feedback()!.message"
          [traceId]="feedback()!.traceId"
        />
      }

      <div class="at__layout">
        <section class="at__panel at__panel--list">
          <div class="at__panel-head">
            <div>
              <h3 class="at__panel-title">Cuentas</h3>
              <p class="at__panel-desc">{{ catalog().tenants.length }} negocios registrados</p>
            </div>
          </div>

          <div class="at__tenant-list">
            @if (loading()) {
              <div class="at__empty">Cargando negocios...</div>
            } @else if (catalog().tenants.length === 0) {
              <div class="at__empty">No hay negocios para administrar.</div>
            } @else {
              @for (tenant of catalog().tenants; track tenant.tenantCode) {
                <button
                  type="button"
                  class="at__tenant"
                  [class.at__tenant--active]="tenant.tenantCode === selectedTenantCode()"
                  (click)="selectTenant(tenant)"
                >
                  <span class="at__tenant-name">{{ tenant.tradeName || tenant.legalName }}</span>
                  <span class="at__tenant-meta">{{ tenant.tenantCode }} · {{ planLabel(tenant.commercialPlan) }}</span>
                  <span class="at__tenant-count">{{ tenant.enabledModules.length }} módulos · {{ tenant.userCount }} usuarios</span>
                </button>
              }
            }
          </div>
        </section>

        <section class="at__panel at__panel--detail">
          @if (selectedTenant(); as tenant) {
            <div class="at__panel-head at__panel-head--split">
              <div>
                <h3 class="at__panel-title">{{ tenant.tradeName || tenant.legalName }}</h3>
                <p class="at__panel-desc">{{ tenant.tenantCode }} · {{ tenant.userCount }} usuarios activos</p>
              </div>
              <span class="at__status" [class.at__status--off]="!tenant.active">
                {{ tenant.active ? 'Activo' : 'Inactivo' }}
              </span>
            </div>

            <div class="at__section">
              <label class="at__label" for="tenantPlan">Plan base</label>
              <div class="at__row">
                <select
                  id="tenantPlan"
                  class="at__select"
                  [ngModel]="selectedPlan()"
                  (ngModelChange)="selectedPlan.set($event)"
                >
                  @for (plan of catalog().availablePlans; track plan) {
                    <option [ngValue]="plan">{{ planLabel(plan) }}</option>
                  }
                </select>
                <button type="button" class="at__btn" [disabled]="saving()" (click)="savePlan(tenant)">
                  Guardar plan
                </button>
              </div>
            </div>

            <div class="at__section">
              <div class="at__section-head">
                <div>
                  <h4 class="at__section-title">Módulos habilitados</h4>
                  <p class="at__section-desc">Marca los módulos comprados para este negocio.</p>
                </div>
                <button type="button" class="at__btn" [disabled]="saving()" (click)="saveModules(tenant)">
                  Guardar módulos
                </button>
              </div>

              <div class="at__modules">
                @for (module of catalog().availableModules; track module.moduleKey) {
                  <label class="at__module" [class.at__module--on]="isModuleSelected(module.moduleKey)">
                    <input
                      type="checkbox"
                      [checked]="isModuleSelected(module.moduleKey)"
                      (change)="toggleModule(module.moduleKey, $any($event.target).checked)"
                    />
                    <span>
                      <strong>{{ moduleLabel(module.moduleKey) }}</strong>
                      <small>{{ module.moduleKey }}</small>
                    </span>
                  </label>
                }
              </div>
            </div>
          } @else {
            <div class="at__empty at__empty--center">Selecciona un negocio para editar su acceso.</div>
          }
        </section>

        <section class="at__panel at__panel--create">
          <div class="at__panel-head">
            <div>
              <h3 class="at__panel-title">Nuevo negocio</h3>
              <p class="at__panel-desc">Se crea con plan Esencial y luego puedes activar módulos.</p>
            </div>
          </div>

          <form class="at__form" (ngSubmit)="createTenant()" novalidate>
            <label class="at__field">
              <span class="at__label">Codigo</span>
              <input class="at__input" [(ngModel)]="newTenant.tenantCode" name="tenantCode" placeholder="bar-centro" />
            </label>
            <label class="at__field">
              <span class="at__label">Razon social</span>
              <input class="at__input" [(ngModel)]="newTenant.legalName" name="legalName" placeholder="Bar Centro" />
            </label>
            <label class="at__field">
              <span class="at__label">Nombre comercial</span>
              <input class="at__input" [(ngModel)]="newTenant.tradeName" name="tradeName" placeholder="Bar Centro" />
            </label>
            <div class="at__mini-grid">
              <label class="at__field">
                <span class="at__label">Pais</span>
                <input class="at__input" [(ngModel)]="newTenant.countryCode" name="countryCode" />
              </label>
              <label class="at__field">
                <span class="at__label">Moneda</span>
                <input class="at__input" [(ngModel)]="newTenant.currencyCode" name="currencyCode" />
              </label>
            </div>
            <button type="submit" class="at__btn at__btn--full" [disabled]="creating()">
              Crear negocio
            </button>
          </form>
        </section>
      </div>
    </div>
  `,
  styles: `
    .at {
      padding: 28px 32px;
      min-height: 100%;
    }

    .at__head,
    .at__panel-head,
    .at__section-head,
    .at__row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }

    .at__head { margin-bottom: 20px; }

    .at__eyebrow {
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: var(--aurora);
      margin: 0 0 4px;
    }

    .at__title {
      font-size: 22px;
      font-weight: 700;
      color: var(--text-strong);
      margin: 0 0 4px;
    }

    .at__desc,
    .at__panel-desc,
    .at__section-desc {
      font-size: 13px;
      color: var(--text-muted);
      margin: 0;
    }

    .at__layout {
      display: grid;
      grid-template-columns: minmax(240px, 300px) minmax(360px, 1fr) minmax(260px, 320px);
      gap: 16px;
      align-items: start;
      margin-top: 16px;
    }

    .at__panel {
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 8px;
      padding: 18px;
      display: grid;
      gap: 16px;
    }

    .at__panel-title {
      font-size: 14px;
      font-weight: 700;
      color: var(--text-strong);
      margin: 0 0 3px;
    }

    .at__tenant-list,
    .at__form,
    .at__section,
    .at__modules {
      display: grid;
      gap: 10px;
    }

    .at__tenant {
      display: grid;
      gap: 3px;
      text-align: left;
      padding: 12px;
      border: 1px solid var(--line);
      background: var(--surface);
      border-radius: 8px;
      color: var(--text);
      cursor: pointer;
      transition: border-color var(--dur-fast) ease, background var(--dur-fast) ease;
    }

    .at__tenant:hover,
    .at__tenant--active {
      border-color: var(--aurora-border);
      background: var(--aurora-ghost);
    }

    .at__tenant-name {
      font-size: 13px;
      font-weight: 700;
      color: var(--text-strong);
    }

    .at__tenant-meta,
    .at__tenant-count,
    .at__module small {
      font-size: 11px;
      color: var(--text-faint);
    }

    .at__label {
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--text-faint);
    }

    .at__input,
    .at__select {
      width: 100%;
      padding: 9px 11px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 8px;
      font-size: 13px;
      color: var(--text);
      outline: none;
    }

    .at__input:focus,
    .at__select:focus {
      border-color: var(--aurora-border);
      box-shadow: 0 0 0 3px var(--aurora-ghost);
    }

    .at__btn {
      border: 1px solid var(--aurora);
      background: var(--aurora);
      color: var(--bg);
      border-radius: 8px;
      padding: 9px 13px;
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;
      white-space: nowrap;
    }

    .at__btn--ghost {
      background: transparent;
      color: var(--aurora);
      border-color: var(--aurora-border);
    }

    .at__btn--full { width: 100%; justify-content: center; }
    .at__btn:disabled { opacity: 0.45; cursor: not-allowed; }

    .at__status {
      font-size: 11px;
      font-weight: 700;
      padding: 4px 9px;
      border-radius: 12px;
      color: var(--ok);
      background: var(--ok-bg);
    }

    .at__status--off {
      color: var(--warn);
      background: var(--warn-bg);
    }

    .at__section-title {
      font-size: 13px;
      font-weight: 700;
      color: var(--text-strong);
      margin: 0 0 3px;
    }

    .at__modules {
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    }

    .at__module {
      display: flex;
      gap: 10px;
      align-items: flex-start;
      padding: 11px;
      border: 1px solid var(--line);
      border-radius: 8px;
      background: var(--surface);
      cursor: pointer;
    }

    .at__module--on {
      border-color: var(--aurora-border);
      background: var(--aurora-ghost);
    }

    .at__module input { margin-top: 2px; accent-color: var(--aurora); }
    .at__module span { display: grid; gap: 2px; min-width: 0; }
    .at__module strong { font-size: 12px; color: var(--text-strong); }

    .at__field { display: grid; gap: 5px; }
    .at__mini-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }

    .at__empty {
      color: var(--text-muted);
      font-size: 13px;
      padding: 12px;
      border: 1px dashed var(--line);
      border-radius: 8px;
      background: var(--surface);
    }

    .at__empty--center {
      min-height: 220px;
      display: flex;
      align-items: center;
      justify-content: center;
      text-align: center;
    }

    @media (max-width: 1180px) {
      .at__layout { grid-template-columns: 1fr; }
    }

    @media (max-width: 640px) {
      .at { padding: 20px; }
      .at__head,
      .at__panel-head--split,
      .at__section-head,
      .at__row {
        align-items: stretch;
        flex-direction: column;
      }
      .at__mini-grid { grid-template-columns: 1fr; }
    }
  `,
})
export class AdminTenants implements OnInit {
  private readonly api = inject(ErpApi);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly toast = inject(ToastService);
  private readonly sessionContext = inject(SessionContext);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly creating = signal(false);
  protected readonly feedback = signal<UiFeedback | null>(null);
  protected readonly selectedTenantCode = signal('');
  protected readonly selectedPlan = signal('START');
  protected readonly selectedModules = signal<string[]>([]);
  protected readonly catalog = signal<AdminTenantCatalogDto>({
    tenants: [],
    availableModules: [],
    availablePlans: ['START', 'PRO', 'VISION_AI'],
  });

  protected newTenant = {
    tenantCode: '',
    legalName: '',
    tradeName: '',
    countryCode: 'EC',
    timeZone: 'America/Guayaquil',
    currencyCode: 'USD',
  };

  protected readonly selectedTenant = computed(() =>
    this.catalog().tenants.find((tenant) => tenant.tenantCode === this.selectedTenantCode()) ?? null,
  );

  ngOnInit(): void {
    void this.reload();
  }

  protected async reload(): Promise<void> {
    this.loading.set(true);
    this.feedback.set(null);
    try {
      const catalog = await firstValueFrom(this.api.getTenantAdminCatalog());
      this.catalog.set({
        tenants: catalog.tenants ?? [],
        availableModules: catalog.availableModules ?? [],
        availablePlans: catalog.availablePlans ?? ['START', 'PRO', 'VISION_AI'],
      });
      const current = this.selectedTenantCode();
      const selected = this.catalog().tenants.find((tenant) => tenant.tenantCode === current)
        ?? this.catalog().tenants[0]
        ?? null;
      if (selected) {
        this.selectTenant(selected);
      }
    } catch (error) {
      this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo cargar la administración de negocios.'));
    } finally {
      this.loading.set(false);
    }
  }

  protected selectTenant(tenant: AdminTenantDto): void {
    this.selectedTenantCode.set(tenant.tenantCode);
    this.selectedPlan.set(tenant.commercialPlan);
    this.selectedModules.set([...(tenant.enabledModules ?? [])]);
  }

  protected isModuleSelected(moduleKey: string): boolean {
    return this.selectedModules().includes(moduleKey);
  }

  protected toggleModule(moduleKey: string, checked: boolean): void {
    this.selectedModules.update((modules) => {
      const next = new Set(modules);
      if (checked) {
        next.add(moduleKey);
      } else {
        next.delete(moduleKey);
      }
      return [...next].sort();
    });
  }

  protected async savePlan(tenant: AdminTenantDto): Promise<void> {
    if (!this.selectedPlan()) {
      this.feedback.set(this.httpFeedback.warning('Selecciona un plan antes de guardar.'));
      return;
    }
    this.saving.set(true);
    this.feedback.set(null);
    try {
      const updated = await firstValueFrom(this.api.updateTenantPlan(tenant.tenantCode, this.selectedPlan()));
      this.patchTenantPlan(updated);
      this.toast.success(`Plan actualizado a ${this.planLabel(updated.commercialPlan)}`, tenant.tenantCode);
    } catch (error) {
      this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo guardar el plan del negocio.'));
    } finally {
      this.saving.set(false);
    }
  }

  protected async saveModules(tenant: AdminTenantDto): Promise<void> {
    if (this.selectedModules().length === 0) {
      this.feedback.set(this.httpFeedback.warning('Selecciona al menos un modulo antes de guardar.'));
      return;
    }
    this.saving.set(true);
    this.feedback.set(null);
    try {
      const updated = await firstValueFrom(this.api.updateTenantModules(tenant.tenantCode, this.selectedModules()));
      this.patchTenantPlan(updated);
      this.toast.success(
        `${updated.enabledModules?.length ?? 0} módulos actualizados`,
        tenant.tenantCode,
      );
    } catch (error) {
      this.feedback.set(this.httpFeedback.fromError(error, 'No se pudieron guardar los módulos del negocio.'));
    } finally {
      this.saving.set(false);
    }
  }

  protected async createTenant(): Promise<void> {
    const tenantCode = this.newTenant.tenantCode.trim();
    const legalName = this.newTenant.legalName.trim();
    if (!tenantCode || !legalName) {
      this.feedback.set(this.httpFeedback.warning('Ingresa código y razón social para crear el negocio.'));
      return;
    }
    if (!/^[A-Za-z0-9_-]{1,64}$/.test(tenantCode)) {
      this.feedback.set(this.httpFeedback.warning('El código solo puede contener letras, números, guiones y guion bajo (máx. 64 caracteres).'));
      return;
    }

    this.creating.set(true);
    this.feedback.set(null);
    try {
      const created = await firstValueFrom(this.api.createTenant({ ...this.newTenant, tenantCode, legalName }));
      this.catalog.update((catalog) => ({
        ...catalog,
        tenants: [...catalog.tenants, created].sort((left, right) =>
          left.tenantCode.localeCompare(right.tenantCode),
        ),
      }));
      this.selectTenant(created);
      this.newTenant = {
        tenantCode: '',
        legalName: '',
        tradeName: '',
        countryCode: 'EC',
        timeZone: 'America/Guayaquil',
        currencyCode: 'USD',
      };
      this.toast.success('Negocio creado', created.tenantCode);
    } catch (error) {
      this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo crear el negocio.'));
    } finally {
      this.creating.set(false);
    }
  }

  protected planLabel(plan: string): string {
    if (plan === 'VISION_AI') return 'Visión AI';
    if (plan === 'PRO') return 'Crecimiento';
    if (plan === 'START') return 'Esencial';
    return plan || 'Sin plan';
  }

  protected moduleLabel(moduleKey: string): string {
    return moduleLabel(moduleKey);
  }

  private patchTenantPlan(plan: TenantCommercialPlanDto): void {
    this.catalog.update((catalog) => ({
      ...catalog,
      tenants: catalog.tenants.map((tenant) =>
        tenant.tenantCode === plan.tenantCode
          ? {
              ...tenant,
              commercialPlan: plan.commercialPlan,
              enabledModules: plan.enabledModules ?? [],
            }
          : tenant,
      ),
    }));
    this.selectedPlan.set(plan.commercialPlan);
    this.selectedModules.set([...(plan.enabledModules ?? [])]);
    if (this.sessionContext.tenantCode().toLowerCase() === plan.tenantCode.toLowerCase()) {
      void this.sessionContext.changeTenantCode(plan.tenantCode);
    }
  }
}
