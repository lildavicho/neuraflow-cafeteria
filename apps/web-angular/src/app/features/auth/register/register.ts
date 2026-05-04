import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthSession } from '../../../core/services/auth-session';
import { HttpFeedback } from '../../../core/services/http-feedback';
import { UiFeedback } from '../../../core/models/ui-feedback';
import { RequestFeedback } from '../../shared/components/request-feedback';
import { FormField } from '../../shared/components/form-field';
import {
  AppValidators,
  ecPhone,
  matchControl,
  strongPassword,
} from '../../shared/validators/validators';

@Component({
  selector: 'app-register',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, RequestFeedback, FormField],
  template: `
    <section class="auth-page">
      <article class="auth-card" [attr.aria-busy]="submitting()">
        <span class="page__eyebrow">Alta de negocio</span>
        <h2>Crea tu espacio comercial</h2>
        <p class="auth-lead">
          Registramos tu negocio, la suscripción inicial y tu usuario administrador en un solo paso.
        </p>

        @if (feedback(); as notice) {
          <app-request-feedback [tone]="notice.tone" [message]="notice.message" [traceId]="notice.traceId" />
        }

        <form [formGroup]="form" (ngSubmit)="submit()" class="rg-form" autocomplete="on">

          <h3 class="rg-section">Negocio</h3>
          <div class="rg-grid rg-grid--2">
            <app-form-field label="Código del negocio" [required]="true"
                            [control]="form.controls.tenantCode"
                            hint="Se usará en URLs y accesos (sólo minúsculas, números y guiones)">
              <input type="text" formControlName="tenantCode" placeholder="mi-negocio" autocomplete="organization" />
            </app-form-field>
            <app-form-field label="Razón social" [required]="true" [control]="form.controls.legalName">
              <input type="text" formControlName="legalName" autocomplete="organization" />
            </app-form-field>
            <app-form-field label="Nombre comercial" [control]="form.controls.tradeName" hint="Opcional">
              <input type="text" formControlName="tradeName" />
            </app-form-field>
          </div>

          <h3 class="rg-section">Administrador</h3>
          <div class="rg-grid rg-grid--2">
            <app-form-field label="Nombre completo" [required]="true" [control]="form.controls.fullName">
              <input type="text" formControlName="fullName" autocomplete="name" />
            </app-form-field>
            <app-form-field label="Email" [required]="true" [control]="form.controls.email">
              <input type="email" formControlName="email" autocomplete="email" inputmode="email" />
            </app-form-field>
            <app-form-field label="Teléfono" [control]="form.controls.phone" hint="Opcional">
              <input type="tel" formControlName="phone" autocomplete="tel" inputmode="tel" />
            </app-form-field>
            <app-form-field label="Contraseña" [required]="true" [control]="form.controls.password"
                            hint="Mínimo 8 caracteres, con letras y números">
              <input type="password" formControlName="password" autocomplete="new-password" />
            </app-form-field>
            <app-form-field label="Confirmar contraseña" [required]="true" [control]="form.controls.confirmPassword">
              <input type="password" formControlName="confirmPassword" autocomplete="new-password" />
            </app-form-field>
          </div>

          <footer class="rg-actions">
            <a routerLink="/login" class="rg-link">Ya tengo cuenta</a>
            <button type="submit" class="page__button rg-submit"
                    [disabled]="submitting() || form.invalid">
              {{ submitting() ? 'Creando…' : 'Crear negocio y cuenta' }}
            </button>
          </footer>
        </form>
      </article>
    </section>
  `,
  styles: `
    :host { display: contents; }
    .auth-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 32px 16px;
      background: var(--bg, #0b0f1a);
    }
    .auth-card {
      width: min(720px, 100%);
      background: var(--surface, rgba(17, 24, 39, 0.96));
      border: 1px solid var(--aurora-border, rgba(148, 163, 184, 0.16));
      border-radius: 14px;
      padding: 32px;
      display: flex;
      flex-direction: column;
      gap: 14px;
      box-shadow: 0 30px 80px rgba(0, 0, 0, 0.35);
    }
    .page__eyebrow {
      font-size: 11px;
      letter-spacing: 0.18em;
      text-transform: uppercase;
      color: var(--aurora-dim, rgba(148, 163, 184, 0.72));
    }
    h2 { margin: 0; font-size: 22px; font-weight: 600; color: var(--text, rgba(226, 232, 240, 0.96)); letter-spacing: -0.01em; }
    .auth-lead { margin: 0 0 8px; font-size: 13px; color: var(--aurora-dim, rgba(148, 163, 184, 0.72)); }

    .rg-section { margin: 6px 0 -4px; font-size: 11px; letter-spacing: 0.16em; text-transform: uppercase;
                  color: var(--aurora-dim, rgba(148, 163, 184, 0.7)); font-weight: 600; }
    .rg-grid { display: grid; gap: 14px; }
    .rg-grid--2 { grid-template-columns: repeat(2, 1fr); }
    @media (max-width: 640px) { .rg-grid--2 { grid-template-columns: 1fr; } }

    .rg-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; gap: 12px; }
    .rg-link { font-size: 13px; color: var(--aurora-dim, rgba(148, 163, 184, 0.85)); text-decoration: none; }
    .rg-link:hover { color: var(--text, rgba(226, 232, 240, 0.96)); }
    .page__button {
      padding: 10px 18px;
      background: var(--aurora-accent, rgba(129, 140, 248, 0.9));
      color: white;
      border: 1px solid transparent;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: background 140ms ease;
    }
    .page__button:hover:not([disabled]) { background: rgba(129, 140, 248, 1); }
    .page__button[disabled] { opacity: 0.5; cursor: not-allowed; }
  `,
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthSession);
  private readonly router = inject(Router);
  private readonly httpFeedback = inject(HttpFeedback);

  protected readonly submitting = signal(false);
  protected readonly feedback = signal<UiFeedback | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    tenantCode: ['', [
      Validators.required, Validators.minLength(3), Validators.maxLength(64),
      Validators.pattern(/^[a-z0-9][a-z0-9_-]*[a-z0-9]$/),
    ]],
    legalName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(255)]],
    tradeName: [''],
    fullName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    email: ['', [Validators.required, AppValidators.email]],
    phone: ['', [ecPhone]],
    password: ['', [Validators.required, strongPassword]],
    confirmPassword: ['', [Validators.required, matchControl('password')]],
  });

  async submit(): Promise<void> {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.submitting()) return;

    this.submitting.set(true);
    this.feedback.set(null);

    const raw = this.form.getRawValue();
    try {
      await this.auth.registerTenant({
        tenantCode: raw.tenantCode.trim().toLowerCase(),
        legalName: raw.legalName.trim(),
        tradeName: raw.tradeName?.trim() || undefined,
        fullName: raw.fullName.trim(),
        email: raw.email.trim().toLowerCase(),
        password: raw.password,
        phone: raw.phone?.trim() || undefined,
      });
      await this.router.navigateByUrl('/dashboard');
    } catch (error) {
      const notice = error instanceof HttpErrorResponse
        ? this.httpFeedback.fromError(error, 'No se pudo crear el negocio. Revisa los datos.')
        : this.httpFeedback.fromError(error as HttpErrorResponse,
            'No se pudo crear el negocio. Revisa los datos.');
      this.feedback.set(notice);
    } finally {
      this.submitting.set(false);
    }
  }
}
