import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErpApi, SequenceDto, TaxRuleDto } from '../../core/services/erp-api';
import { SessionContext } from '../../core/services/session-context';
import { AuthSession } from '../../core/services/auth-session';
import { ToastService } from '../../core/services/toast';

type SettingsTab = 'negocio' | 'facturacion' | 'comercial' | 'modulos';

const IVA_RATE = 0.15;

@Component({
  selector: 'app-settings',
  imports: [FormsModule],
  template: `
    <div class="st page">

      <!-- Header -->
      <div class="st__head">
        <div>
          <p class="st__eyebrow">Configuración</p>
          <h2 class="st__title">Ajustes del negocio</h2>
          <p class="st__desc">Configura tu operación, facturación y accesos desde un solo lugar.</p>
        </div>
      </div>

      <!-- Tabs -->
      <div class="st__tabs">
        @for (tab of tabs; track tab.id) {
          <button
            type="button"
            class="st__tab"
            [class.st__tab--active]="activeTab() === tab.id"
            (click)="activeTab.set(tab.id)"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" [attr.d]="tab.icon"/>
            </svg>
            {{ tab.label }}
          </button>
        }
      </div>

      <!-- TAB: Negocio -->
      @if (activeTab() === 'negocio') {
        <div class="st__section" style="animation: pageEnter var(--dur-normal) var(--ease-out) both">
          <div class="st__cards">

            <div class="st__card">
              <h3 class="st__card-title">Identidad comercial</h3>
              <p class="st__card-desc">Datos del negocio activo en esta sesión.</p>
              <div class="st__fields">
                <div class="st__field">
                  <label class="st__label">Código del negocio</label>
                  <div class="st__field-row">
                    <input class="st__input" [value]="sessionCtx.tenantCode()" readonly />
                    <span class="st__badge st__badge--src" [attr.data-src]="sessionCtx.snapshotSource()">
                      {{ sourceLabel() }}
                    </span>
                  </div>
                </div>
                <div class="st__field">
                  <label class="st__label">Plan comercial</label>
                  <input class="st__input" [value]="planLabel()" readonly />
                </div>
                <div class="st__field">
                  <label class="st__label">Usuario activo</label>
                  <input class="st__input" [value]="userDisplay()" readonly />
                </div>
                <div class="st__field">
                  <label class="st__label">Rol de acceso</label>
                  <input class="st__input" [value]="roleLabel()" readonly />
                </div>
              </div>
            </div>

            <div class="st__card">
              <h3 class="st__card-title">Estado del sistema</h3>
              <p class="st__card-desc">Contexto operacional actual.</p>
              <div class="st__status-list">
                <div class="st__status-item">
                  <div class="st__status-dot st__status-dot--ok"></div>
                  <div>
                    <p class="st__status-name">Sesión autenticada</p>
                    <p class="st__status-sub">Token JWT activo y válido</p>
                  </div>
                </div>
                <div class="st__status-item">
                  <div class="st__status-dot" [class.st__status-dot--ok]="sessionCtx.snapshotSource() === 'live'" [class.st__status-dot--warn]="sessionCtx.snapshotSource() !== 'live'"></div>
                  <div>
                    <p class="st__status-name">Plan comercial</p>
                    <p class="st__status-sub">{{ sourceStatusDesc() }}</p>
                  </div>
                </div>
                <div class="st__status-item">
                  <div class="st__status-dot" [class.st__status-dot--ok]="sessionCtx.modules().length > 0" [class.st__status-dot--warn]="sessionCtx.modules().length === 0"></div>
                  <div>
                    <p class="st__status-name">Módulos cargados</p>
                    <p class="st__status-sub">{{ sessionCtx.modules().length }} módulos habilitados</p>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
      }

      <!-- TAB: Facturación -->
      @if (activeTab() === 'facturacion') {
        <div class="st__section" style="animation: pageEnter var(--dur-normal) var(--ease-out) both">

          <!-- IVA Banner -->
          <div class="st__iva-banner">
            <div class="st__iva-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 14.25l6-6m4.5-3.493V21.75l-3.75-1.5-3.75 1.5-3.75-1.5-3.75 1.5V4.757c0-1.108.806-2.057 1.907-2.185a48.507 48.507 0 0111.186 0c1.1.128 1.907 1.077 1.907 2.185zM9.75 9h.008v.008H9.75V9zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm4.125 4.5h.008v.008h-.008V13.5zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z"/>
              </svg>
            </div>
            <div class="st__iva-info">
              <strong class="st__iva-rate">IVA {{ ivaPercent }}%</strong>
              <span class="st__iva-desc">Tasa vigente en Ecuador — aplica a todas las operaciones gravadas</span>
            </div>
            <div class="st__iva-example">
              <span class="st__iva-ex-label">Ejemplo con $100:</span>
              <span class="st__iva-ex-val">Subtotal: {{ ivaExample.subtotal }} · IVA: {{ ivaExample.iva }}</span>
            </div>
          </div>

          <div class="st__cards">
            <div class="st__card">
              <h3 class="st__card-title">Secuencias de documentos</h3>
              <p class="st__card-desc">Numeración configurada para emisión de comprobantes.</p>
              @if (loadingSeq()) {
                <div class="st__loading-row">
                  <div class="st__mini-skeleton"></div>
                  <div class="st__mini-skeleton"></div>
                </div>
              } @else if (sequences().length === 0) {
                <p class="st__empty-note">No hay secuencias configuradas. Verifica el módulo de Contabilidad.</p>
              } @else {
                <div class="st__seq-list">
                  @for (seq of sequences(); track seq.id) {
                    <div class="st__seq-item" [class.st__seq-item--inactive]="!seq.active">
                      <div class="st__seq-left">
                        <span class="st__seq-code">{{ seq.documentCode }}</span>
                        <span class="st__seq-point">{{ seq.establishmentCode }}-{{ seq.emissionPointCode }}</span>
                      </div>
                      <div class="st__seq-right">
                        <span class="st__seq-num">Nº {{ seq.currentNumber }}</span>
                        <span class="st__seq-status" [class.st__seq-status--ok]="seq.active">
                          {{ seq.active ? 'Activa' : 'Inactiva' }}
                        </span>
                      </div>
                    </div>
                  }
                </div>
              }
            </div>

            <div class="st__card">
              <h3 class="st__card-title">Reglas de impuesto</h3>
              <p class="st__card-desc">Tasas configuradas para el cálculo automático de impuestos.</p>
              @if (loadingTax()) {
                <div class="st__loading-row">
                  <div class="st__mini-skeleton"></div>
                </div>
              } @else if (taxRules().length === 0) {
                <p class="st__empty-note">No hay reglas de impuesto configuradas.</p>
              } @else {
                <div class="st__tax-list">
                  @for (rule of taxRules(); track rule.id) {
                    <div class="st__tax-item">
                      <div class="st__tax-left">
                        <span class="st__tax-name">{{ rule.taxName }}</span>
                        <span class="st__tax-code">{{ rule.taxCode }} · {{ rule.taxScope }}</span>
                      </div>
                      <div class="st__tax-right">
                        <span class="st__tax-rate">{{ rule.rate }}%</span>
                        @if (rule.includedInPrice) {
                          <span class="st__tax-incl">Incluido</span>
                        }
                      </div>
                    </div>
                  }
                </div>
              }
            </div>
          </div>
        </div>
      }

      <!-- TAB: Comercial -->
      @if (activeTab() === 'comercial') {
        <div class="st__section" style="animation: pageEnter var(--dur-normal) var(--ease-out) both">
          <div class="st__cards">
            <div class="st__card">
              <h3 class="st__card-title">Plan actual</h3>
              <p class="st__card-desc">Acceso y capacidades habilitadas para este negocio.</p>
              <div class="st__plan-display">
                <div class="st__plan-badge" [attr.data-plan]="sessionCtx.snapshot()?.commercialPlan">
                  {{ planLabel() }}
                </div>
                <p class="st__plan-desc">{{ planDescription() }}</p>
              </div>
              <div class="st__plan-features">
                @for (feat of planFeatures(); track feat) {
                  <div class="st__plan-feat">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                    {{ feat }}
                  </div>
                }
              </div>
            </div>

            <div class="st__card">
              <h3 class="st__card-title">Planes disponibles</h3>
              <p class="st__card-desc">Opciones de upgrade para acceder a más funcionalidades.</p>
              <div class="st__plan-list">
                @for (plan of availablePlans(); track plan) {
                  <div class="st__plan-option" [class.st__plan-option--current]="plan === sessionCtx.snapshot()?.commercialPlan">
                    <div class="st__plan-option-name">{{ planLabelFor(plan) }}</div>
                    <div class="st__plan-option-desc">{{ planDescFor(plan) }}</div>
                    @if (plan === sessionCtx.snapshot()?.commercialPlan) {
                      <span class="st__plan-current-tag">Plan actual</span>
                    }
                  </div>
                }
              </div>
            </div>
          </div>
        </div>
      }

      <!-- TAB: Módulos -->
      @if (activeTab() === 'modulos') {
        <div class="st__section" style="animation: pageEnter var(--dur-normal) var(--ease-out) both">
          <div class="st__card">
            <h3 class="st__card-title">Módulos habilitados</h3>
            <p class="st__card-desc">Funciones disponibles según tu plan comercial activo.</p>
            <div class="st__modules-grid">
              @for (mod of allModules; track mod.key) {
                <div class="st__mod-card" [class.st__mod-card--enabled]="isEnabled(mod.key)" [class.st__mod-card--disabled]="!isEnabled(mod.key)">
                  <div class="st__mod-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path stroke-linecap="round" stroke-linejoin="round" [attr.d]="mod.icon"/>
                    </svg>
                  </div>
                  <div class="st__mod-info">
                    <span class="st__mod-name">{{ mod.label }}</span>
                    <span class="st__mod-desc">{{ mod.desc }}</span>
                  </div>
                  <div class="st__mod-status">
                    @if (isEnabled(mod.key)) {
                      <span class="st__mod-chip st__mod-chip--on">Activo</span>
                    } @else {
                      <span class="st__mod-chip st__mod-chip--off">No incluido</span>
                    }
                  </div>
                </div>
              }
            </div>
          </div>
        </div>
      }

    </div>
  `,
  styles: `
    .st {
      padding: 28px 32px;
      min-height: 100%;
    }

    .st__head {
      margin-bottom: 20px;
    }

    .st__eyebrow {
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: var(--aurora);
      margin: 0 0 4px;
    }

    .st__title {
      font-size: 22px;
      font-weight: 700;
      color: var(--text-strong);
      margin: 0 0 4px;
    }

    .st__desc {
      font-size: 13px;
      color: var(--text-muted);
      margin: 0;
    }

    /* Tabs */
    .st__tabs {
      display: flex;
      gap: 4px;
      margin-bottom: 24px;
      border-bottom: 1px solid var(--line);
      flex-wrap: wrap;
    }

    .st__tab {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 9px 16px;
      background: none;
      border: none;
      border-bottom: 2px solid transparent;
      margin-bottom: -1px;
      font-size: 13px;
      color: var(--text-muted);
      cursor: pointer;
      transition: all var(--dur-fast) ease;
      white-space: nowrap;
    }

    .st__tab svg { width: 15px; height: 15px; }
    .st__tab:hover { color: var(--text); }
    .st__tab--active { color: var(--aurora); border-bottom-color: var(--aurora); font-weight: 600; }

    /* Section */
    .st__section {}

    .st__cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;
    }

    .st__card {
      background: var(--bg-panel);
      border: 1px solid var(--line);
      border-radius: 12px;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .st__card-title { font-size: 14px; font-weight: 700; color: var(--text-strong); margin: 0; }
    .st__card-desc  { font-size: 12px; color: var(--text-muted); margin: 0; margin-top: -8px; }

    /* Fields */
    .st__fields { display: flex; flex-direction: column; gap: 10px; }

    .st__field { display: flex; flex-direction: column; gap: 4px; }

    .st__label {
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--text-faint);
    }

    .st__field-row { display: flex; align-items: center; gap: 8px; }

    .st__input {
      flex: 1;
      padding: 8px 12px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 8px;
      font-size: 13px;
      color: var(--text);
      outline: none;
    }

    .st__input[readonly] { cursor: default; opacity: 0.8; }

    .st__badge--src {
      padding: 3px 10px;
      border-radius: 10px;
      font-size: 11px;
      font-weight: 600;
      white-space: nowrap;
    }

    [data-src="live"]  { background: var(--ok-bg); color: var(--ok); }
    [data-src="cache"] { background: var(--warn-bg); color: var(--warn); }
    [data-src="safe"]  { background: var(--surface-strong); color: var(--text-muted); }

    /* Status list */
    .st__status-list { display: flex; flex-direction: column; gap: 10px; }

    .st__status-item { display: flex; align-items: flex-start; gap: 12px; }

    .st__status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--surface-strong);
      margin-top: 5px;
      flex-shrink: 0;
    }

    .st__status-dot--ok   { background: var(--ok); }
    .st__status-dot--warn { background: var(--warn); }

    .st__status-name { font-size: 13px; font-weight: 500; color: var(--text-strong); margin: 0 0 2px; }
    .st__status-sub  { font-size: 11px; color: var(--text-muted); margin: 0; }

    /* IVA banner */
    .st__iva-banner {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px 20px;
      background: var(--info-bg);
      border: 1px solid var(--info-border);
      border-radius: 12px;
      margin-bottom: 16px;
      flex-wrap: wrap;
    }

    .st__iva-icon {
      width: 44px;
      height: 44px;
      border-radius: 10px;
      background: var(--info);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .st__iva-icon svg { width: 22px; height: 22px; }

    .st__iva-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 120px; }

    .st__iva-rate {
      font-size: 18px;
      font-weight: 700;
      color: var(--info);
    }

    .st__iva-desc { font-size: 12px; color: var(--text-muted); }

    .st__iva-example {
      display: flex;
      flex-direction: column;
      gap: 2px;
      text-align: right;
    }

    .st__iva-ex-label { font-size: 10px; color: var(--text-faint); text-transform: uppercase; letter-spacing: 0.05em; }
    .st__iva-ex-val   { font-size: 12px; font-weight: 600; color: var(--text-strong); font-family: monospace; }

    /* Sequences */
    .st__seq-list { display: flex; flex-direction: column; gap: 8px; }

    .st__seq-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 12px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 8px;
    }

    .st__seq-item--inactive { opacity: 0.5; }

    .st__seq-left { display: flex; flex-direction: column; gap: 1px; }
    .st__seq-code { font-size: 13px; font-weight: 600; color: var(--text-strong); }
    .st__seq-point { font-size: 11px; color: var(--text-faint); font-family: monospace; }

    .st__seq-right { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }
    .st__seq-num { font-size: 12px; font-weight: 600; color: var(--text); font-family: monospace; }

    .st__seq-status {
      font-size: 10px;
      padding: 1px 7px;
      border-radius: 8px;
      background: var(--surface-strong);
      color: var(--text-muted);
    }
    .st__seq-status--ok { background: var(--ok-bg); color: var(--ok); }

    /* Tax rules */
    .st__tax-list { display: flex; flex-direction: column; gap: 8px; }

    .st__tax-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 12px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 8px;
    }

    .st__tax-left { display: flex; flex-direction: column; gap: 1px; }
    .st__tax-name { font-size: 13px; font-weight: 600; color: var(--text-strong); }
    .st__tax-code { font-size: 11px; color: var(--text-faint); }

    .st__tax-right { display: flex; align-items: center; gap: 8px; }
    .st__tax-rate { font-size: 15px; font-weight: 700; color: var(--text-strong); }
    .st__tax-incl {
      font-size: 10px;
      padding: 2px 7px;
      background: var(--info-bg);
      color: var(--info);
      border-radius: 8px;
    }

    /* Plan */
    .st__plan-display { display: flex; flex-direction: column; gap: 8px; }

    .st__plan-badge {
      display: inline-flex;
      padding: 6px 18px;
      border-radius: 20px;
      font-size: 14px;
      font-weight: 700;
      background: var(--aurora-ghost);
      border: 1px solid var(--aurora-border);
      color: var(--aurora);
      align-self: flex-start;
    }

    [data-plan="VISION_AI"] { background: var(--info-bg); border-color: var(--info-border); color: var(--info); }
    [data-plan="START"]     { background: var(--surface-strong); border-color: var(--line-strong); color: var(--text); }

    .st__plan-desc { font-size: 12px; color: var(--text-muted); margin: 0; }

    .st__plan-features { display: flex; flex-direction: column; gap: 6px; }

    .st__plan-feat {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: var(--text);
    }

    .st__plan-feat svg { width: 16px; height: 16px; color: var(--ok); flex-shrink: 0; }

    .st__plan-list { display: flex; flex-direction: column; gap: 10px; }

    .st__plan-option {
      padding: 12px 16px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 8px;
      position: relative;
    }

    .st__plan-option--current {
      background: var(--aurora-ghost);
      border-color: var(--aurora-border);
    }

    .st__plan-option-name { font-size: 13px; font-weight: 700; color: var(--text-strong); margin-bottom: 2px; }
    .st__plan-option-desc { font-size: 12px; color: var(--text-muted); }

    .st__plan-current-tag {
      position: absolute;
      top: 10px;
      right: 12px;
      font-size: 10px;
      font-weight: 600;
      padding: 2px 8px;
      background: var(--aurora);
      color: var(--bg);
      border-radius: 10px;
    }

    /* Modules grid */
    .st__modules-grid { display: flex; flex-direction: column; gap: 8px; }

    .st__mod-card {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 12px 14px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 8px;
      transition: all var(--dur-fast) ease;
    }

    .st__mod-card--enabled { border-left: 3px solid var(--ok); }
    .st__mod-card--disabled { opacity: 0.5; }

    .st__mod-icon {
      width: 36px;
      height: 36px;
      border-radius: 8px;
      background: var(--surface-strong);
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-muted);
      flex-shrink: 0;
    }

    .st__mod-card--enabled .st__mod-icon { background: var(--ok-bg); color: var(--ok); }
    .st__mod-icon svg { width: 17px; height: 17px; }

    .st__mod-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
    .st__mod-name { font-size: 13px; font-weight: 600; color: var(--text-strong); }
    .st__mod-desc { font-size: 11px; color: var(--text-muted); }

    .st__mod-chip {
      font-size: 10px;
      font-weight: 600;
      padding: 2px 9px;
      border-radius: 10px;
      white-space: nowrap;
    }

    .st__mod-chip--on  { background: var(--ok-bg);         color: var(--ok);        }
    .st__mod-chip--off { background: var(--surface-strong); color: var(--text-faint); }

    /* Loading helpers */
    .st__loading-row { display: flex; flex-direction: column; gap: 8px; }

    .st__mini-skeleton {
      height: 42px;
      background: var(--surface);
      border-radius: 8px;
      animation: fadeIn 1.5s ease infinite alternate;
    }

    .st__empty-note { font-size: 13px; color: var(--text-faint); margin: 0; }
  `,
})
export class Settings implements OnInit {
  private readonly api = inject(ErpApi);
  protected readonly sessionCtx = inject(SessionContext);
  private readonly authSession = inject(AuthSession);
  private readonly toast = inject(ToastService);

  readonly ivaRate = IVA_RATE;
  readonly ivaPercent = Math.round(IVA_RATE * 100);
  readonly ivaExample = {
    subtotal: `$${(100 / (1 + IVA_RATE)).toFixed(2)}`,
    iva: `$${(100 - 100 / (1 + IVA_RATE)).toFixed(2)}`,
  };

  protected readonly activeTab = signal<SettingsTab>('negocio');
  protected readonly sequences = signal<SequenceDto[]>([]);
  protected readonly taxRules = signal<TaxRuleDto[]>([]);
  protected readonly loadingSeq = signal(false);
  protected readonly loadingTax = signal(false);

  protected readonly availablePlans = computed(
    () => this.sessionCtx.snapshot()?.availablePlans ?? ['START', 'PRO', 'VISION_AI'],
  );

  protected readonly tabs = [
    { id: 'negocio'     as SettingsTab, label: 'Negocio',     icon: 'M2.25 21h19.5m-18-18v18m10.5-18v18m6-13.5V21M6.75 6.75h.75m-.75 3h.75m-.75 3h.75m3-6h.75m-.75 3h.75m-.75 3h.75M6.75 21v-3.375c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21M3 3h12m-.75 4.5H21m-3.75 3.75h.008v.008h-.008v-.008zm0 3h.008v.008h-.008v-.008zm0 3h.008v.008h-.008v-.008z' },
    { id: 'facturacion' as SettingsTab, label: 'Facturación', icon: 'M9 14.25l6-6m4.5-3.493V21.75l-3.75-1.5-3.75 1.5-3.75-1.5-3.75 1.5V4.757c0-1.108.806-2.057 1.907-2.185a48.507 48.507 0 0111.186 0c1.1.128 1.907 1.077 1.907 2.185z' },
    { id: 'comercial'   as SettingsTab, label: 'Comercial',   icon: 'M2.25 18.75a60.07 60.07 0 0115.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 013 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 00-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 01-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 003 15h-.75M15 10.5a3 3 0 11-6 0 3 3 0 016 0zm3 0h.008v.008H18V10.5zm-12 0h.008v.008H6V10.5z' },
    { id: 'modulos'     as SettingsTab, label: 'Módulos',     icon: 'M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zM3.75 15.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zM13.5 6a2.25 2.25 0 012.25-2.25H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zM13.5 15.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z' },
  ];

  protected readonly allModules = [
    { key: 'pos',                label: 'POS',           desc: 'Punto de venta para cobro rápido',      icon: 'M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.25L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z' },
    { key: 'inventory',          label: 'Inventario',    desc: 'Control de stock y movimientos',        icon: 'M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z' },
    { key: 'customers',          label: 'Clientes',      desc: 'CRM y seguimiento de clientes',         icon: 'M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z' },
    { key: 'basic-reports',      label: 'Reportes',      desc: 'Reportes de ventas y resultados',       icon: 'M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z' },
    { key: 'executive-dashboard',label: 'Dashboard',     desc: 'Métricas ejecutivas en tiempo real',    icon: 'M7.5 14.25v2.25m3-4.5v4.5m3-6.75v6.75m3-9v9M6 20.25h12A2.25 2.25 0 0020.25 18V6A2.25 2.25 0 0018 3.75H6A2.25 2.25 0 003.75 6v12A2.25 2.25 0 006 20.25z' },
    { key: 'purchases',          label: 'Compras',       desc: 'Gestión de proveedores y compras',      icon: 'M15.75 10.5V6a3.75 3.75 0 10-7.5 0v4.5m11.356-1.993l1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 01-1.12-1.243l1.264-12A1.125 1.125 0 015.513 7.5h12.974c.576 0 1.059.435 1.119 1.007z' },
    { key: 'insights',           label: 'Insights',      desc: 'Análisis e inteligencia de negocio',    icon: 'M3.75 3v11.25A2.25 2.25 0 006 16.5h2.25M3.75 3h-1.5m1.5 0h16.5m0 0h1.5m-1.5 0v11.25A2.25 2.25 0 0118 16.5h-2.25m-7.5 0h7.5m-7.5 0l-1 3m8.5-3l1 3m0 0l.5 1.5m-.5-1.5h-9.5m0 0l-.5 1.5M9 11.25v1.5M12 9v3.75m3-6v6' },
    { key: 'accounting',         label: 'Contabilidad',  desc: 'Diario, cuentas por cobrar y pagar',    icon: 'M12 7.5h1.5m-1.5 3h1.5m-7.5 3h7.5m-7.5 3h7.5m3-9h3.375c.621 0 1.125.504 1.125 1.125V18a2.25 2.25 0 01-2.25 2.25M16.5 7.5V18a2.25 2.25 0 002.25 2.25M16.5 7.5V4.875c0-.621-.504-1.125-1.125-1.125H4.125C3.504 3.75 3 4.254 3 4.875V18a2.25 2.25 0 002.25 2.25h13.5M6 7.5h3v3H6V7.5z' },
    { key: 'sri',                label: 'SRI',           desc: 'Facturación electrónica y autorización', icon: 'M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z' },
    { key: 'vision-ai',          label: 'Visión AI',     desc: 'Afluencia, conversión e IA visual',     icon: 'M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z M15 12a3 3 0 11-6 0 3 3 0 016 0z' },
  ];

  ngOnInit(): void {
    this.loadFacturacion();
  }

  private loadFacturacion(): void {
    this.loadingSeq.set(true);
    this.loadingTax.set(true);

    this.api.getSequences().subscribe({
      next: (seqs) => { this.sequences.set(seqs); this.loadingSeq.set(false); },
      error: () => this.loadingSeq.set(false),
    });

    this.api.getTaxRules().subscribe({
      next: (rules) => { this.taxRules.set(rules); this.loadingTax.set(false); },
      error: () => this.loadingTax.set(false),
    });
  }

  protected isEnabled(key: string): boolean {
    return this.sessionCtx.modules().includes(key);
  }

  protected planLabel(): string {
    return this.planLabelFor(this.sessionCtx.snapshot()?.commercialPlan ?? 'UNKNOWN');
  }

  protected planLabelFor(plan: string): string {
    if (plan === 'VISION_AI') return 'Visión AI';
    if (plan === 'PRO') return 'Crecimiento';
    if (plan === 'START') return 'Esencial';
    return 'Por validar';
  }

  protected planDescFor(plan: string): string {
    if (plan === 'VISION_AI') return 'Inteligencia visual, afluencia, conversión y predicciones con IA.';
    if (plan === 'PRO') return 'Contabilidad, compras, SRI, insights y reportes avanzados.';
    if (plan === 'START') return 'POS, inventario, clientes y reportes básicos.';
    return 'Plan no identificado.';
  }

  protected planDescription(): string {
    return this.planDescFor(this.sessionCtx.snapshot()?.commercialPlan ?? 'UNKNOWN');
  }

  protected planFeatures(): string[] {
    const plan = this.sessionCtx.snapshot()?.commercialPlan ?? 'UNKNOWN';
    if (plan === 'VISION_AI') return [
      'Afluencia en tiempo real con cámara', 'Tasa de conversión por hora', 'Predicciones con IA',
      'Insights de negocio automáticos', 'Horas pico y estadísticas', 'Dashboard ejecutivo completo',
    ];
    if (plan === 'PRO') return [
      'Contabilidad con diario de asientos', 'Compras y gestión de proveedores',
      'Facturación electrónica SRI', 'Reportes de ventas avanzados', 'Insights de negocio',
    ];
    if (plan === 'START') return [
      'Punto de venta (POS)', 'Control de inventario', 'Gestión de clientes', 'Reportes básicos de ventas',
    ];
    return [];
  }

  protected sourceLabel(): string {
    const src = this.sessionCtx.snapshotSource();
    if (src === 'live') return 'En vivo';
    if (src === 'cache') return 'Caché';
    return 'Seguro';
  }

  protected sourceStatusDesc(): string {
    const src = this.sessionCtx.snapshotSource();
    if (src === 'live') return 'Datos actualizados desde el servidor';
    if (src === 'cache') return 'Usando datos guardados — servidor no disponible';
    return 'Modo seguro — sin datos del servidor';
  }

  protected userDisplay(): string {
    const u = this.authSession.user();
    return u?.fullName || u?.email || 'Operador';
  }

  protected roleLabel(): string {
    const role = this.authSession.user()?.role || 'USER';
    const map: Record<string, string> = { ADMIN: 'Administrador', OWNER: 'Propietario', CASHIER: 'Cajero', VIEWER: 'Consulta' };
    return map[role] ?? 'Operador';
  }
}
