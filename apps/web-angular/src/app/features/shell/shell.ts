import { Component, DestroyRef, HostListener, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import {
  NavigationCancel,
  NavigationEnd,
  NavigationError,
  NavigationStart,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { AuthSession } from '../../core/services/auth-session';
import { SessionContext } from '../../core/services/session-context';
import { ThemeService } from '../../core/services/theme';
import { ErpApi } from '../../core/services/erp-api';
import { NavigationModule } from '../../core/models/commercial-plan';
import { Brand } from '../shared/components/brand';
import { RequestFeedback } from '../shared/components/request-feedback';
import { ToastOutlet } from '../shared/components/toast-outlet';
import { tenantDisplayLabel } from '../../core/labels';

type NavGroup = {
  title: string;
  items: NavigationModule[];
};

@Component({
  selector: 'app-shell',
  imports: [FormsModule, RouterLink, RouterLinkActive, RouterOutlet, Brand, RequestFeedback, ToastOutlet],
  template: `
    <div class="sh" [class.sh--route-busy]="routeTransitioning()">
      <div
        class="sh-mobile-backdrop"
        [class.sh-mobile-backdrop--open]="mobileNavOpen()"
        (click)="closeMobileNav()"
        aria-hidden="true"
      ></div>

      <!-- ══════════════════════════════
           SIDEBAR
      ══════════════════════════════ -->
      <aside id="erp-shell-sidebar" class="sh-sidebar" [class.sh-sidebar--open]="mobileNavOpen()">

        <!-- Logo / marca -->
        <div class="sh-brand">
          <app-brand size="small" />
          <div class="sh-brand__copy">
            <span class="sh-label">ERP Comercial</span>
            <p class="sh-brand__tagline">Vende, cobra y decide desde una sola vista</p>
          </div>
          <button
            type="button"
            class="sh-sidebar-close"
            (click)="closeMobileNav()"
            aria-label="Cerrar navegación"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="sh-divider"></div>

        <!-- Contexto del negocio -->
        <div class="sh-context">
          <div class="sh-context__head">
            <div>
              <span class="sh-label">Negocio activo</span>
              <strong class="sh-context__name" [title]="sessionContext.tenantCode()">{{ tenantDisplayName() }}</strong>
            </div>
            <span class="sh-badge" [attr.data-state]="sessionContext.snapshotSource()">
              {{ sourceBadge() }}
            </span>
          </div>

          @if (canSwitchTenant()) {
            <div class="sh-context__row">
              <label class="sh-input-group">
                <span class="sh-input-label">Cambiar negocio</span>
                <input
                  class="sh-input"
                  [(ngModel)]="tenantDraft"
                  placeholder="Código del negocio"
                  aria-label="Código del negocio"
                  (keyup.enter)="applyTenant()"
                />
              </label>
              <button
                type="button"
                class="sh-btn-icon"
                [disabled]="sessionContext.loading()"
                (click)="applyTenant()"
                aria-label="Aplicar negocio"
                title="Aplicar"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12h15m0 0l-6.75-6.75M19.5 12l-6.75 6.75" />
                </svg>
              </button>
            </div>
          }

          <div class="sh-plan-row">
            <span class="sh-plan-source">Estado: {{ sourceLabel() }}</span>
          </div>
        </div>

        <div class="sh-divider"></div>

        <!-- Navegación -->
        <nav class="sh-nav">
          @for (group of navGroups(); track group.title) {
            <div class="sh-nav-group">
              <span class="sh-nav-group__title">{{ group.title }}</span>
              @for (item of group.items; track item.path) {
                <a
                  [routerLink]="item.path"
                  routerLinkActive="sh-link--active"
                  class="sh-link"
                  (click)="closeMobileNav()"
                >
                  <span class="sh-link__label">{{ item.label }}</span>
                  <span class="sh-link__hint">{{ navHint(item.path) }}</span>
                </a>
              }
            </div>
          }
        </nav>

        <div class="sh-divider"></div>

        <!-- Usuario / sesión -->
        <div class="sh-user">
          <div class="sh-user__info">
            <div class="sh-user__avatar">
              {{ userInitial() }}
            </div>
            <div class="sh-user__meta">
              <strong>{{ authSession.user()?.fullName || authSession.user()?.email || 'Operador' }}</strong>
              <span>{{ roleLabel() }}</span>
            </div>
          </div>
          <button type="button" class="sh-logout" (click)="logout()" aria-label="Cerrar sesión">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
            </svg>
            Salir
          </button>
        </div>

      </aside>

      <!-- ══════════════════════════════
           CONTENIDO PRINCIPAL
      ══════════════════════════════ -->
      <div class="sh-main" [class.sh-main--route-busy]="routeTransitioning()">
        <div class="sh-route-progress" [class.sh-route-progress--active]="routeTransitioning()" aria-hidden="true"></div>

        <!-- Header de módulo -->
        <header class="sh-module-header" [class.sh-module-header--transitioning]="routeTransitioning()">
          <div class="sh-module-header__left">
            <span class="sh-label sh-label--section">{{ currentSection() }}</span>
            <h1 class="sh-module-header__title">{{ currentTitle() }}</h1>
            <p class="sh-module-header__desc">{{ currentDescription() }}</p>
          </div>
          <div class="sh-module-header__right">
            <button
              type="button"
              class="sh-hbtn sh-hbtn--menu"
              (click)="toggleMobileNav()"
              [attr.aria-expanded]="mobileNavOpen()"
              aria-controls="erp-shell-sidebar"
              aria-label="Abrir navegación"
              title="Módulos"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 6.75h15M4.5 12h15m-15 5.25h15" />
              </svg>
            </button>


            <!-- Plan chip -->
            <span class="sh-plan-tag" [attr.data-src]="sessionContext.snapshotSource()" [title]="sourceHeadline()">
              {{ planLabel() }}
            </span>

            <!-- Notificaciones con badge -->
            <a
              routerLink="/notificaciones"
              class="sh-hbtn"
              [class.sh-hbtn--notif]="notifCount() > 0"
              aria-label="Notificaciones"
              title="Notificaciones"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
              </svg>
              @if (notifCount() > 0) {
                <span class="sh-notif-badge">{{ notifCount() > 9 ? '9+' : notifCount() }}</span>
              }
            </a>

            <!-- Toggle de tema -->
            <button
              type="button"
              class="sh-hbtn"
              (click)="themeService.toggle()"
              [attr.aria-label]="themeService.isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
              [title]="themeService.isDark ? 'Modo claro' : 'Modo oscuro'"
            >
              @if (themeService.isDark) {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v2.25m6.364.386l-1.591 1.591M21 12h-2.25m-.386 6.364l-1.591-1.591M12 18.75V21m-4.773-4.227l-1.591 1.591M5.25 12H3m4.227-4.773L5.636 5.636M15.75 12a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0z" />
                </svg>
              } @else {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z" />
                </svg>
              }
            </button>

            <!-- Perfil (avatar) -->
            <a routerLink="/perfil" class="sh-hbtn sh-hbtn--avatar" title="Mi perfil" aria-label="Mi perfil">
              {{ userInitial() }}
            </a>

            <!-- Cerrar sesión -->
            <button
              type="button"
              class="sh-hbtn sh-hbtn--danger"
              (click)="logout()"
              title="Cerrar sesión"
              aria-label="Cerrar sesión"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
              </svg>
            </button>

          </div>
        </header>

        <!-- Alertas del sistema -->
        @if (sessionContext.loadError()) {
          <app-request-feedback tone="warning" [message]="sessionContext.loadError()" />
        }
        @if (sessionContext.moduleAlert()) {
          <app-request-feedback tone="warning" [message]="sessionContext.moduleAlert()" />
        }

        <!-- Contenido del módulo -->
        <main class="sh-outlet" [class.sh-outlet--transitioning]="routeTransitioning()">
          <router-outlet />
        </main>

      </div>
    </div>

    <!-- Toast overlay (global) -->
    <app-toast-outlet />
  `,
  styles: `
    :host { display: contents; }

    /* ─── Layout principal ─── */
    .sh {
      display: grid;
      grid-template-columns: 264px 1fr;
      height: 100vh;
      overflow: hidden;
      background: var(--bg);
    }

    /* ════════════════════════
       SIDEBAR
    ════════════════════════ */
    .sh-sidebar {
      position: sticky;
      top: 0;
      height: 100vh;
      overflow-y: auto;
      overflow-x: hidden;
      display: flex;
      flex-direction: column;
      gap: 0;
      padding: 24px 20px;
      background: var(--bg-panel);
      border-right: 1px solid var(--line);
      transition:
        background-color var(--dur-normal) ease,
        border-color var(--dur-normal) ease,
        transform var(--dur-normal) var(--ease-out),
        opacity var(--dur-normal) var(--ease-out),
        box-shadow var(--dur-normal) var(--ease-out);
    }

    .sh-sidebar::-webkit-scrollbar { width: 3px; }
    .sh-sidebar::-webkit-scrollbar-track { background: transparent; }
    .sh-sidebar::-webkit-scrollbar-thumb { background: var(--line-strong); border-radius: 99px; }

    .sh-mobile-backdrop,
    .sh-sidebar-close,
    .sh-hbtn--menu {
      display: none;
    }

    /* ─── Marca ─── */
    .sh-brand {
      display: grid;
      align-items: start;
      gap: 8px;
      padding-bottom: 4px;
    }

    .sh-brand__copy { display: grid; gap: 3px; }

    .sh-brand__tagline {
      font-size: 10px;
      font-weight: 400;
      color: var(--text-faint);
      line-height: 1.4;
      margin: 0;
    }

    /* ─── Label genérico ─── */
    .sh-label {
      font-size: 9.5px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.2em;
      color: var(--text-faint);
    }

    .sh-label--section { color: var(--aurora-dim); }

    /* ─── Divisor ─── */
    .sh-divider {
      height: 1px;
      background: var(--line);
      margin: 16px 0;
      flex-shrink: 0;
    }

    /* ─── Contexto del negocio ─── */
    .sh-context { display: grid; gap: 12px; }

    .sh-context__head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 8px;
    }

    .sh-context__name {
      display: block;
      margin-top: 4px;
      font-size: 13px;
      font-weight: 600;
      color: var(--text);
      letter-spacing: -0.01em;
      user-select: none;
      pointer-events: none;
    }

    .sh-badge {
      font-size: 9px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.12em;
      padding: 3px 8px;
      border-radius: 2px;
      border: 1px solid var(--aurora-border);
      color: var(--aurora-dim);
      background: var(--aurora-ghost);
      white-space: nowrap;
    }

    .sh-badge[data-state='cache'] {
      border-color: var(--warn-border);
      color: var(--warn);
      background: var(--warn-bg);
    }

    .sh-badge[data-state='safe'] {
      border-color: var(--line-strong);
      color: var(--text-faint);
      background: transparent;
    }

    .sh-context__row {
      display: flex;
      gap: 6px;
      align-items: center;
    }

    .sh-input-group {
      flex: 1;
      min-width: 0;
      display: grid;
      gap: 2px;
    }

    .sh-input-label {
      font-size: 9px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.18em;
      color: var(--text-faint);
    }

    .sh-input {
      width: 100%;
      min-width: 0;
      background: transparent;
      border: none;
      border-bottom: 1px solid var(--line-strong);
      color: var(--text);
      padding: 7px 0;
      font-size: 12px;
      font-weight: 400;
      outline: none;
      transition: border-color 200ms ease;
    }

    .sh-input::placeholder { color: var(--text-faint); }
    .sh-input:focus { border-bottom-color: var(--aurora-border); }
    .sh-input[readonly] {
      cursor: not-allowed;
      opacity: 0.75;
    }

    .sh-btn-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      background: transparent;
      border: 1px solid var(--line-strong);
      border-radius: 3px;
      color: var(--text-faint);
      cursor: pointer;
      flex-shrink: 0;
      transition: border-color 180ms ease, color 180ms ease, background 180ms ease;
    }

    .sh-btn-icon:hover:not(:disabled) {
      border-color: var(--aurora-border);
      color: var(--aurora-dim);
      background: var(--aurora-ghost);
    }

    .sh-btn-icon:disabled { opacity: 0.35; cursor: not-allowed; }
    .sh-btn-icon svg { width: 13px; height: 13px; }

    .sh-plan-row {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .sh-plan-chip {
      font-size: 9px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.12em;
      color: var(--aurora-dim);
      border: 1px solid var(--aurora-border);
      padding: 2px 7px;
      border-radius: 2px;
    }

    .sh-plan-source {
      font-size: 10px;
      font-weight: 400;
      color: var(--text-faint);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    /* ─── Navegación ─── */
    .sh-nav {
      display: flex;
      flex-direction: column;
      gap: 20px;
      flex: 1;
    }

    .sh-nav-group { display: grid; gap: 2px; }

    .sh-nav-group__title {
      font-size: 9px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.2em;
      color: var(--text-faint);
      padding: 0 10px;
      margin-bottom: 4px;
    }

    .sh-link {
      display: flex;
      flex-direction: column;
      gap: 1px;
      padding: 9px 10px;
      border-radius: 5px;
      text-decoration: none;
      color: var(--text-muted);
      border: 1px solid transparent;
      transition: background var(--dur-fast) ease,
                  border-color var(--dur-fast) ease,
                  color var(--dur-fast) ease;
    }

    .sh-link:hover {
      background: var(--bg-hover);
      border-color: var(--line);
      color: var(--text);
    }

    .sh-link--active {
      background: var(--aurora-ghost) !important;
      border-color: var(--aurora-border) !important;
      color: var(--aurora) !important;
    }

    .sh-link--active .sh-link__hint { color: var(--aurora-border) !important; }

    .sh-link__label {
      font-size: 13px;
      font-weight: 500;
      letter-spacing: -0.01em;
    }

    .sh-link__hint {
      font-size: 10.5px;
      font-weight: 400;
      color: var(--text-faint);
      transition: color var(--dur-fast) ease;
    }

    /* ─── Usuario ─── */
    .sh-user {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding-top: 4px;
    }

    .sh-user__info {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }

    .sh-user__avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: var(--aurora-ghost);
      border: 1px solid var(--aurora-border);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 11px;
      font-weight: 700;
      color: var(--aurora);
      flex-shrink: 0;
      text-transform: uppercase;
    }

    .sh-user__meta { display: grid; gap: 1px; min-width: 0; }

    .sh-user__meta strong {
      font-size: 12px;
      font-weight: 600;
      color: var(--text);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .sh-user__meta span {
      font-size: 10px;
      font-weight: 400;
      color: var(--text-faint);
    }

    .sh-logout {
      display: flex;
      align-items: center;
      gap: 5px;
      background: none;
      border: 1px solid var(--line-strong);
      border-radius: 3px;
      color: var(--text-faint);
      font-size: 10.5px;
      font-weight: 500;
      letter-spacing: 0.04em;
      padding: 5px 9px;
      cursor: pointer;
      flex-shrink: 0;
      transition: border-color 160ms ease, color 160ms ease, background 160ms ease;
    }

    .sh-logout:hover {
      border-color: var(--danger-border);
      color: var(--danger);
      background: var(--danger-bg);
    }

    .sh-logout svg { width: 13px; height: 13px; }

    /* ════════════════════════
       CONTENIDO PRINCIPAL
    ════════════════════════ */
    .sh-main {
      display: flex;
      flex-direction: column;
      gap: 0;
      min-width: 0;
      height: 100vh;
      overflow: hidden;
      background: var(--bg);
      transition: background-color var(--dur-normal) ease;
    }

    /* ─── Header del módulo ─── */
    .sh-module-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 24px;
      padding: 24px 32px 20px;
      border-bottom: 1px solid var(--line);
      background: var(--bg-panel);
      transition: background-color var(--dur-normal) ease, border-color var(--dur-normal) ease;
    }

    .sh-module-header__left { display: grid; gap: 5px; }

    .sh-module-header__title {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: clamp(22px, 2.2vw, 30px);
      font-weight: 400;
      color: var(--text-strong);
      letter-spacing: -0.02em;
      margin: 0;
      line-height: 1.15;
    }

    .sh-module-header__desc {
      font-size: 13px;
      font-weight: 400;
      color: var(--text-muted);
      line-height: 1.6;
      max-width: 560px;
      margin: 0;
    }

    .sh-module-header__right {
      display: flex;
      gap: 6px;
      flex-shrink: 0;
      align-items: center;
      flex-wrap: wrap;
    }

    /* ─── Plan tag ─── */
    .sh-plan-tag {
      font-size: 10px;
      font-weight: 600;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      padding: 4px 10px;
      border-radius: 20px;
      background: var(--aurora-ghost);
      border: 1px solid var(--aurora-border);
      color: var(--aurora);
      white-space: nowrap;
      transition: all var(--dur-fast) ease;
    }

    .sh-plan-tag[data-src="cache"] { background: var(--warn-bg); border-color: var(--warn-border); color: var(--warn); }
    .sh-plan-tag[data-src="safe"]  { background: var(--surface-strong); border-color: var(--line); color: var(--text-faint); }

    /* ─── Header buttons (all 36×36, icon-only) ─── */
    .sh-hbtn {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 8px;
      color: var(--text-muted);
      cursor: pointer;
      flex-shrink: 0;
      text-decoration: none;
      position: relative;
      transition: background var(--dur-fast) ease,
                  border-color var(--dur-fast) ease,
                  color var(--dur-fast) ease,
                  box-shadow var(--dur-fast) ease;
    }

    .sh-hbtn:hover {
      background: var(--aurora-ghost);
      border-color: var(--aurora-border);
      color: var(--aurora);
      box-shadow: var(--shadow-sm);
    }

    .sh-hbtn svg { width: 16px; height: 16px; }

    /* Bell animation when has notifications */
    .sh-hbtn--notif svg {
      color: var(--aurora);
      animation: bellRing 2.5s ease-in-out infinite;
    }

    @keyframes bellRing {
      0%, 100% { transform: rotate(0deg); }
      10%        { transform: rotate(12deg); }
      20%        { transform: rotate(-10deg); }
      30%        { transform: rotate(8deg); }
      40%        { transform: rotate(-6deg); }
      50%        { transform: rotate(0deg); }
    }

    /* Notification badge */
    .sh-notif-badge {
      position: absolute;
      top: -4px;
      right: -4px;
      min-width: 16px;
      height: 16px;
      padding: 0 3px;
      background: var(--danger);
      color: #fff;
      border-radius: 8px;
      font-size: 9px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 2px solid var(--bg-panel);
      animation: scaleIn var(--dur-fast) var(--ease-out) both;
    }

    /* Avatar button */
    .sh-hbtn--avatar {
      font-size: 13px;
      font-weight: 700;
      color: var(--aurora);
      background: var(--aurora-ghost);
      border-color: var(--aurora-border);
      text-transform: uppercase;
      border-radius: 50%;
    }

    .sh-hbtn--avatar:hover {
      background: var(--aurora);
      color: var(--bg);
      border-color: var(--aurora);
    }

    /* Logout button */
    .sh-hbtn--danger:hover {
      background: var(--danger-bg);
      border-color: var(--danger-border);
      color: var(--danger);
      box-shadow: none;
    }

    /* ─── Outlet ─── */
    .sh-outlet {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
      overflow-x: hidden;
      padding: 0;
      background: var(--bg);
    }
    /* Transición de ruta: cada componente activado entra con un fade+slide sutil. */
    .sh-outlet > *:not(router-outlet) {
      animation: sh-route-enter 220ms ease-out both;
    }
    @keyframes sh-route-enter {
      from { opacity: 0; transform: translateY(6px); }
      to   { opacity: 1; transform: translateY(0); }
    }
    @media (prefers-reduced-motion: reduce) {
      .sh-outlet > *:not(router-outlet) { animation: none; }
    }

    /* ─── Responsive ─── */
    @media (max-width: 1024px) {
      .sh { grid-template-columns: 1fr; }

      .sh-sidebar {
        position: static;
        height: auto;
        overflow: visible;
        border-right: none;
        border-bottom: 1px solid var(--line);
        flex-direction: row;
        flex-wrap: wrap;
        padding: 16px;
        gap: 12px;
      }

      .sh-nav { flex-direction: row; flex-wrap: wrap; }
      .sh-nav-group { min-width: 140px; }
      .sh-module-header { flex-direction: column; padding: 20px; }
      .sh-module-header__right { flex-direction: row; flex-wrap: wrap; }
    }

    @media (max-width: 600px) {
      .sh-module-header { padding: 16px; }
      .sh-outlet { padding: 0; }
    }

    /* Dashboard shell refresh */
    .sh {
      grid-template-columns: 286px minmax(0, 1fr);
      gap: 18px;
      padding: 18px;
      background:
        radial-gradient(circle at top left, var(--aurora-ghost), transparent 38%),
        radial-gradient(circle at bottom right, var(--surface-strong), transparent 34%),
        var(--bg);
    }

    .sh-sidebar,
    .sh-main {
      border: 1px solid var(--line);
      border-radius: 30px;
      box-shadow: var(--shadow-md);
      backdrop-filter: blur(18px);
      -webkit-backdrop-filter: blur(18px);
    }

    .sh-sidebar {
      height: calc(100vh - 36px);
      padding: 26px 22px;
      border-right: 1px solid var(--line);
      background:
        linear-gradient(180deg, var(--bg-panel) 0%, var(--bg-raised) 100%);
    }

    .sh-main {
      position: relative;
      height: calc(100vh - 36px);
      background:
        linear-gradient(180deg, var(--bg-panel) 0%, rgba(255, 255, 255, 0) 30%),
        var(--bg-panel);
      overflow: hidden;
      transition:
        transform var(--dur-normal) var(--ease-out),
        box-shadow var(--dur-normal) var(--ease-out),
        border-color var(--dur-normal) var(--ease-in-out),
        background-color var(--dur-normal) var(--ease-in-out);
    }

    .sh-main--route-busy {
      transform: translateY(2px);
      box-shadow: var(--shadow-lg);
    }

    .sh-route-progress {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(90deg, var(--aurora), var(--aurora-light), var(--aurora));
      transform: scaleX(0);
      transform-origin: left center;
      opacity: 0;
      z-index: 4;
      transition:
        opacity var(--dur-fast) ease,
        transform 900ms var(--ease-out);
    }

    .sh-route-progress--active {
      opacity: 1;
      transform: scaleX(1);
    }

    .sh-module-header {
      padding: 26px 30px 22px;
      background:
        linear-gradient(180deg, var(--surface-muted) 0%, transparent 100%),
        transparent;
      border-bottom-color: var(--line-subtle);
      transition:
        opacity var(--dur-normal) var(--ease-out),
        transform var(--dur-normal) var(--ease-out),
        background-color var(--dur-normal) var(--ease-in-out),
        border-color var(--dur-normal) var(--ease-in-out);
    }

    .sh-module-header--transitioning {
      opacity: 0.88;
      transform: translateY(-4px);
    }

    .sh-module-header__title {
      font-size: clamp(24px, 2.4vw, 34px);
      letter-spacing: -0.04em;
    }

    .sh-module-header__desc {
      max-width: 620px;
      font-size: 13.5px;
      color: var(--text-muted);
    }

    .sh-plan-tag {
      min-height: 38px;
      padding: 0 14px;
      display: inline-flex;
      align-items: center;
      border-radius: 999px;
      background: var(--surface);
      color: var(--aurora);
      border-color: var(--aurora-border);
    }

    .sh-hbtn {
      width: 40px;
      height: 40px;
      border-radius: 14px;
      background: var(--surface-muted);
      border-color: var(--line-subtle);
      color: var(--text);
    }

    .sh-hbtn:hover {
      transform: translateY(-1px);
    }

    .sh-hbtn--avatar {
      width: 42px;
      height: 42px;
    }

    .sh-link {
      border-radius: 18px;
      padding: 12px 12px;
      background: transparent;
    }

    .sh-link:hover {
      background: var(--surface-muted);
      border-color: var(--line-subtle);
    }

    .sh-link--active {
      box-shadow: inset 0 0 0 1px var(--aurora-border);
      background: linear-gradient(135deg, var(--aurora-ghost), transparent 82%) !important;
      color: var(--text-strong) !important;
    }

    .sh-user__avatar {
      width: 38px;
      height: 38px;
    }

    .sh-outlet {
      position: relative;
      padding: 0 8px 8px;
      background: transparent;
      transition:
        opacity var(--dur-normal) var(--ease-out),
        transform var(--dur-normal) var(--ease-out);
    }

    .sh-outlet--transitioning {
      opacity: 0.72;
      transform: translateY(8px);
    }

    .sh-outlet > *:not(router-outlet) {
      animation: sh-route-enter 280ms var(--ease-out) both;
    }

    @media (max-width: 1024px) {
      .sh {
        grid-template-columns: 1fr;
        height: auto;
        min-height: 100vh;
      }

      .sh-mobile-backdrop {
        display: block;
        position: fixed;
        inset: 0;
        z-index: 12;
        background: rgba(7, 9, 14, 0.52);
        backdrop-filter: blur(10px);
        -webkit-backdrop-filter: blur(10px);
        opacity: 0;
        pointer-events: none;
        transition: opacity var(--dur-normal) var(--ease-out);
      }

      .sh-mobile-backdrop--open {
        opacity: 1;
        pointer-events: auto;
      }

      .sh-sidebar,
      .sh-main {
        min-height: 0;
        border-radius: 24px;
      }

      .sh-sidebar {
        position: fixed;
        top: 18px;
        left: 18px;
        bottom: 18px;
        width: min(320px, calc(100vw - 36px));
        height: auto;
        z-index: 13;
        border-right: 1px solid var(--line);
        border-bottom: none;
        padding: 20px 18px;
        flex-direction: column;
        flex-wrap: nowrap;
        transform: translateX(calc(-100% - 28px));
        opacity: 0;
        pointer-events: none;
        box-shadow: none;
      }

      .sh-sidebar--open {
        transform: translateX(0);
        opacity: 1;
        pointer-events: auto;
        box-shadow: var(--shadow-lg);
      }

      .sh-sidebar-close,
      .sh-hbtn--menu {
        display: inline-flex;
      }

      .sh-sidebar-close {
        width: 40px;
        height: 40px;
        align-items: center;
        justify-content: center;
        border: 1px solid var(--line-subtle);
        border-radius: 14px;
        background: var(--surface-muted);
        color: var(--text);
        cursor: pointer;
      }

      .sh-sidebar-close svg {
        width: 16px;
        height: 16px;
      }

      .sh-brand {
        grid-template-columns: auto 1fr auto;
        gap: 10px;
        align-items: start;
      }

      .sh-brand__copy {
        min-width: 0;
      }

      .sh-nav {
        flex-direction: column;
        flex-wrap: nowrap;
      }

      .sh-nav-group {
        min-width: 0;
      }

      .sh-main {
        height: auto;
        min-height: calc(100vh - 36px);
      }

      .sh-module-header {
        padding: 22px 22px 18px;
        flex-direction: column;
        align-items: stretch;
        gap: 16px;
      }

      .sh-module-header__right {
        width: 100%;
        justify-content: flex-end;
      }

      .sh-hbtn--menu {
        margin-right: auto;
      }
    }

    @media (max-width: 600px) {
      .sh {
        gap: 12px;
        padding: 12px;
      }

      .sh-sidebar,
      .sh-main {
        border-radius: 20px;
      }

      .sh-sidebar {
        top: 12px;
        left: 12px;
        bottom: 12px;
        width: min(320px, calc(100vw - 24px));
      }

      .sh-module-header {
        padding: 18px 18px 16px;
      }
    }
  `,
})
export class Shell implements OnInit, OnDestroy {
  protected readonly authSession    = inject(AuthSession);
  protected readonly sessionContext = inject(SessionContext);
  protected readonly themeService   = inject(ThemeService);
  private   readonly erpApi         = inject(ErpApi);
  private   readonly router         = inject(Router);
  private   readonly destroyRef     = inject(DestroyRef);
  protected tenantDraft = this.sessionContext.tenantCode();

  /** Unread notification count — refreshes every 60s */
  readonly notifCount = signal(0);
  readonly routeTransitioning = signal(false);
  readonly mobileNavOpen = signal(false);

  private notifTimer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    void this.sessionContext.ensureTenantPlanLoaded(true).then(() => {
      this.tenantDraft = this.sessionContext.tenantCode();
    });

    this.router.events
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => {
        if (event instanceof NavigationStart) {
          this.mobileNavOpen.set(false);
          this.routeTransitioning.set(true);
          return;
        }

        if (
          event instanceof NavigationEnd ||
          event instanceof NavigationCancel ||
          event instanceof NavigationError
        ) {
          this.mobileNavOpen.set(false);
          window.setTimeout(() => this.routeTransitioning.set(false), 180);
        }
      });
  }

  ngOnInit(): void {
    this.refreshNotifCount();
    this.notifTimer = setInterval(() => this.refreshNotifCount(), 120_000);
  }

  ngOnDestroy(): void {
    if (this.notifTimer) clearInterval(this.notifTimer);
  }

  private refreshNotifCount(): void {
    if (!this.authSession.isAuthenticated()) return;
    this.erpApi.getDashboardSnapshot().subscribe({
      next: (snap) => {
        const count =
          (snap.alertas?.length ?? 0) +
          (snap.productosBajoStock?.length ?? 0) +
          (snap.pagosTransferenciaPendientes?.length ?? 0) +
          ((snap.documentosSriPendientes ?? 0) > 0 ? 1 : 0);
        this.notifCount.set(count);
      },
      error: () => { /* silent — badge stays at 0 */ },
    });
  }

  protected userInitial(): string {
    const name = this.authSession.user()?.fullName || this.authSession.user()?.email || 'O';
    return name.charAt(0).toUpperCase();
  }

  protected roleLabel(): string {
    const roleMap: Record<string, string> = {
      ADMIN:       'Administrador',
      BUYER:       'Compras',
      CASHIER:     'Cajero',
      CUSTOMER:    'Cliente',
    };
    return roleMap[this.authSession.user()?.role || ''] || 'Operador';
  }

  protected async applyTenant(): Promise<void> {
    if (!this.canSwitchTenant()) {
      this.tenantDraft = this.sessionContext.tenantCode();
      return;
    }
    this.mobileNavOpen.set(false);
    await this.sessionContext.changeTenantCode(this.tenantDraft);
    this.tenantDraft = this.sessionContext.tenantCode();
    const currentPath = this.router.url.split('?')[0];
    const allowedPaths = [...this.sessionContext.navItems().map(i => i.path), '/onboarding'];
    const fallback = this.sessionContext.fallbackRoute();
    if (!allowedPaths.includes(currentPath) && currentPath !== fallback) {
      await this.router.navigateByUrl(fallback);
    }
  }

  protected async logout(): Promise<void> {
    this.mobileNavOpen.set(false);
    this.authSession.logout();
    await this.router.navigateByUrl('/login');
  }

  protected toggleMobileNav(): void {
    this.mobileNavOpen.update((open) => !open);
  }

  protected closeMobileNav(): void {
    this.mobileNavOpen.set(false);
  }

  protected tenantDisplayName(): string {
    return tenantDisplayLabel(this.sessionContext.tenantCode());
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    if (this.mobileNavOpen()) {
      this.closeMobileNav();
    }
  }

  protected navGroups(): NavGroup[] {
    const items = [...this.sessionContext.navItems()];
    if (this.authSession.user()?.role === 'ADMIN' || this.authSession.user()?.platformAdmin === true) {
      items.push({ path: '/onboarding', label: 'Configuración inicial' });
    }
    const groups: Array<{ title: string; paths: string[] }> = [
      { title: 'Operación',  paths: ['/pos', '/inventario', '/compras', '/clientes'] },
      { title: 'Análisis',   paths: ['/dashboard-ejecutivo', '/reportes', '/insights', '/contabilidad', '/sri', '/vision'] },
      { title: 'Admin',      paths: ['/onboarding', '/admin/negocios'] },
      { title: 'Cuenta',     paths: ['/perfil', '/notificaciones', '/ajustes'] },
    ];
    return groups
      .map(g => ({
        title: g.title,
        items: g.paths.map(p => items.find(i => i.path === p)).filter((i): i is NavigationModule => Boolean(i)),
      }))
      .filter(g => g.items.length > 0);
  }

  protected canSwitchTenant(): boolean {
    return this.authSession.user()?.platformAdmin === true;
  }

  protected sourceLabel(): string {
    const map: Record<string, string> = {
      live:  'En línea',
      cache: 'Respaldo',
      safe:  'Pendiente',
    };
    return map[this.sessionContext.snapshotSource()] || 'Pendiente';
  }

  protected sourceHeadline(): string {
    const map: Record<string, string> = {
      live:  'Conectado',
      cache: 'Respaldo reciente',
      safe:  'Modo seguro',
    };
    return map[this.sessionContext.snapshotSource()] || 'Modo seguro';
  }

  protected sourceBadge(): string {
    const map: Record<string, string> = {
      live:  'En línea',
      cache: 'Respaldo',
      safe:  'Pendiente',
    };
    return map[this.sessionContext.snapshotSource()] || 'Pendiente';
  }

  protected planLabel(): string {
    const map: Record<string, string> = {
      VISION_AI: 'Inteligencia comercial',
      PRO:       'Pro',
      START:     'Esencial',
    };
    return map[this.sessionContext.snapshot()?.commercialPlan || ''] || 'Por validar';
  }

  protected friendlyModuleLabel(module: string): string {
    const labels: Record<string, string> = {
      pos:                 'Ventas',
      inventory:           'Inventario',
      customers:           'Clientes',
      'basic-reports':     'Reportes',
      'executive-dashboard': 'Dashboard',
      purchases:           'Compras',
      insights:            'Insights',
      accounting:          'Finanzas',
      sri:                 'Facturación',
      'vision-ai':         'Visión AI',
    };
    return labels[module] || module;
  }

  protected fallbackLabel(): string {
    return this.sessionContext.navItems()
      .find(i => i.path === this.sessionContext.fallbackRoute())?.label || 'Perfil';
  }

  protected currentTitle(): string {
    return this.currentNavItem()?.label || 'ERP Comercial';
  }

  protected currentSection(): string {
    const path = this.currentNavItem()?.path || '';
    if (path.startsWith('/admin') || path === '/onboarding') return 'Administración';
    if (['/perfil', '/notificaciones', '/ajustes'].includes(path)) return 'Cuenta';
    if (['/reportes', '/insights', '/contabilidad', '/sri', '/vision', '/dashboard-ejecutivo'].includes(path)) return 'Análisis y control';
    return 'Operación diaria';
  }

  protected currentDescription(): string {
    const descriptions: Record<string, string> = {
      '/pos':                 'Registra ventas, gestiona el carrito y cobra en segundos.',
      '/inventario':          'Observa stock disponible, movimientos y alertas de reposición.',
      '/clientes':            'Historial, cartera y oportunidades de recompra por cliente.',
      '/reportes':            'Indicadores clave del negocio con comparativos y filtros útiles.',
      '/dashboard-ejecutivo': 'Vista ejecutiva con los KPIs más importantes del día.',
      '/compras':             'Órdenes a proveedores, recepción de mercadería y pagos.',
      '/insights':            'Detecta patrones, riesgos y oportunidades automáticamente.',
      '/contabilidad':        'Ingresos, gastos, cartera y obligaciones en lenguaje simple.',
      '/sri':                 'Emisión, seguimiento y estado de documentos electrónicos.',
      '/vision':              'Afluencia de personas, horas pico y conversión comercial.',
      '/admin/negocios':      'Activa planes y módulos por negocio sin cambiar código.',
      '/onboarding':          'Guía paso a paso para dejar listo el negocio: RUC, firma electrónica, ventas, cuentas y comprobantes.',
      '/perfil':              'Tus datos de acceso y preferencias del sistema.',
      '/notificaciones':      'Alertas del negocio que necesitan tu atención.',
      '/ajustes':             'Configuración comercial y operativa del negocio.',
    };
    return descriptions[this.currentNavItem()?.path || ''] || 'Todo el negocio conectado en un solo panel.';
  }

  protected navHint(path: string): string {
    const hints: Record<string, string> = {
      '/pos':                 'Venta rápida',
      '/inventario':          'Stock y movimientos',
      '/compras':             'Proveedores',
      '/clientes':            'CRM',
      '/dashboard-ejecutivo': 'KPIs del día',
      '/reportes':            'Comparativos',
      '/insights':            'Oportunidades',
      '/contabilidad':        'Resultados',
      '/sri':                 'Facturación electrónica',
      '/vision':              'Afluencia',
      '/admin/negocios':      'Negocios',
      '/onboarding':          'Configuración Ecuador',
      '/perfil':              'Acceso',
      '/notificaciones':      'Pendientes',
      '/ajustes':             'Preferencias',
    };
    return hints[path] || '';
  }

  private currentNavItem(): NavigationModule | undefined {
    const currentPath = this.router.url.split('?')[0];
    if (currentPath === '/onboarding') {
      return { path: '/onboarding', label: 'Configuración inicial' };
    }
    return this.sessionContext.navItems().find(i => i.path === currentPath);
  }
}
