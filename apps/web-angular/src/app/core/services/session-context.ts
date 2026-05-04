import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../api-base';
import { CommercialPlanSnapshot, NavigationModule } from '../models/commercial-plan';
import { AuthSession } from './auth-session';
import { HttpFeedback } from './http-feedback';

@Injectable({ providedIn: 'root' })
export class SessionContext {
  private readonly http = inject(HttpClient);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly authSession = inject(AuthSession);

  private readonly navigationCatalog: NavigationModule[] = [
    { path: '/pos', label: 'POS', requiredModule: 'pos' },
    { path: '/inventario', label: 'Inventario', requiredModule: 'inventory' },
    { path: '/clientes', label: 'Clientes', requiredModule: 'customers' },
    { path: '/reportes', label: 'Reportes', requiredModule: 'basic-reports' },
    { path: '/dashboard-ejecutivo', label: 'Dashboard', requiredModule: 'executive-dashboard' },
    { path: '/compras', label: 'Compras', requiredModule: 'purchases' },
    { path: '/insights', label: 'Insights', requiredModule: 'insights' },
    { path: '/contabilidad', label: 'Contabilidad', requiredModule: 'accounting' },
    { path: '/sri', label: 'SRI', requiredModule: 'sri' },
    { path: '/vision', label: 'Visión AI', requiredModule: 'vision-ai' },
    { path: '/sucursales', label: 'Sucursales', requiredModule: 'branches' },
    { path: '/bodegas', label: 'Bodegas', requiredModule: 'warehouses' },
    { path: '/contactos', label: 'Contactos', requiredModule: 'parties' },
    { path: '/aprobaciones', label: 'Aprobaciones', requiredModule: 'approvals' },
    { path: '/secuencias', label: 'Secuencias', requiredModule: 'branches' },
    { path: '/admin/negocios', label: 'Admin negocios', platformOnly: true },
    { path: '/perfil', label: 'Perfil' },
    { path: '/notificaciones', label: 'Notificaciones' },
    { path: '/ajustes', label: 'Ajustes' },
  ];

  private readonly storageKeyPrefix = 'tenant-plan:';

  readonly tenantCode = signal(this.readStoredTenantCode());
  readonly loading = signal(false);
  readonly snapshot = signal<CommercialPlanSnapshot | null>(this.readStoredSnapshot(this.readStoredTenantCode()));
  readonly loadError = signal('');
  readonly moduleAlert = signal('');
  readonly snapshotSource = signal<'live' | 'cache' | 'safe'>(
    this.readStoredSnapshot(this.readStoredTenantCode()) ? 'cache' : 'safe',
  );

  protected readonly enabledModules = signal<string[]>(this.snapshot()?.enabledModules ?? []);

  protected readonly navigation = signal<NavigationModule[]>(
    this.resolveNavigation(this.snapshot()?.enabledModules ?? []),
  );

  async ensureTenantPlanLoaded(force = false): Promise<void> {
    this.syncTenantWithUser();

    if (
      (!force && this.snapshotSource() === 'live' && this.snapshotMatchesTenant(this.snapshot(), this.tenantCode()))
      || this.loading()
    ) {
      return;
    }

    if (!this.authSession.isAuthenticated()) {
      this.applySnapshot(
        {
          tenantCode: this.tenantCode(),
          commercialPlan: 'UNKNOWN',
          enabledModules: [],
          availablePlans: ['START', 'PRO', 'VISION_AI'],
        },
        'safe',
      );
      this.loadError.set('Inicia sesión para cargar los módulos disponibles de tu negocio.');
      this.moduleAlert.set('');
      return;
    }

    this.loading.set(true);
    this.loadError.set('');
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('tenantCode', this.tenantCode());
    }

    try {
      const snapshot = await firstValueFrom(
        this.http.get<CommercialPlanSnapshot>(
          `${API_BASE_URL}/erp/tenants/${this.tenantCode()}/commercial-plan`,
          {
            headers: {
              'X-Tenant-Code': this.tenantCode(),
            },
          },
        ),
      );
      this.applySnapshot(
        {
          tenantCode: snapshot?.tenantCode ?? this.tenantCode(),
          commercialPlan: snapshot?.commercialPlan ?? 'START',
          enabledModules: snapshot?.enabledModules ?? [],
          availablePlans: snapshot?.availablePlans ?? ['START', 'PRO', 'VISION_AI'],
        },
        'live',
      );
      this.moduleAlert.set('');
      this.loadError.set('');
    } catch (error) {
      const cachedSnapshot = this.readStoredSnapshot(this.tenantCode());
      if (cachedSnapshot) {
        this.applySnapshot(cachedSnapshot, 'cache');
        const feedback = this.httpFeedback.fromError(
          error,
          'No pudimos actualizar la información del negocio en este momento. Te mostramos el último estado guardado.',
        );
        this.loadError.set(this.appendTrace(feedback.message, feedback.traceId));
      } else {
        const authUser = this.authSession.user();
        const authModules = authUser?.enabledModules ?? [];
        const authPlan = authUser?.commercialPlan ?? 'UNKNOWN';
        if (authModules.length > 0 && authPlan !== 'UNKNOWN') {
          this.applySnapshot(
            {
              tenantCode: this.tenantCode(),
              commercialPlan: authPlan,
              enabledModules: authModules,
              availablePlans: ['START', 'PRO', 'VISION_AI'],
            },
            'cache',
          );
          const feedback = this.httpFeedback.fromError(
            error,
            'No pudimos conectar el sistema en este momento. Revisa tu conexión e inténtalo de nuevo.',
          );
          this.loadError.set(this.appendTrace(feedback.message, feedback.traceId));
        } else {
          this.applySnapshot(
            {
              tenantCode: this.tenantCode(),
              commercialPlan: 'UNKNOWN',
              enabledModules: [],
              availablePlans: ['START', 'PRO', 'VISION_AI'],
            },
            'safe',
          );
          const feedback = this.httpFeedback.fromError(
            error,
            'No pudimos validar tu negocio en este momento. Reintenta en unos segundos.',
          );
          this.loadError.set(this.appendTrace(feedback.message, feedback.traceId));
        }
      }
    } finally {
      this.loading.set(false);
    }
  }

  async changeTenantCode(nextTenantCode: string): Promise<void> {
    const requested = this.normalizeTenantCode(nextTenantCode);
    const normalized = this.allowedTenantCode(requested);
    this.tenantCode.set(normalized);
    this.moduleAlert.set('');
    this.loadError.set('');
    if (normalized !== requested) {
      this.loadError.set(`Tu cuenta pertenece al negocio ${normalized}. Solo el administrador de plataforma puede cambiar a otro negocio.`);
    }
    const cachedSnapshot = this.readStoredSnapshot(normalized);
    if (cachedSnapshot) {
      this.applySnapshot(cachedSnapshot, 'cache');
    } else {
      this.applySnapshot(
        {
          tenantCode: normalized,
          commercialPlan: 'UNKNOWN',
          enabledModules: [],
          availablePlans: ['START', 'PRO', 'VISION_AI'],
        },
        'safe',
      );
    }
    await this.ensureTenantPlanLoaded(true);
  }

  async hasModule(moduleKey: string): Promise<boolean> {
    await this.ensureTenantPlanLoaded();
    return this.enabledModules().includes(moduleKey);
  }

  registerModuleDenied(moduleKey: string): void {
    const plan = this.planLabel(this.snapshot()?.commercialPlan || 'UNKNOWN');
    this.moduleAlert.set(`${this.moduleLabel(moduleKey)} no está disponible en tu acceso actual (${plan}).`);
  }

  clearModuleDenied(): void {
    this.moduleAlert.set('');
  }

  fallbackRoute(): string {
    const preferred = this.navigation().find((item) => item.requiredModule);
    return preferred?.path ?? '/perfil';
  }

  modules(): string[] {
    return this.enabledModules();
  }

  navItems(): NavigationModule[] {
    return this.navigation();
  }

  private applySnapshot(snapshot: CommercialPlanSnapshot, source: 'live' | 'cache' | 'safe'): void {
    this.snapshot.set(snapshot);
    this.snapshotSource.set(source);
    this.enabledModules.set(snapshot.enabledModules ?? []);
    this.navigation.set(this.resolveNavigation(snapshot.enabledModules ?? []));
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('tenantCode', snapshot.tenantCode);
      localStorage.setItem(this.storageKey(snapshot.tenantCode), JSON.stringify(snapshot));
    }
  }

  private resolveNavigation(enabledModules: string[]): NavigationModule[] {
    const isPlatformAdmin = this.authSession.user()?.platformAdmin === true;
    return this.navigationCatalog.filter(
      (item) => (!item.platformOnly || isPlatformAdmin)
        && (!item.requiredModule || enabledModules.includes(item.requiredModule)),
    );
  }

  private readStoredTenantCode(): string {
    const userTenant = this.userTenantCode();
    if (this.authSession.user() && userTenant && this.authSession.user()?.platformAdmin !== true) {
      return userTenant;
    }
    if (typeof localStorage === 'undefined') {
      return userTenant || 'default';
    }
    return this.normalizeTenantCode(localStorage.getItem('tenantCode') || userTenant || 'default');
  }

  private readStoredSnapshot(tenantCode: string): CommercialPlanSnapshot | null {
    if (typeof localStorage === 'undefined') {
      return null;
    }
    const raw = localStorage.getItem(this.storageKey(tenantCode));
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as CommercialPlanSnapshot;
      if (!parsed?.tenantCode || !Array.isArray(parsed.enabledModules)) {
        return null;
      }
      return parsed;
    } catch {
      return null;
    }
  }

  private storageKey(tenantCode: string): string {
    return `${this.storageKeyPrefix}${tenantCode.toLowerCase()}`;
  }

  private syncTenantWithUser(): void {
    const current = this.normalizeTenantCode(this.tenantCode());
    const allowed = this.allowedTenantCode(current);
    if (allowed === current) {
      return;
    }
    this.tenantCode.set(allowed);
    const cachedSnapshot = this.readStoredSnapshot(allowed);
    if (cachedSnapshot) {
      this.applySnapshot(cachedSnapshot, 'cache');
      return;
    }
    this.applySnapshot(
      {
        tenantCode: allowed,
        commercialPlan: 'UNKNOWN',
        enabledModules: [],
        availablePlans: ['START', 'PRO', 'VISION_AI'],
      },
      'safe',
    );
  }

  private allowedTenantCode(requestedTenantCode: string): string {
    const normalized = this.normalizeTenantCode(requestedTenantCode);
    const userTenant = this.userTenantCode();
    if (this.authSession.user() && userTenant && this.authSession.user()?.platformAdmin !== true) {
      return userTenant;
    }
    return normalized;
  }

  private userTenantCode(): string {
    const tenantCode = this.authSession.user()?.tenantCode;
    return tenantCode ? this.normalizeTenantCode(tenantCode) : '';
  }

  private normalizeTenantCode(tenantCode: string): string {
    return (tenantCode || '').trim().toLowerCase() || 'default';
  }

  private snapshotMatchesTenant(snapshot: CommercialPlanSnapshot | null, tenantCode: string): boolean {
    return this.normalizeTenantCode(snapshot?.tenantCode || '') === this.normalizeTenantCode(tenantCode);
  }

  private appendTrace(message: string, traceId?: string): string {
    return traceId ? `${message} Código de ayuda: ${traceId}` : message;
  }

  private moduleLabel(moduleKey: string): string {
    const item = this.navigationCatalog.find((candidate) => candidate.requiredModule === moduleKey);
    return item?.label || 'Este módulo';
  }

  private planLabel(plan: string): string {
    if (plan === 'VISION_AI') {
      return 'Inteligencia comercial';
    }
    if (plan === 'PRO') {
      return 'Crecimiento';
    }
    if (plan === 'START') {
      return 'Esencial';
    }
    return 'Acceso actual';
  }
}
