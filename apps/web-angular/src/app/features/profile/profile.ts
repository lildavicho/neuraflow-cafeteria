import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthSession } from '../../core/services/auth-session';
import { SessionContext } from '../../core/services/session-context';
import { ThemeService } from '../../core/services/theme';
import { ToastService } from '../../core/services/toast';
import { moduleLabel } from '../../core/labels';

@Component({
  selector: 'app-profile',
  imports: [FormsModule],
  template: `
    <div class="pf page">

      <!-- Header -->
      <div class="pf__head">
        <div class="pf__avatar-wrap">
          <div class="pf__avatar">{{ userInitial() }}</div>
          <div class="pf__avatar-badge" [attr.data-role]="userRole()"></div>
        </div>
        <div class="pf__head-info">
          <h2 class="pf__name">{{ userName() }}</h2>
          <p class="pf__email">{{ userEmail() }}</p>
          <div class="pf__chips">
            <span class="pf__chip pf__chip--role">{{ roleLabel() }}</span>
            <span class="pf__chip pf__chip--plan">{{ planLabel() }}</span>
            <span class="pf__chip" [class.pf__chip--ok]="isOnline" [class.pf__chip--warn]="!isOnline">
              {{ isOnline ? 'Conectado' : 'Sin conexión' }}
            </span>
          </div>
        </div>
      </div>

      <div class="pf__grid">

        <!-- Contexto del negocio -->
        <div class="pf__card">
          <h3 class="pf__card-title">Negocio activo</h3>
          <p class="pf__card-desc">Cambia el negocio con el que deseas operar.</p>
          <div class="pf__field">
            <label class="pf__label">Código del negocio</label>
            @if (canSwitchTenant()) {
              <div class="pf__field-row">
                <input
                  class="pf__input"
                  [(ngModel)]="tenantDraft"
                  placeholder="ej. mi-negocio"
                  (keyup.enter)="applyTenant()"
                />
                <button type="button" class="pf__btn pf__btn--primary" (click)="applyTenant()" [disabled]="sessionCtx.loading()">
                  {{ sessionCtx.loading() ? 'Cambiando...' : 'Aplicar' }}
                </button>
              </div>
            } @else {
              <div class="pf__tenant-lock">
                <strong class="pf__tenant-lock__code">{{ sessionCtx.tenantCode() }}</strong>
                <small class="pf__hint">Tu cuenta opera solo en el negocio asignado por el administrador.</small>
              </div>
            }
          </div>
          <div class="pf__kpi-row">
            <div class="pf__kpi">
              <span class="pf__kpi-label">Negocio</span>
              <strong class="pf__kpi-val">{{ sessionCtx.tenantCode() }}</strong>
            </div>
            <div class="pf__kpi">
              <span class="pf__kpi-label">Módulos</span>
              <strong class="pf__kpi-val">{{ sessionCtx.modules().length }}</strong>
            </div>
            <div class="pf__kpi">
              <span class="pf__kpi-label">Fuente</span>
              <strong class="pf__kpi-val">{{ sourceLabel() }}</strong>
            </div>
          </div>
        </div>

        <!-- Módulos habilitados -->
        <div class="pf__card">
          <h3 class="pf__card-title">Módulos habilitados</h3>
          <p class="pf__card-desc">Funciones disponibles según tu plan.</p>
          @if (sessionCtx.modules().length === 0) {
            <p class="pf__empty">Sin módulos cargados. Verifica el código de negocio.</p>
          } @else {
            <div class="pf__modules">
              @for (mod of sessionCtx.modules(); track mod) {
                <span class="pf__module-chip">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                  {{ moduleLabel(mod) }}
                </span>
              }
            </div>
          }
        </div>

        <!-- Preferencias -->
        <div class="pf__card">
          <h3 class="pf__card-title">Preferencias</h3>
          <p class="pf__card-desc">Apariencia y comportamiento de la aplicación.</p>

          <div class="pf__pref-row">
            <div class="pf__pref-info">
              <span class="pf__pref-name">Tema</span>
              <span class="pf__pref-desc">{{ themeService.isDark ? 'Modo oscuro activo' : 'Modo claro activo' }}</span>
            </div>
            <button
              type="button"
              class="pf__toggle"
              [class.pf__toggle--on]="themeService.isDark"
              (click)="themeService.toggle()"
              [attr.aria-label]="themeService.isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
            >
              <span class="pf__toggle-knob"></span>
            </button>
          </div>

          <div class="pf__pref-row">
            <div class="pf__pref-info">
              <span class="pf__pref-name">Notificaciones</span>
              <span class="pf__pref-desc">Alertas de stock y operación</span>
            </div>
            <button
              type="button"
              class="pf__toggle pf__toggle--on"
              aria-label="Notificaciones activadas"
            >
              <span class="pf__toggle-knob"></span>
            </button>
          </div>
        </div>

        <!-- Seguridad -->
        <div class="pf__card">
          <h3 class="pf__card-title">Seguridad</h3>
          <p class="pf__card-desc">Protege tu sesión y mantén el acceso seguro.</p>

          <div class="pf__security-items">
            <div class="pf__security-item">
              <div class="pf__security-icon pf__security-icon--ok">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z"/>
                </svg>
              </div>
              <div>
                <p class="pf__security-name">Sesión activa</p>
                <p class="pf__security-sub">Token JWT vigente — cierra sesión al terminar</p>
              </div>
            </div>
            <div class="pf__security-item">
              <div class="pf__security-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z"/>
                </svg>
              </div>
              <div>
                <p class="pf__security-name">Acceso por rol</p>
                <p class="pf__security-sub">{{ roleLabel() }} — permisos según tu rol asignado</p>
              </div>
            </div>
          </div>

          <button type="button" class="pf__btn pf__btn--danger" (click)="logout()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"/>
            </svg>
            Cerrar sesión
          </button>
        </div>

      </div>
    </div>
  `,
  styles: `
    .pf {
      padding: 28px 32px;
      min-height: 100%;
    }

    .pf__head {
      display: flex;
      align-items: center;
      gap: 20px;
      margin-bottom: 28px;
      padding-bottom: 24px;
      border-bottom: 1px solid var(--line);
      flex-wrap: wrap;
    }

    .pf__avatar-wrap { position: relative; }

    .pf__avatar {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background: var(--aurora-ghost);
      border: 2px solid var(--aurora-border);
      color: var(--aurora);
      font-size: 24px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .pf__avatar-badge {
      position: absolute;
      bottom: 2px;
      right: 2px;
      width: 14px;
      height: 14px;
      background: var(--ok);
      border: 2px solid var(--bg-panel);
      border-radius: 50%;
    }

    .pf__head-info { display: flex; flex-direction: column; gap: 4px; }

    .pf__name {
      font-size: 20px;
      font-weight: 700;
      color: var(--text-strong);
      margin: 0;
    }

    .pf__email {
      font-size: 13px;
      color: var(--text-muted);
      margin: 0;
    }

    .pf__chips { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 4px; }

    .pf__chip {
      padding: 3px 10px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 600;
      background: var(--surface-strong);
      color: var(--text-muted);
      border: 1px solid var(--line);
    }

    .pf__chip--role { background: var(--aurora-ghost); border-color: var(--aurora-border); color: var(--aurora); }
    .pf__chip--plan { background: var(--info-bg); border-color: var(--info-border); color: var(--info); }
    .pf__chip--ok   { background: var(--ok-bg); border-color: transparent; color: var(--ok); }
    .pf__chip--warn { background: var(--warn-bg); border-color: transparent; color: var(--warn); }

    /* Grid layout */
    .pf__grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;
    }

    .pf__card {
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 12px;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .pf__card-title {
      font-size: 14px;
      font-weight: 700;
      color: var(--text-strong);
      margin: 0;
    }

    .pf__card-desc {
      font-size: 12px;
      color: var(--text-muted);
      margin: 0;
      margin-top: -8px;
    }

    .pf__field { display: flex; flex-direction: column; gap: 6px; }

    .pf__label {
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--text-faint);
    }

    .pf__field-row { display: flex; gap: 8px; }

    .pf__tenant-lock {
      display: grid;
      gap: 4px;
      padding: 10px 12px;
      border: 1px solid var(--line);
      border-radius: 8px;
      background: var(--surface-muted);
    }

    .pf__tenant-lock__code {
      font-size: 13px;
      color: var(--text-strong);
      font-weight: 700;
      letter-spacing: 0.01em;
    }

    .pf__input {
      flex: 1;
      padding: 8px 12px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 8px;
      font-size: 13px;
      color: var(--text);
      outline: none;
      transition: border-color var(--dur-fast) ease;
    }

    .pf__input:focus { border-color: var(--aurora-border); }
    .pf__input::placeholder { color: var(--text-faint); }
    .pf__input[readonly] {
      color: var(--text-muted);
      cursor: not-allowed;
      opacity: 0.8;
    }

    .pf__hint {
      font-size: 11px;
      color: var(--text-faint);
      line-height: 1.45;
    }

    .pf__btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 8px 16px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all var(--dur-fast) ease;
      white-space: nowrap;
      border: none;
    }

    .pf__btn svg { width: 15px; height: 15px; }

    .pf__btn--primary {
      background: var(--aurora);
      color: var(--bg);
    }
    .pf__btn--primary:hover:not(:disabled) { opacity: 0.85; }
    .pf__btn--primary:disabled { opacity: 0.5; cursor: not-allowed; }

    .pf__btn--danger {
      background: var(--danger-bg);
      border: 1px solid var(--danger-border, var(--danger));
      color: var(--danger);
      width: 100%;
      justify-content: center;
    }
    .pf__btn--danger:hover { background: var(--danger); color: #fff; }

    .pf__kpi-row {
      display: flex;
      gap: 0;
      border: 1px solid var(--line);
      border-radius: 8px;
      overflow: hidden;
    }

    .pf__kpi {
      flex: 1;
      padding: 10px 12px;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .pf__kpi + .pf__kpi { border-left: 1px solid var(--line); }

    .pf__kpi-label {
      font-size: 10px;
      color: var(--text-faint);
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }

    .pf__kpi-val { font-size: 13px; font-weight: 700; color: var(--text-strong); }

    .pf__empty { font-size: 13px; color: var(--text-faint); margin: 0; }

    .pf__modules { display: flex; flex-wrap: wrap; gap: 6px; }

    .pf__module-chip {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 4px 10px;
      background: var(--ok-bg);
      border-radius: 12px;
      font-size: 11px;
      font-weight: 500;
      color: var(--ok);
    }

    .pf__module-chip svg { width: 12px; height: 12px; }

    /* Toggle switch */
    .pf__pref-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 0;
      border-bottom: 1px solid var(--line);
    }

    .pf__pref-row:last-of-type { border-bottom: none; }

    .pf__pref-info { display: flex; flex-direction: column; gap: 2px; }
    .pf__pref-name { font-size: 13px; font-weight: 500; color: var(--text-strong); }
    .pf__pref-desc { font-size: 11px; color: var(--text-muted); }

    .pf__toggle {
      width: 40px;
      height: 22px;
      border-radius: 11px;
      background: var(--surface-strong);
      border: none;
      cursor: pointer;
      position: relative;
      flex-shrink: 0;
      transition: background var(--dur-fast) ease;
    }

    .pf__toggle--on { background: var(--aurora); }

    .pf__toggle-knob {
      position: absolute;
      top: 3px;
      left: 3px;
      width: 16px;
      height: 16px;
      border-radius: 50%;
      background: var(--bg-panel);
      transition: transform var(--dur-fast) ease;
      box-shadow: 0 1px 3px rgba(0,0,0,0.2);
    }

    .pf__toggle--on .pf__toggle-knob { transform: translateX(18px); }

    /* Security */
    .pf__security-items { display: flex; flex-direction: column; gap: 10px; }

    .pf__security-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
    }

    .pf__security-icon {
      width: 36px;
      height: 36px;
      border-radius: 8px;
      background: var(--surface-strong);
      border: 1px solid var(--line);
      color: var(--text-muted);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .pf__security-icon--ok {
      background: var(--ok-bg);
      border-color: transparent;
      color: var(--ok);
    }

    .pf__security-icon svg { width: 16px; height: 16px; }

    .pf__security-name { font-size: 13px; font-weight: 500; color: var(--text-strong); margin: 0 0 2px; }
    .pf__security-sub  { font-size: 11px; color: var(--text-muted); margin: 0; }
  `,
})
export class Profile {
  protected readonly authSession = inject(AuthSession);
  protected readonly sessionCtx = inject(SessionContext);
  protected readonly themeService = inject(ThemeService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected tenantDraft = this.sessionCtx.tenantCode();
  protected readonly isOnline = navigator.onLine;
  protected readonly canSwitchTenant = computed(() => this.authSession.user()?.platformAdmin === true);

  protected readonly userName = computed(
    () => this.authSession.user()?.fullName || this.authSession.user()?.email || 'Operador',
  );

  protected readonly userEmail = computed(() => this.authSession.user()?.email || '');

  protected readonly userInitial = computed(() => this.userName().charAt(0).toUpperCase());

  protected readonly userRole = computed(() => this.authSession.user()?.role || 'USER');

  protected roleLabel(): string {
    const role = this.userRole();
    if (role === 'ADMIN') return 'Administrador';
    if (role === 'BUYER') return 'Compras';
    if (role === 'CASHIER') return 'Cajero';
    if (role === 'CUSTOMER') return 'Cliente';
    return role || 'Operador';
  }

  protected planLabel(): string {
    const plan = this.sessionCtx.snapshot()?.commercialPlan || 'UNKNOWN';
    if (plan === 'VISION_AI') return 'Visión AI';
    if (plan === 'PRO') return 'Crecimiento';
    if (plan === 'START') return 'Esencial';
    if (this.sessionCtx.loading()) return 'Cargando...';
    return 'Por validar';
  }

  protected sourceLabel(): string {
    const src = this.sessionCtx.snapshotSource();
    if (src === 'live') return 'En vivo';
    if (src === 'cache') return 'Caché';
    return 'Seguro';
  }

  protected moduleLabel(key: string): string {
    return moduleLabel(key);
  }

  protected applyTenant(): void {
    if (!this.canSwitchTenant()) {
      this.tenantDraft = this.sessionCtx.tenantCode();
      this.toast.info('Negocio protegido', `Tu cuenta opera en: ${this.sessionCtx.tenantCode()}`);
      return;
    }
    const code = this.tenantDraft.trim();
    if (!code) return;
    this.sessionCtx.changeTenantCode(code).then(() => {
      this.tenantDraft = this.sessionCtx.tenantCode();
      this.toast.success('Negocio actualizado', `Ahora trabajas en: ${code}`);
    });
  }

  constructor() {
    void this.refreshTenantPlan();
  }

  private async refreshTenantPlan(): Promise<void> {
    await this.sessionCtx.ensureTenantPlanLoaded(true);
    this.tenantDraft = this.sessionCtx.tenantCode();
  }

  protected logout(): void {
    this.authSession.logout();
    this.router.navigate(['/login']);
  }
}
