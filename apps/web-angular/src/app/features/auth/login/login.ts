import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiFeedback } from '../../../core/models/ui-feedback';
import { AuthSession } from '../../../core/services/auth-session';
import { HttpFeedback } from '../../../core/services/http-feedback';
import { SessionContext } from '../../../core/services/session-context';
import { ThemeService } from '../../../core/services/theme';
import { Brand } from '../../shared/components/brand';
import { RequestFeedback } from '../../shared/components/request-feedback';

type TypingPhase = {
  text: string;
  typeSpeed: number;
  deleteSpeed: number;
  pause: number;
  final?: boolean;
};

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, Brand, RequestFeedback],
  template: `
    <main class="portal">
      <button
        type="button"
        class="theme-button"
        (click)="themeService.toggle()"
        [attr.aria-label]="themeService.isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
      >
        @if (themeService.isDark) {
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v2.2m6.36.44-1.55 1.55M21 12h-2.2m-.44 6.36-1.55-1.55M12 18.8V21m-4.81-4.19-1.55 1.55M5.2 12H3m4.19-4.81L5.64 5.64M15.6 12a3.6 3.6 0 1 1-7.2 0 3.6 3.6 0 0 1 7.2 0z" />
          </svg>
        } @else {
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 14.8A8.4 8.4 0 0 1 9.2 3a8.8 8.8 0 1 0 11.8 11.8z" />
          </svg>
        }
      </button>

      <section class="hero" aria-label="Portal InsightVision IA">
        <div class="ambient" aria-hidden="true">
          <span class="mesh mesh-a"></span>
          <span class="mesh mesh-b"></span>
          <span class="grid"></span>
        </div>

        <div class="hero-content">
          <span class="location-pill">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 20s6-5.33 6-10a6 6 0 1 0-12 0c0 4.67 6 10 6 10z" />
              <circle cx="12" cy="10" r="2.35" />
            </svg>
            Cuenca, Ecuador
          </span>

          <h1 aria-live="polite">
            @for (line of titleLines(); track $index) {
              <span class="title-line">
                <span class="title-text">{{ line }}</span>
                @if (caretLineIndex() === $index) {
                  <i aria-hidden="true"></i>
                }
              </span>
            }
          </h1>

          <p>
            Gestión empresarial unificada, análisis de datos avanzado y control total desde un dashboard ultra optimizado.
          </p>

          <div class="chips" aria-label="Módulos principales">
            @for (tag of moduleTags; track tag) {
              <span>{{ tag }}</span>
            }
          </div>

          <div class="metrics">
            @for (metric of metrics; track metric.title) {
              <article>
                <span class="metric-icon" aria-hidden="true">
                  @switch (metric.icon) {
                    @case ('shield') {
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 3l7 4v5c0 5-3.5 8-7 9-3.5-1-7-4-7-9V7l7-4z" />
                        <path stroke-linecap="round" stroke-linejoin="round" d="m9.5 12 1.7 1.7 3.3-3.4" />
                      </svg>
                    }
                    @case ('receipt') {
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M7 3h10v18l-2-1.5L12 21l-3-1.5L7 21V3z" />
                        <path stroke-linecap="round" stroke-linejoin="round" d="M9.5 8h5M9.5 12h5M9.5 16h3.5" />
                      </svg>
                    }
                    @default {
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                        <rect x="3.5" y="4" width="7" height="7" rx="1.5" />
                        <rect x="13.5" y="4" width="7" height="7" rx="1.5" />
                        <rect x="3.5" y="14" width="7" height="7" rx="1.5" />
                        <rect x="13.5" y="14" width="7" height="7" rx="1.5" />
                      </svg>
                    }
                  }
                </span>
                <div>
                  <small>{{ metric.title }}</small>
                  <strong>{{ metric.value }}</strong>
                </div>
              </article>
            }
          </div>
        </div>
      </section>

      <section class="login-area" aria-label="Acceso al ERP">
        <div class="login-card">
          <div class="login-brand"><app-brand size="large" /></div>

          <header>
            <h2>Acceso seguro</h2>
            <p>Ingresa tus credenciales de acceso</p>
          </header>

          @if (feedback()) {
            <app-request-feedback
              [tone]="feedback()!.tone"
              [message]="feedback()!.message"
              [traceId]="feedback()!.traceId"
            />
          }

          <form (ngSubmit)="login()" novalidate autocomplete="off">
            <label>
              <span>Empresa / dominio</span>
              <div class="field">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 3l7.5 4.3v8.4L12 20l-7.5-4.3V7.3L12 3z" />
                </svg>
                <input
                  [(ngModel)]="tenantCode"
                  name="tenantWorkspace"
                  placeholder="ej. corporacion"
                  autocomplete="off"
                  autocapitalize="none"
                  autocorrect="off"
                  spellcheck="false"
                  maxlength="64"
                />
              </div>
            </label>

            <label>
              <span>Correo electrónico</span>
              <div class="field">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16v12H4z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="m4 7 8 6 8-6" />
                </svg>
                <input [(ngModel)]="email" name="email" type="email" placeholder="usuario@empresa.com" autocomplete="username" />
              </div>
            </label>

            <label>
              <span>Contraseña</span>
              <div class="field field-password">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M7 10V8a5 5 0 0 1 10 0v2" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 10h12v10H6z" />
                </svg>
                <input [(ngModel)]="password" name="password" [type]="showPassword() ? 'text' : 'password'" placeholder="********" autocomplete="current-password" />
                <button type="button" class="eye" (click)="togglePassword()" [attr.aria-label]="showPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M2.04 12.32a1 1 0 0 1 0-.64C3.42 7.51 7.36 4.5 12 4.5s8.57 3.01 9.96 7.18c.07.21.07.43 0 .64C20.58 16.49 16.64 19.5 12 19.5s-8.57-3.01-9.96-7.18z" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z" />
                  </svg>
                </button>
              </div>
            </label>

            <div class="meta">
              <button type="button" class="remember" (click)="toggleRemember()" [attr.aria-pressed]="rememberMe()">
                <span [class.checked]="rememberMe()">
                  @if (rememberMe()) {
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  }
                </span>
                Recordarme
              </button>
              <a routerLink="/forgot-password">¿Olvidaste tu clave?</a>
            </div>

            <div class="commercial-links" aria-label="Acceso comercial">
              <a routerLink="/demo">Solicitar demo</a>
              <a routerLink="/contacto">Contactar ventas</a>
            </div>

            <button type="submit" class="submit" [disabled]="submitting()">
              @if (submitting()) {
                <svg class="spinner" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2.5" opacity="0.22" />
                  <path fill="currentColor" d="M4 12a8 8 0 0 1 8-8V0C5.37 0 0 5.37 0 12h4z" opacity="0.82" />
                </svg>
                Verificando acceso...
              } @else {
                Iniciar sesión
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4 12h15m0 0-5-5m5 5-5 5" />
                </svg>
              }
            </button>
          </form>

          @if (googleLoginEnabled || microsoftLoginEnabled) {
          <div class="divider"><span></span><small>O accede con</small><span></span></div>

          <div class="providers">
            <button type="button" [hidden]="!googleLoginEnabled" [disabled]="submitting()" (click)="loginWithGoogle()">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path fill="#4285F4" d="M23.49 12.27c0-.85-.08-1.67-.22-2.46H12v4.65h6.44a5.51 5.51 0 0 1-2.39 3.62v3.01h3.87c2.26-2.08 3.57-5.15 3.57-8.82z" />
                <path fill="#34A853" d="M12 24c3.24 0 5.95-1.07 7.93-2.91l-3.87-3.01c-1.07.72-2.44 1.14-4.06 1.14-3.13 0-5.78-2.11-6.73-4.95H1.29v3.11A11.99 11.99 0 0 0 12 24z" />
                <path fill="#FBBC05" d="M5.27 14.27A7.21 7.21 0 0 1 4.89 12c0-.79.14-1.56.38-2.27V6.62H1.29A11.99 11.99 0 0 0 0 12c0 1.93.46 3.76 1.29 5.38l3.98-3.11z" />
                <path fill="#EA4335" d="M12 4.78c1.76 0 3.34.61 4.59 1.8l3.43-3.43C17.95 1.22 15.24 0 12 0A11.99 11.99 0 0 0 1.29 6.62l3.98 3.11C6.22 6.89 8.87 4.78 12 4.78z" />
              </svg>
              Google
            </button>
            <button type="button" [hidden]="!microsoftLoginEnabled" [disabled]="submitting()" (click)="loginWithMicrosoft()">
              <svg viewBox="0 0 23 23" aria-hidden="true">
                <path fill="#f25022" d="M1 1h10v10H1z" />
                <path fill="#7fba00" d="M12 1h10v10H12z" />
                <path fill="#00a4ef" d="M1 12h10v10H1z" />
                <path fill="#ffb900" d="M12 12h10v10H12z" />
              </svg>
              Microsoft
            </button>
          </div>
          }
        </div>
      </section>
    </main>
  `,
  styles: `
    :host { display: contents; }

    @keyframes enter { from { opacity: 0; transform: translateY(18px); filter: blur(12px); } to { opacity: 1; transform: none; filter: none; } }
    @keyframes cardEnter { from { opacity: 0; transform: translateX(30px) scale(.985); filter: blur(12px); } to { opacity: 1; transform: none; filter: none; } }
    @keyframes float { 0%,100% { transform: translate3d(0,0,0); } 50% { transform: translate3d(0,-10px,0); } }
    @keyframes blink { 0%,48% { opacity: 1; } 49%,100% { opacity: 0; } }
    @keyframes spin { to { transform: rotate(360deg); } }

    .portal {
      width: 100vw;
      height: 100dvh;
      min-height: 640px;
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(390px, 0.43fr);
      overflow: hidden;
      color: var(--text);
      background: radial-gradient(circle at 14% 16%, color-mix(in srgb, var(--aurora) 20%, transparent), transparent 28%),
        radial-gradient(circle at 48% 92%, color-mix(in srgb, var(--aurora-light) 18%, transparent), transparent 34%),
        linear-gradient(115deg, color-mix(in srgb, var(--bg) 94%, var(--aurora) 6%), var(--bg) 58%, color-mix(in srgb, var(--bg-panel) 82%, var(--aurora) 18%));
    }

    :host-context([data-theme='dark']) .portal {
      background: radial-gradient(circle at 14% 18%, rgba(79,70,229,.24), transparent 30%),
        radial-gradient(circle at 48% 92%, rgba(139,92,246,.18), transparent 34%),
        linear-gradient(110deg, #081428 0%, #07090e 54%, #05070b 100%);
    }

    .theme-button {
      position: absolute;
      top: 22px;
      right: 28px;
      z-index: 8;
      width: 40px;
      height: 40px;
      display: grid;
      place-items: center;
      border: 1px solid color-mix(in srgb, var(--line) 74%, transparent);
      border-radius: 999px;
      color: var(--text-muted);
      background: color-mix(in srgb, var(--bg-panel) 78%, transparent);
      box-shadow: 0 14px 36px rgba(15,23,42,.12);
      backdrop-filter: blur(18px);
    }

    .theme-button svg { width: 18px; height: 18px; }

    .hero {
      position: relative;
      min-width: 0;
      display: grid;
      align-items: center;
      padding: clamp(38px, 5vw, 72px);
      overflow: hidden;
      isolation: isolate;
    }

    .ambient, .grid, .mesh { position: absolute; inset: 0; pointer-events: none; }
    .ambient { z-index: -2; }
    .grid {
      opacity: .13;
      background-image: linear-gradient(var(--line-subtle) 1px, transparent 1px), linear-gradient(90deg, var(--line-subtle) 1px, transparent 1px);
      background-size: 56px 56px;
      mask-image: radial-gradient(circle at 36% 50%, black, transparent 70%);
    }
    .mesh {
      width: 420px;
      height: 420px;
      border-radius: 999px;
      filter: blur(88px);
      opacity: .2;
      animation: float 9s ease-in-out infinite;
    }
    .mesh-a { inset: 4% auto auto 7%; background: var(--aurora); }
    .mesh-b { inset: auto 16% 3% auto; background: var(--aurora-light); animation-delay: -3s; }

    .hero-content {
      position: relative;
      z-index: 2;
      width: min(720px, 100%);
      display: grid;
      gap: 16px;
      animation: enter 680ms var(--ease-out) both;
    }
    .location-pill {
      width: fit-content;
      display: inline-flex;
      align-items: center;
      gap: 9px;
      padding: 8px 14px;
      border: 1px solid color-mix(in srgb, var(--line) 76%, transparent);
      border-radius: 999px;
      color: var(--aurora-light);
      background: color-mix(in srgb, var(--bg-panel) 56%, transparent);
      box-shadow: 0 14px 34px color-mix(in srgb, var(--aurora) 10%, transparent);
      backdrop-filter: blur(18px);
      font-size: 11px;
      font-weight: 900;
      letter-spacing: .12em;
      text-transform: uppercase;
    }
    .location-pill svg {
      width: 15px;
      height: 15px;
      flex: 0 0 auto;
    }
    h1 {
      max-width: 720px;
      display: grid;
      gap: clamp(4px, .35vw, 8px);
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
      font-size: clamp(43px, 5vw, 72px);
      font-weight: 900;
      line-height: 1.06;
      letter-spacing: -.055em;
      margin: 0;
    }
    .title-line {
      min-height: 1.02em;
      display: inline-flex;
      align-items: flex-end;
      gap: 8px;
      width: fit-content;
    }
    .title-text {
      display: inline-block;
      min-height: 1em;
      color: transparent;
      background: linear-gradient(130deg, #60a5fa 0%, var(--aurora) 44%, var(--aurora-light) 100%);
      -webkit-background-clip: text;
      background-clip: text;
      white-space: nowrap;
    }
    h1 i {
      display: inline-block;
      width: 4px;
      height: .9em;
      flex: 0 0 auto;
      transform: translateY(-.02em);
      background: var(--aurora-light);
      box-shadow: 0 0 18px var(--aurora-light);
      animation: blink 860ms steps(2, start) infinite;
    }
    .hero-content p {
      max-width: 620px;
      color: var(--text-muted);
      font-size: clamp(16px, 1.4vw, 19px);
      line-height: 1.65;
      margin: 0;
    }
    .chips { display: flex; flex-wrap: wrap; gap: 9px; }
    .chips span {
      padding: 7px 12px;
      border: 1px solid color-mix(in srgb, var(--line) 70%, transparent);
      border-radius: 999px;
      color: var(--text-muted);
      background: color-mix(in srgb, var(--bg-panel) 62%, transparent);
      font-size: 10px;
      font-weight: 900;
      letter-spacing: .12em;
      text-transform: uppercase;
      backdrop-filter: blur(16px);
    }
    .metrics {
      width: min(680px, 100%);
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 14px;
      margin-top: 4px;
    }
    .metrics article {
      display: grid;
      grid-template-columns: 38px 1fr;
      gap: 12px;
      align-items: center;
      min-width: 0;
      padding: 14px;
      border: 1px solid color-mix(in srgb, var(--line) 72%, transparent);
      border-radius: 16px;
      background: color-mix(in srgb, var(--bg-panel) 58%, transparent);
      box-shadow: 0 20px 54px rgba(15,23,42,.1);
      backdrop-filter: blur(20px);
    }
    .metric-icon {
      width: 38px;
      height: 38px;
      display: grid;
      place-items: center;
      border-radius: 13px;
      color: var(--aurora-light);
      background: var(--aurora-ghost);
    }
    .metric-icon svg {
      width: 18px;
      height: 18px;
    }
    .metrics small { color: var(--text-faint); font-size: 11px; font-weight: 800; }
    .metrics strong { display: block; color: var(--text-strong); font-size: 13px; font-weight: 900; margin-top: 2px; }

    .login-area {
      width: 100%;
      display: grid;
      place-items: center;
      justify-items: center;
      align-items: center;
      padding: clamp(24px, 4vw, 58px);
      background: color-mix(in srgb, var(--bg) 76%, transparent);
    }
    .login-card {
      width: min(100%, 462px);
      display: grid;
      justify-self: center;
      margin-inline: auto;
      padding: clamp(30px, 3.6vw, 44px);
      border: 1px solid color-mix(in srgb, var(--line) 74%, transparent);
      border-radius: 26px;
      background: color-mix(in srgb, var(--bg-panel) 82%, transparent);
      box-shadow: 0 34px 90px rgba(15,23,42,.16), inset 0 1px 0 rgba(255,255,255,.12);
      backdrop-filter: blur(28px);
      animation: cardEnter 720ms var(--ease-out) 120ms both;
    }
    :host-context([data-theme='dark']) .login-card { background: rgba(9,13,22,.82); box-shadow: 0 38px 110px rgba(0,0,0,.46), inset 0 1px 0 rgba(255,255,255,.06); }
    .login-brand {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 96px;
      margin-bottom: 18px;
      padding: 4px 0 10px;
      filter: drop-shadow(0 6px 20px rgba(139, 92, 246, 0.24));
    }
    .login-brand app-brand {
      width: min(100%, 320px);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-inline: auto;
    }
    .login-card header { display: grid; gap: 8px; text-align: center; margin-bottom: 26px; }
    .login-card h2 { color: var(--text-strong); font-size: 24px; font-weight: 900; letter-spacing: -.04em; }
    .login-card header p { color: var(--text-muted); font-size: 13px; font-weight: 700; }
    form { display: grid; gap: 15px; }
    label { display: grid; gap: 8px; }
    label > span { color: var(--text-faint); font-size: 10px; font-weight: 900; letter-spacing: .14em; text-transform: uppercase; }
    .field {
      min-height: 50px;
      display: grid;
      grid-template-columns: 18px 1fr;
      align-items: center;
      gap: 11px;
      padding: 0 14px;
      border: 1px solid color-mix(in srgb, var(--line) 82%, transparent);
      border-radius: 13px;
      color: var(--text-faint);
      background: color-mix(in srgb, var(--bg-raised) 68%, transparent);
      transition: transform 150ms ease, border-color 150ms ease, box-shadow 150ms ease;
    }
    .field:focus-within { transform: translateY(-1px); border-color: var(--aurora-border); box-shadow: 0 0 0 4px var(--aurora-ghost); color: var(--aurora-light); }
    .field svg { width: 17px; height: 17px; }
    .field input {
      min-width: 0;
      width: 100%;
      border: 0;
      outline: 0;
      padding: 13px 0;
      color: var(--text-strong);
      background: transparent;
      font-size: 14px;
      font-weight: 800;
    }
    .field input::placeholder { color: var(--text-faint); }
    .field input:-webkit-autofill,
    .field input:-webkit-autofill:hover,
    .field input:-webkit-autofill:focus {
      -webkit-text-fill-color: var(--text-strong);
      -webkit-box-shadow: 0 0 0 1000px color-mix(in srgb, var(--bg-raised) 68%, transparent) inset;
      box-shadow: 0 0 0 1000px color-mix(in srgb, var(--bg-raised) 68%, transparent) inset;
      caret-color: var(--text-strong);
      transition: background-color 9999s ease-in-out 0s;
    }
    .field-password { grid-template-columns: 18px 1fr 28px; }
    .eye { width: 28px; height: 28px; display: grid; place-items: center; border: 0; border-radius: 8px; color: var(--text-faint); background: transparent; }
    .meta { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 1px 0 4px; }
    .remember { display: inline-flex; align-items: center; gap: 8px; padding: 0; border: 0; background: transparent; color: var(--text-muted); font-size: 12px; font-weight: 800; }
    .remember span { width: 16px; height: 16px; display: grid; place-items: center; border: 1px solid var(--line-strong); border-radius: 4px; color: white; }
    .remember span.checked { border-color: var(--aurora); background: var(--aurora); }
    .remember svg { width: 12px; height: 12px; }
    .meta a { color: var(--aurora-light); font-size: 12px; font-weight: 900; white-space: nowrap; }
    .commercial-links {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-top: -2px;
    }
    .commercial-links a {
      color: var(--text-muted);
      font-size: 12px;
      font-weight: 900;
      text-decoration: underline;
      text-underline-offset: 3px;
    }
    .submit {
      min-height: 51px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      border: 0;
      border-radius: 13px;
      color: white;
      background: linear-gradient(135deg, var(--aurora), var(--aurora-light));
      box-shadow: 0 18px 38px color-mix(in srgb, var(--aurora-light) 30%, transparent);
      font-size: 14px;
      font-weight: 900;
    }
    .submit svg, .spinner { width: 17px; height: 17px; }
    .spinner { animation: spin .9s linear infinite; }
    .divider { display: flex; align-items: center; gap: 12px; margin: 20px 0 14px; }
    .divider span { flex: 1; height: 1px; background: var(--line); }
    .divider small { color: var(--text-faint); font-size: 10px; font-weight: 900; letter-spacing: .16em; text-transform: uppercase; white-space: nowrap; }
    .providers { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
    .providers button {
      min-height: 46px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      border: 1px solid color-mix(in srgb, var(--line) 78%, transparent);
      border-radius: 13px;
      color: var(--text);
      background: color-mix(in srgb, var(--bg-raised) 66%, transparent);
      font-size: 13px;
      font-weight: 900;
    }
    .providers svg { width: 19px; height: 19px; display: block; }

    @media (max-width: 1180px) {
      .portal { grid-template-columns: minmax(0, 1fr) minmax(370px, 430px); }
      h1 { font-size: clamp(40px, 4.8vw, 64px); }
      .metrics { grid-template-columns: 1fr; width: min(430px, 100%); }
    }

    @media (max-width: 900px) {
      .portal { height: auto; min-height: 100dvh; grid-template-columns: 1fr; overflow-y: auto; }
      .hero { min-height: 52dvh; padding: 72px 22px 34px; }
      h1 { font-size: clamp(36px, 9vw, 54px); }
      .login-area { padding: 16px 16px 28px; }
      .theme-button { top: 16px; right: 16px; }
    }

    @media (max-width: 520px) {
      .hero { min-height: 48dvh; }
      .hero-content { gap: 15px; }
      h1 { font-size: clamp(31px, 11vw, 44px); }
      .hero-content p { font-size: 14px; line-height: 1.55; }
      .chips { display: none; }
      .login-card { padding: 22px 16px; border-radius: 22px; }
      .login-brand { min-height: 68px; margin-bottom: 16px; padding-bottom: 4px; }
      .login-brand app-brand { width: min(100%, 248px); }
      .providers { grid-template-columns: 1fr; }
      .meta { align-items: flex-start; flex-direction: column; gap: 10px; }
    }

    @media (prefers-reduced-motion: reduce) {
      *, *::before, *::after { animation-duration: .01ms !important; animation-iteration-count: 1 !important; transition-duration: .01ms !important; }
    }
  `,
})
export class Login implements OnInit, OnDestroy {
  private readonly authSession = inject(AuthSession);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly sessionContext = inject(SessionContext);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly themeService = inject(ThemeService);
  protected readonly googleLoginEnabled = this.runtimeFlag('publicEnableGoogleLogin');
  protected readonly microsoftLoginEnabled = this.runtimeFlag('publicEnableMicrosoftLogin');

  protected email = '';
  protected password = '';
  protected tenantCode = this.initialTenantCode();

  protected readonly feedback = signal<UiFeedback | null>(null);
  protected readonly submitting = computed(() => this.authSession.authenticating());
  protected readonly showPassword = signal(false);
  protected readonly rememberMe = signal(false);
  protected readonly typedText = signal('');
  protected readonly activePhaseIndex = signal(0);

  protected readonly moduleTags = ['Ventas · POS', 'Inventario', 'Compras', 'Facturación', 'Reportes'];
  protected readonly metrics = [
    { icon: 'shield', title: 'Control de acceso', value: 'Protegido' },
    { icon: 'receipt', title: 'Facturación SRI', value: 'Integrada' },
    { icon: 'grid', title: 'Módulos ERP', value: 'Conectados' },
  ];
  private readonly finalTitleLayout = [
    'Bienvenido al ERP',
    'de InsightVision',
    'IA - Confianza',
    'y Comodidad',
  ];
  protected readonly titleLines = computed(() => {
    const text = this.typedText();
    if (this.activePhaseIndex() !== this.typingSequence.length - 1) {
      return [text, '', '', ''];
    }
    return this.buildFinalTitleLines(text);
  });
  protected readonly caretLineIndex = computed(() => {
    const lines = this.titleLines();
    for (let index = lines.length - 1; index >= 0; index -= 1) {
      if (lines[index].length > 0) {
        return index;
      }
    }
    return 0;
  });

  private readonly typingSequence: TypingPhase[] = [
    { text: 'Seguridad', typeSpeed: 58, deleteSpeed: 32, pause: 480 },
    { text: 'Ahorro', typeSpeed: 58, deleteSpeed: 32, pause: 480 },
    { text: 'Cobro', typeSpeed: 58, deleteSpeed: 32, pause: 480 },
    { text: 'Ventas', typeSpeed: 58, deleteSpeed: 32, pause: 480 },
    { text: 'Inventario', typeSpeed: 58, deleteSpeed: 32, pause: 480 },
    { text: 'Compras', typeSpeed: 58, deleteSpeed: 32, pause: 480 },
    { text: 'Facturación', typeSpeed: 58, deleteSpeed: 32, pause: 480 },
    { text: 'Reportes', typeSpeed: 58, deleteSpeed: 32, pause: 480 },
    { text: 'Contabilidad', typeSpeed: 58, deleteSpeed: 32, pause: 480 },
    { text: 'Bienvenido al ERP de InsightVision IA - Confianza y Comodidad', typeSpeed: 58, deleteSpeed: 0, pause: Number.POSITIVE_INFINITY, final: true },
  ];

  private typingTimer: number | null = null;
  private typingIndex = 0;
  private typingDeleting = false;

  protected togglePassword(): void {
    this.showPassword.update((value) => !value);
  }

  protected toggleRemember(): void {
    this.rememberMe.update((value) => !value);
  }

  async ngOnInit(): Promise<void> {
    this.startTypewriter();
  }

  ngOnDestroy(): void {
    this.clearTypingTimer();
  }

  protected async login(): Promise<void> {
    const email = this.email.trim();
    const password = this.password.trim();
    const tenantCode = this.normalizeTenantCode(this.tenantCode);

    if (!email || !password) {
      this.feedback.set(this.httpFeedback.warning('Ingresa correo y contraseña para continuar.'));
      return;
    }

    this.feedback.set(null);

    try {
      const user = await this.authSession.login(email, password, tenantCode);
      await this.sessionContext.changeTenantCode(this.normalizeTenantCode(user.tenantCode ?? tenantCode));
      await this.router.navigateByUrl(this.resolveNextRoute());
    } catch (error) {
      if (error instanceof Error && error.message.includes('2FA')) {
        this.feedback.set(this.httpFeedback.warning(error.message));
        return;
      }
      this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo iniciar sesión en el ERP.'));
    }
  }

  protected async loginWithGoogle(): Promise<void> {
    await this.loginWithProvider('google');
  }

  protected async loginWithMicrosoft(): Promise<void> {
    await this.loginWithProvider('microsoft');
  }

  private startTypewriter(): void {
    this.clearTypingTimer();
    this.typedText.set('');
    this.typingIndex = 0;
    this.typingDeleting = false;
    this.activePhaseIndex.set(0);
    this.queueTyping(280);
  }

  private advanceTypewriter(): void {
    this.activePhaseIndex.set(this.typingIndex);
    const phase = this.typingSequence[this.typingIndex];
    const currentText = this.typedText();

    if (!this.typingDeleting) {
      if (currentText.length < phase.text.length) {
        this.typedText.set(phase.text.slice(0, currentText.length + 1));
        this.queueTyping(phase.typeSpeed);
        return;
      }
      if (phase.final) {
        return;
      }
      this.typingDeleting = true;
      this.queueTyping(phase.pause);
      return;
    }

    if (currentText.length > 0) {
      this.typedText.set(currentText.slice(0, -1));
      this.queueTyping(phase.deleteSpeed);
      return;
    }

    this.typingDeleting = false;
    this.typingIndex = Math.min(this.typingIndex + 1, this.typingSequence.length - 1);
    this.activePhaseIndex.set(this.typingIndex);
    this.queueTyping(120);
  }

  private queueTyping(delay: number): void {
    this.typingTimer = window.setTimeout(() => this.advanceTypewriter(), delay);
  }

  private clearTypingTimer(): void {
    if (this.typingTimer !== null) {
      window.clearTimeout(this.typingTimer);
      this.typingTimer = null;
    }
  }

  private buildFinalTitleLines(text: string): string[] {
    let remaining = text;
    return this.finalTitleLayout.map((segment) => {
      const part = remaining.slice(0, Math.min(segment.length, remaining.length));
      remaining = remaining.slice(part.length);
      if (remaining.startsWith(' ')) {
        remaining = remaining.slice(1);
      }
      return part;
    });
  }

  private async loginWithProvider(provider: 'google' | 'microsoft'): Promise<void> {
    const tenantCode = this.normalizeOptionalTenantCode(this.tenantCode);
    this.feedback.set(null);

    try {
      const user = provider === 'google'
        ? await this.authSession.loginWithGoogle(tenantCode)
        : await this.authSession.loginWithMicrosoft(tenantCode);
      await this.sessionContext.changeTenantCode(this.normalizeTenantCode(user.tenantCode ?? tenantCode));
      await this.router.navigateByUrl(this.resolveNextRoute());
    } catch (error) {
      const label = provider === 'google' ? 'Google' : 'Microsoft';
      this.feedback.set(this.providerFeedback(error, label));
    }
  }

  private providerFeedback(error: unknown, label: string): UiFeedback {
    if (error instanceof HttpErrorResponse) {
      return this.httpFeedback.fromError(error, `No se pudo iniciar sesión con ${label}.`);
    }
    const code = this.firebaseErrorCode(error);
    if (code === 'auth/unauthorized-domain') {
      return this.httpFeedback.warning(
        `Firebase bloqueó este dominio. Agrega ${globalThis.location?.hostname ?? 'este dominio'} en Authentication > Settings > Authorized domains.`,
      );
    }
    if (code === 'auth/popup-closed-by-user') {
      return this.httpFeedback.warning(`Cerraste la ventana de ${label} antes de completar el acceso.`);
    }
    if (code === 'auth/account-exists-with-different-credential') {
      return this.httpFeedback.warning('Esta cuenta ya existe con otro proveedor de inicio de sesión.');
    }
    if (error instanceof Error && error.message) {
      return this.httpFeedback.warning(error.message);
    }
    return this.httpFeedback.fromError(error, `No se pudo iniciar sesión con ${label}.`);
  }

  private firebaseErrorCode(error: unknown): string {
    if (typeof error === 'object' && error !== null && 'code' in error) {
      const code = (error as { code?: unknown }).code;
      return typeof code === 'string' ? code : '';
    }
    return '';
  }

  private runtimeFlag(key: 'publicEnableGoogleLogin' | 'publicEnableMicrosoftLogin'): boolean {
    const globalWindow = globalThis as typeof globalThis & { __env?: Record<string, unknown> };
    const value = globalWindow.__env?.[key];
    return value === true || value === 'true';
  }

  private resolveNextRoute(): string {
    const redirect = this.route.snapshot.queryParamMap.get('redirect');
    if (redirect && redirect.startsWith('/') && !redirect.startsWith('//') && !redirect.startsWith('/login')) {
      return redirect;
    }
    return this.sessionContext.fallbackRoute();
  }

  private normalizeTenantCode(code: string): string {
    return (code || '').trim().toLowerCase() || 'default';
  }

  private normalizeOptionalTenantCode(code: string): string {
    return (code || '').trim().toLowerCase();
  }

  private initialTenantCode(): string {
    const storedTenantCode = this.sessionContext.tenantCode();
    return storedTenantCode === 'default' ? '' : storedTenantCode;
  }
}
