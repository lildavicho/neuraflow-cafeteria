import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../api-base';
import { AuthResponse, AuthUser } from '../models/auth';
import { FirebaseAuthClient } from './firebase-auth-client';

const ACCESS_TOKEN_KEY = 'token';
const REFRESH_TOKEN_KEY = 'refreshToken';
const USER_KEY = 'auth.user';
const FIREBASE_REDIRECT_TENANT_KEY = 'auth.firebaseRedirectTenant';

@Injectable({ providedIn: 'root' })
export class AuthSession {
  private readonly http = inject(HttpClient);
  private readonly firebaseAuth = inject(FirebaseAuthClient);

  readonly token = signal<string | null>(this.readStorage(ACCESS_TOKEN_KEY));
  readonly refreshToken = signal<string | null>(this.readStorage(REFRESH_TOKEN_KEY));
  readonly user = signal<AuthUser | null>(this.readUser());
  readonly authenticating = signal(false);
  readonly isAuthenticated = computed(() => this.hasActiveToken(this.token()));

  constructor() {
    if (this.token() && !this.isAuthenticated()) {
      this.clearSession();
    }
  }

  async login(email: string, password: string, tenantCode: string): Promise<AuthUser> {
    this.authenticating.set(true);

    try {
      const response = await firstValueFrom(
        this.http.post<AuthResponse>(`${API_BASE_URL}/auth/login`, {
          email,
          password,
        }),
      );

      if (response.requires2FA) {
        throw new Error('El backend pidió 2FA y este frontend aún no expone ese paso.');
      }

      const user = this.toAuthUser(response);

      this.persistSession(response.token, response.refreshToken, user, response.tenantCode ?? tenantCode);
      return user;
    } finally {
      this.authenticating.set(false);
    }
  }

  async loginWithGoogle(tenantCode: string): Promise<AuthUser> {
    return this.loginWithFirebaseProvider('google', tenantCode);
  }

  async loginWithMicrosoft(tenantCode: string): Promise<AuthUser> {
    return this.loginWithFirebaseProvider('microsoft', tenantCode);
  }

  async resetPasswordWithSms(email: string, firebaseIdToken: string, newPassword: string): Promise<void> {
    await firstValueFrom(
      this.http.post(`${API_BASE_URL}/auth/password-reset/sms`, {
        email,
        idToken: firebaseIdToken,
        newPassword,
      }),
    );
  }

  async completePendingFirebaseRedirect(): Promise<AuthUser | null> {
    const idToken = await this.firebaseAuth.consumeRedirectResult();
    if (!idToken) {
      return null;
    }

    const tenantCode = this.readRedirectTenantCode();
    this.authenticating.set(true);
    try {
      const user = await this.completeFirebaseLogin(idToken, tenantCode);
      this.clearRedirectTenantCode();
      return user;
    } finally {
      this.authenticating.set(false);
    }
  }

  private async loginWithFirebaseProvider(provider: 'google' | 'microsoft', tenantCode: string): Promise<AuthUser> {
    this.authenticating.set(true);

    try {
      const normalizedTenantCode = this.normalizeOptionalTenantCode(tenantCode);
      this.storeRedirectTenantCode(normalizedTenantCode);
      const idToken = await this.firebaseAuth.signInWithProvider(provider);
      const user = await this.completeFirebaseLogin(idToken, normalizedTenantCode);
      this.clearRedirectTenantCode();
      return user;
    } finally {
      this.authenticating.set(false);
    }
  }

  async registerTenant(payload: {
    tenantCode: string;
    legalName: string;
    tradeName?: string;
    fullName: string;
    email: string;
    password: string;
    phone?: string;
  }): Promise<AuthUser> {
    this.authenticating.set(true);
    try {
      const response = await firstValueFrom(
        this.http.post<AuthResponse>(`${API_BASE_URL}/auth/register-tenant`, payload),
      );
      const user = this.toAuthUser(response);
      this.persistSession(response.token, response.refreshToken, user, response.tenantCode ?? payload.tenantCode);
      return user;
    } finally {
      this.authenticating.set(false);
    }
  }

  private async completeFirebaseLogin(idToken: string, tenantCode: string): Promise<AuthUser> {
    const response = await firstValueFrom(
      this.http.post<AuthResponse>(`${API_BASE_URL}/auth/firebase`, {
        idToken,
        tenantCode: this.normalizeOptionalTenantCode(tenantCode),
      }),
    );
    const user = this.toAuthUser(response);
    this.persistSession(response.token, response.refreshToken, user, response.tenantCode ?? tenantCode);
    return user;
  }

  logout(): void {
    this.clearSession();
  }

  private persistSession(token: string, refreshToken: string, user: AuthUser, tenantCode: string): void {
    const normalizedUser = {
      ...user,
      platformAdmin: this.normalizePlatformAdmin(user.platformAdmin),
    };

    this.token.set(token);
    this.refreshToken.set(refreshToken);
    this.user.set(normalizedUser);

    if (typeof localStorage === 'undefined') {
      return;
    }

    const normalizedTenant = this.normalizeTenantCode(tenantCode);
    localStorage.setItem(ACCESS_TOKEN_KEY, token);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(normalizedUser));
    localStorage.setItem('tenantCode', normalizedTenant);

    if (normalizedUser.commercialPlan && normalizedUser.enabledModules && normalizedUser.enabledModules.length > 0) {
      const snapshot = {
        tenantCode: normalizedTenant,
        commercialPlan: normalizedUser.commercialPlan,
        enabledModules: normalizedUser.enabledModules,
        availablePlans: ['START', 'PRO', 'VISION_AI'],
      };
      localStorage.setItem(`tenant-plan:${normalizedTenant}`, JSON.stringify(snapshot));
    }
  }

  private toAuthUser(response: AuthResponse): AuthUser {
    return {
      userId: response.userId,
      email: response.email,
      fullName: response.fullName,
      role: response.role,
      tenantCode: response.tenantCode,
      platformAdmin: this.normalizePlatformAdmin(response.platformAdmin),
      commercialPlan: response.commercialPlan,
      enabledModules: response.enabledModules ?? [],
    };
  }

  private clearSession(): void {
    this.token.set(null);
    this.refreshToken.set(null);
    this.user.set(null);

    if (typeof localStorage === 'undefined') {
      return;
    }

    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem('tenantCode');

    const keysToRemove: string[] = [];
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index);
      if (key?.startsWith('tenant-plan:')) {
        keysToRemove.push(key);
      }
    }

    for (const key of keysToRemove) {
      localStorage.removeItem(key);
    }
  }

  private readStorage(key: string): string | null {
    if (typeof localStorage === 'undefined') {
      return null;
    }
    return localStorage.getItem(key);
  }

  private readUser(): AuthUser | null {
    if (typeof localStorage === 'undefined') {
      return null;
    }

    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as AuthUser;
      if (!parsed?.email || !parsed?.role) {
        return null;
      }
      return {
        ...parsed,
        platformAdmin: this.normalizePlatformAdmin(parsed.platformAdmin),
        enabledModules: parsed.enabledModules ?? [],
      };
    } catch {
      return null;
    }
  }

  private hasActiveToken(token: string | null): boolean {
    if (!token) {
      return false;
    }

    const payload = this.decodeTokenPayload(token);
    const expiresAt = payload?.['exp'];
    if (typeof expiresAt !== 'number') {
      return false;
    }

    return expiresAt * 1000 > Date.now();
  }

  private decodeTokenPayload(token: string): Record<string, unknown> | null {
    const segments = token.split('.');
    if (segments.length < 2) {
      return null;
    }

    try {
      const normalized = segments[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
      const decoded = atob(padded);
      return JSON.parse(decoded) as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private normalizeTenantCode(tenantCode: string): string {
    return (tenantCode || '').trim().toLowerCase() || 'default';
  }

  private storeRedirectTenantCode(tenantCode: string): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }
    sessionStorage.setItem(FIREBASE_REDIRECT_TENANT_KEY, this.normalizeOptionalTenantCode(tenantCode));
  }

  private readRedirectTenantCode(): string {
    if (typeof sessionStorage === 'undefined') {
      return this.normalizeOptionalTenantCode(this.readStorage('tenantCode') ?? '');
    }
    return this.normalizeOptionalTenantCode(
      sessionStorage.getItem(FIREBASE_REDIRECT_TENANT_KEY) ?? this.readStorage('tenantCode') ?? '',
    );
  }

  private clearRedirectTenantCode(): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }
    sessionStorage.removeItem(FIREBASE_REDIRECT_TENANT_KEY);
  }

  private normalizeOptionalTenantCode(tenantCode: string): string {
    return (tenantCode || '').trim().toLowerCase();
  }

  private normalizePlatformAdmin(value: unknown): boolean {
    if (value === true) {
      return true;
    }
    if (value === false || value == null) {
      return false;
    }
    if (typeof value === 'string') {
      return value.trim().toLowerCase() === 'true';
    }
    if (typeof value === 'number') {
      return value === 1;
    }
    return false;
  }
}
