import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import type { ConfirmationResult } from 'firebase/auth';
import { UiFeedback } from '../../../core/models/ui-feedback';
import { AuthSession } from '../../../core/services/auth-session';
import { FirebaseAuthClient } from '../../../core/services/firebase-auth-client';
import { HttpFeedback } from '../../../core/services/http-feedback';
import { RequestFeedback } from '../../shared/components/request-feedback';

type ResetStep = 'request' | 'verify' | 'done';

@Component({
  selector: 'app-forgot-password',
  imports: [FormsModule, RouterLink, RequestFeedback],
  template: `
    <div class="fp-wrap">
      <div class="fp-bg" aria-hidden="true">
        <div class="fp-bg__glow"></div>
      </div>

      <div class="fp-inner">
        <div class="fp-brand">
          <span class="fp-dot" aria-hidden="true"></span>
          <span class="fp-eyebrow">ERP Comercial</span>
        </div>

        <div class="fp-head">
          <h2 class="fp-title">Recuperar acceso</h2>
          <p class="fp-subtitle">
            Verifica el teléfono de tu cuenta y cambia tu contraseña con un código SMS.
          </p>
        </div>

        <div class="fp-steps" aria-label="Progreso de recuperación">
          <span class="fp-step" [class.fp-step--active]="step() === 'request'" [class.fp-step--done]="step() !== 'request'">Teléfono</span>
          <span class="fp-step" [class.fp-step--active]="step() === 'verify'" [class.fp-step--done]="step() === 'done'">Código</span>
          <span class="fp-step" [class.fp-step--active]="step() === 'done'">Nueva clave</span>
        </div>

        @if (feedback()) {
          <div class="fp-feedback">
            <app-request-feedback
              [tone]="feedback()!.tone"
              [message]="feedback()!.message"
              [traceId]="feedback()!.traceId"
            />
          </div>
        }

        @if (step() === 'request') {
          <form class="fp-form" (ngSubmit)="sendSms()" novalidate>
            <div class="fp-field">
              <label class="fp-field__label" for="fp-email">Correo electrónico</label>
              <input
                id="fp-email"
                class="fp-field__input"
                type="email"
                [(ngModel)]="email"
                name="email"
                placeholder="nombre@empresa.com"
                autocomplete="email"
                required
              />
            </div>

            <div class="fp-field">
              <label class="fp-field__label" for="fp-phone">Teléfono</label>
              <input
                id="fp-phone"
                class="fp-field__input"
                type="tel"
                [(ngModel)]="phone"
                name="phone"
                placeholder="+593999999999"
                autocomplete="tel"
                required
              />
            </div>

            <button type="submit" class="fp-submit" [disabled]="loading()">
              @if (loading()) {
                <svg class="fp-spinner" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2.5" style="opacity:0.2"/>
                  <path fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" style="opacity:0.75"/>
                </svg>
                Enviando SMS...
              } @else {
                Enviar código SMS
              }
            </button>
          </form>
        }

        @if (step() === 'verify') {
          <form class="fp-form" (ngSubmit)="resetPassword()" novalidate>
            <div class="fp-field">
              <label class="fp-field__label" for="fp-code">Código SMS</label>
              <input
                id="fp-code"
                class="fp-field__input"
                type="text"
                inputmode="numeric"
                [(ngModel)]="code"
                name="code"
                placeholder="123456"
                autocomplete="one-time-code"
                required
              />
            </div>

            <div class="fp-field">
              <label class="fp-field__label" for="fp-password">Nueva contraseña</label>
              <input
                id="fp-password"
                class="fp-field__input"
                type="password"
                [(ngModel)]="newPassword"
                name="newPassword"
                autocomplete="new-password"
                required
              />
            </div>

            <div class="fp-field">
              <label class="fp-field__label" for="fp-confirm">Confirmar contraseña</label>
              <input
                id="fp-confirm"
                class="fp-field__input"
                type="password"
                [(ngModel)]="confirmPassword"
                name="confirmPassword"
                autocomplete="new-password"
                required
              />
            </div>

            <button type="submit" class="fp-submit" [disabled]="loading()">
              @if (loading()) {
                <svg class="fp-spinner" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2.5" style="opacity:0.2"/>
                  <path fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" style="opacity:0.75"/>
                </svg>
                Actualizando...
              } @else {
                Cambiar contraseña
              }
            </button>

            <button type="button" class="fp-secondary" [disabled]="loading()" (click)="sendSms()">
              Reenviar código
            </button>
          </form>
        }

        @if (step() === 'done') {
          <div class="fp-done">
            <strong>Contraseña actualizada</strong>
            <span>Ya puedes iniciar sesión con tu nueva contraseña.</span>
          </div>
        }

        <div id="fp-recaptcha" class="fp-recaptcha" aria-hidden="true"></div>

        <a routerLink="/login" class="fp-back">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
          </svg>
          Volver al inicio de sesión
        </a>

        <p class="fp-footer">© 2025 ERP Comercial · Todos los derechos reservados</p>
      </div>
    </div>
  `,
  styles: `
    :host { display: contents; }

    .fp-wrap {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background:
        linear-gradient(135deg, rgba(35, 102, 65, 0.08), transparent 36%),
        linear-gradient(315deg, rgba(66, 133, 244, 0.08), transparent 32%),
        #f4f7f6;
      padding: clamp(24px, 5vw, 64px);
      position: relative;
      overflow: hidden;
    }

    .fp-bg {
      position: absolute;
      inset: 0;
      pointer-events: none;
    }

    .fp-bg__glow {
      position: absolute;
      bottom: -80px;
      left: -60px;
      width: 500px;
      height: 440px;
      border-radius: 50%;
      background: radial-gradient(circle, #4a7c5e 0%, transparent 70%);
      opacity: 0.18;
      filter: blur(80px);
    }

    .fp-inner {
      position: relative;
      z-index: 1;
      width: 100%;
      max-width: 430px;
      display: grid;
      gap: 0;
      background: rgba(255, 255, 255, 0.86);
      border: 1px solid rgba(23, 32, 27, 0.08);
      border-radius: 8px;
      padding: clamp(28px, 5vw, 42px);
      box-shadow: 0 24px 70px rgba(23, 32, 27, 0.14);
      backdrop-filter: blur(18px);
      animation: fp-panel-in 420ms cubic-bezier(0,0,0.2,1) both;
    }

    .fp-brand {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 34px;
    }

    .fp-dot {
      display: inline-block;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: rgba(74, 124, 94, 0.7);
      flex-shrink: 0;
    }

    .fp-eyebrow {
      font-size: 10px;
      letter-spacing: 0.22em;
      text-transform: uppercase;
      font-weight: 500;
      color: rgba(74, 124, 94, 0.65);
    }

    .fp-head { margin-bottom: 18px; }

    .fp-title {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 28px;
      font-weight: 400;
      color: #17201b;
      letter-spacing: -0.01em;
      margin: 0 0 8px;
    }

    .fp-subtitle {
      font-size: 13px;
      font-weight: 300;
      color: rgba(130, 125, 115, 0.65);
      line-height: 1.65;
      margin: 0;
    }

    .fp-steps {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 8px;
      margin-bottom: 24px;
    }

    .fp-step {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 34px;
      border: 1px solid rgba(23, 32, 27, 0.1);
      border-radius: 8px;
      color: #6b716c;
      background: #f7f9f8;
      font-size: 10px;
      font-weight: 600;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      transition: background 180ms ease, border-color 180ms ease, color 180ms ease, transform 180ms ease;
    }

    .fp-step--active {
      color: #173c29;
      border-color: rgba(35, 102, 65, 0.35);
      background: rgba(35, 102, 65, 0.08);
      transform: translateY(-1px);
    }

    .fp-step--done {
      color: #ffffff;
      border-color: #236641;
      background: #236641;
    }

    .fp-feedback { margin-bottom: 20px; }
    .fp-form { display: grid; gap: 0; }
    .fp-field { display: grid; gap: 8px; margin-bottom: 22px; }

    .fp-field__label {
      font-size: 10px;
      letter-spacing: 0.2em;
      text-transform: uppercase;
      font-weight: 500;
      color: rgba(100, 98, 90, 0.75);
    }

    .fp-field__input {
      width: 100%;
      background: transparent;
      border: none;
      border-bottom: 1px solid rgba(23, 32, 27, 0.18);
      color: #17201b;
      padding: 11px 0;
      font-size: 14px;
      font-weight: 300;
      letter-spacing: 0.015em;
      outline: none;
      transition: border-color 220ms ease;
      box-sizing: border-box;
    }

    .fp-field__input::placeholder { color: rgba(23, 32, 27, 0.35); }
    .fp-field__input:focus { border-bottom-color: rgba(74, 124, 94, 0.6); }
    .fp-field__input:-webkit-autofill {
      -webkit-box-shadow: 0 0 0 40px #ffffff inset !important;
      -webkit-text-fill-color: #17201b !important;
    }

    .fp-submit,
    .fp-secondary {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      padding: 13px 20px;
      background: #236641;
      border: 1px solid #236641;
      color: #ffffff;
      font-size: 11px;
      font-weight: 500;
      letter-spacing: 0.14em;
      text-transform: uppercase;
      border-radius: 8px;
      cursor: pointer;
      transition: background 220ms ease, border-color 220ms ease, color 220ms ease;
      margin-bottom: 14px;
    }

      .fp-secondary {
      background: transparent;
      border-color: rgba(23, 32, 27, 0.18);
      color: #5f6862;
    }

    .fp-submit:hover:not(:disabled),
    .fp-secondary:hover:not(:disabled) {
      background: #1e5937;
      border-color: #1e5937;
      color: #ffffff;
      transform: translateY(-1px);
    }

    .fp-submit:disabled,
    .fp-secondary:disabled {
      opacity: 0.35;
      cursor: not-allowed;
    }

    .fp-spinner {
      width: 14px;
      height: 14px;
      animation: fp-spin 0.9s linear infinite;
      flex-shrink: 0;
    }

    .fp-done {
      display: grid;
      gap: 6px;
      border: 1px solid rgba(74, 124, 94, 0.28);
      background: rgba(74, 124, 94, 0.08);
      color: #17201b;
      border-radius: 8px;
      padding: 18px;
      margin-bottom: 24px;
    }

    .fp-done strong { color: rgba(74, 124, 94, 0.95); font-size: 14px; }
    .fp-done span { color: rgba(130, 125, 115, 0.75); font-size: 12px; line-height: 1.5; }
    .fp-recaptcha { min-height: 1px; }

    @keyframes fp-spin { to { transform: rotate(360deg); } }
    @keyframes fp-panel-in {
      from { opacity: 0; transform: translateY(16px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .fp-back {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 11.5px;
      color: rgba(100, 98, 90, 0.65);
      text-decoration: none;
      letter-spacing: 0.04em;
      transition: color 150ms ease;
      margin: 10px 0 40px;
    }

    .fp-back:hover { color: rgba(74, 124, 94, 0.8); }
    .fp-back svg { width: 12px; height: 12px; }

    .fp-footer {
      text-align: center;
      font-size: 10.5px;
      color: rgba(80, 78, 70, 0.5);
      letter-spacing: 0.1em;
      margin: 0;
    }

    @media (prefers-color-scheme: dark) {
      .fp-wrap {
        background:
          linear-gradient(135deg, rgba(35, 102, 65, 0.16), transparent 38%),
          linear-gradient(315deg, rgba(66, 133, 244, 0.12), transparent 34%),
          #090b0f;
      }

      .fp-inner {
        background: rgba(15, 17, 20, 0.88);
        border-color: rgba(255, 255, 255, 0.08);
        box-shadow: 0 24px 80px rgba(0, 0, 0, 0.38);
      }

      .fp-title,
      .fp-field__input,
      .fp-done {
        color: rgba(242, 240, 233, 0.9);
      }

      .fp-step {
        color: rgba(210, 214, 211, 0.72);
        background: rgba(255, 255, 255, 0.04);
        border-color: rgba(255, 255, 255, 0.08);
      }

      .fp-step--active {
        color: #a9d8b9;
        background: rgba(74, 124, 94, 0.14);
        border-color: rgba(74, 124, 94, 0.45);
      }

      .fp-step--done {
        color: #ffffff;
        background: #236641;
        border-color: #236641;
      }

      .fp-field__input {
        border-bottom-color: rgba(255, 255, 255, 0.16);
      }

      .fp-field__input:-webkit-autofill {
        -webkit-box-shadow: 0 0 0 40px #101114 inset !important;
        -webkit-text-fill-color: rgba(242, 240, 233, 0.9) !important;
      }

      .fp-secondary {
        border-color: rgba(255, 255, 255, 0.12);
        color: rgba(210, 214, 211, 0.72);
      }
    }

    @media (max-width: 420px) {
      .fp-steps {
        grid-template-columns: 1fr;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .fp-inner,
      .fp-step,
      .fp-submit,
      .fp-secondary {
        animation: none;
        transition: none;
      }
    }
  `,
})
export class ForgotPassword {
  private readonly firebaseAuth = inject(FirebaseAuthClient);
  private readonly authSession = inject(AuthSession);
  private readonly httpFeedback = inject(HttpFeedback);

  protected email = '';
  protected phone = '';
  protected code = '';
  protected newPassword = '';
  protected confirmPassword = '';

  protected readonly step = signal<ResetStep>('request');
  protected readonly loading = signal(false);
  protected readonly feedback = signal<UiFeedback | null>(null);

  private confirmationResult: ConfirmationResult | null = null;

  protected async sendSms(): Promise<void> {
    const email = this.email.trim();
    if (!this.isEmail(email)) {
      this.feedback.set(this.httpFeedback.warning('Ingresa un correo electrónico válido.'));
      return;
    }

    let phone: string;
    try {
      phone = this.normalizePhone(this.phone);
    } catch (error) {
      this.handleError(error, 'Ingresa un teléfono válido.');
      return;
    }

    this.loading.set(true);
    this.feedback.set(null);
    try {
      this.phone = phone;
      this.confirmationResult = await this.firebaseAuth.sendPasswordResetSms(phone, 'fp-recaptcha');
      this.step.set('verify');
      this.feedback.set(this.httpFeedback.info('Te enviamos un código SMS al teléfono registrado.'));
    } catch (error) {
      this.handleError(error, 'No se pudo enviar el código SMS.');
    } finally {
      this.loading.set(false);
    }
  }

  protected async resetPassword(): Promise<void> {
    if (!this.confirmationResult) {
      this.feedback.set(this.httpFeedback.warning('Primero solicita el código SMS.'));
      this.step.set('request');
      return;
    }
    if (!this.code.trim()) {
      this.feedback.set(this.httpFeedback.warning('Ingresa el código SMS.'));
      return;
    }
    if (this.newPassword.length < 8) {
      this.feedback.set(this.httpFeedback.warning('La nueva contraseña debe tener al menos 8 caracteres.'));
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.feedback.set(this.httpFeedback.warning('Las contraseñas no coinciden.'));
      return;
    }

    this.loading.set(true);
    this.feedback.set(null);
    try {
      const idToken = await this.firebaseAuth.verifyPasswordResetSms(this.confirmationResult, this.code.trim());
      await this.authSession.resetPasswordWithSms(this.email.trim(), idToken, this.newPassword);
      this.code = '';
      this.newPassword = '';
      this.confirmPassword = '';
      this.step.set('done');
      this.feedback.set(this.httpFeedback.success('Contraseña actualizada correctamente.'));
    } catch (error) {
      this.handleError(error, 'No se pudo cambiar la contraseña.');
    } finally {
      this.loading.set(false);
    }
  }

  private isEmail(value: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value);
  }

  private normalizePhone(value: string): string {
    const raw = (value || '').trim();
    if (!raw) {
      throw new Error('Ingresa el teléfono registrado de la cuenta.');
    }
    if (raw.startsWith('+')) {
      const clean = `+${raw.slice(1).replace(/\D/g, '')}`;
      if (clean.length >= 9) {
        return clean;
      }
    }
    const digits = raw.replace(/\D/g, '');
    if (digits.length === 10 && digits.startsWith('0')) {
      return `+593${digits.slice(1)}`;
    }
    if (digits.length >= 10) {
      return `+${digits}`;
    }
    throw new Error('Usa el formato internacional, por ejemplo +593999999999.');
  }

  private handleError(error: unknown, fallback: string): void {
    if (error instanceof Error && !(error instanceof HttpErrorResponse)) {
      this.feedback.set(this.httpFeedback.warning(error.message || fallback));
      return;
    }
    this.feedback.set(this.httpFeedback.fromError(error, fallback));
  }
}
