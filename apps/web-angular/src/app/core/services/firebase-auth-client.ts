import { Injectable } from '@angular/core';
import { initializeApp, getApp, getApps, type FirebaseApp, type FirebaseOptions } from 'firebase/app';
import {
  browserLocalPersistence,
  type ConfirmationResult,
  getAuth,
  getRedirectResult,
  GoogleAuthProvider,
  OAuthProvider,
  RecaptchaVerifier,
  setPersistence,
  signInWithPhoneNumber,
  signInWithPopup,
  signInWithRedirect,
  signOut,
} from 'firebase/auth';

type FirebaseRuntimeConfig = FirebaseOptions & {
  apiKey?: string;
  authDomain?: string;
  projectId?: string;
  appId?: string;
};

@Injectable({ providedIn: 'root' })
export class FirebaseAuthClient {
  private app: FirebaseApp | null = null;
  private recaptchaVerifier: RecaptchaVerifier | null = null;

  async signInWithGoogle(): Promise<string> {
    return this.signInWithProvider('google');
  }

  async signInWithMicrosoft(): Promise<string> {
    return this.signInWithProvider('microsoft');
  }

  async signInWithProvider(providerId: 'google' | 'microsoft'): Promise<string> {
    const auth = getAuth(this.getApp());
    const provider = this.createProvider(providerId);
    try {
      const credential = await signInWithPopup(auth, provider);
      return credential.user.getIdToken();
    } catch (error) {
      if (this.shouldFallbackToRedirect(error)) {
        await setPersistence(auth, browserLocalPersistence);
        await signInWithRedirect(auth, provider);
        return new Promise<string>(() => undefined);
      }
      throw error;
    }
  }

  async consumeRedirectResult(): Promise<string | null> {
    const auth = getAuth(this.getApp());
    await setPersistence(auth, browserLocalPersistence);
    const credential = await getRedirectResult(auth);
    return credential ? credential.user.getIdToken() : null;
  }

  async sendPasswordResetSms(phoneNumber: string, containerId: string): Promise<ConfirmationResult> {
    const auth = getAuth(this.getApp());
    this.recaptchaVerifier?.clear();
    this.recaptchaVerifier = new RecaptchaVerifier(auth, containerId, {
      size: 'invisible',
      callback: () => undefined,
    });
    return signInWithPhoneNumber(auth, phoneNumber, this.recaptchaVerifier);
  }

  async verifyPasswordResetSms(confirmationResult: ConfirmationResult, code: string): Promise<string> {
    const credential = await confirmationResult.confirm(code);
    const idToken = await credential.user.getIdToken(true);
    await signOut(getAuth(this.getApp()));
    return idToken;
  }

  private getApp(): FirebaseApp {
    if (this.app) {
      return this.app;
    }

    const config = this.readConfig();
    if (!config.apiKey || !config.authDomain || !config.projectId || !config.appId) {
      throw new Error('Configura Firebase Web en public/env.js antes de usar autenticacion Firebase.');
    }

    this.app = getApps().length > 0 ? getApp() : initializeApp(config);
    return this.app;
  }

  private readConfig(): FirebaseRuntimeConfig {
    const globalWindow = globalThis as typeof globalThis & {
      __env?: {
        firebase?: FirebaseRuntimeConfig;
        firebaseApiKey?: string;
        firebaseAuthDomain?: string;
        firebaseProjectId?: string;
        firebaseAppId?: string;
        firebaseMessagingSenderId?: string;
        firebaseStorageBucket?: string;
      };
    };
    const env = globalWindow.__env ?? {};
    return {
      apiKey: env.firebase?.apiKey ?? env.firebaseApiKey,
      authDomain: env.firebase?.authDomain ?? env.firebaseAuthDomain,
      projectId: env.firebase?.projectId ?? env.firebaseProjectId,
      appId: env.firebase?.appId ?? env.firebaseAppId,
      messagingSenderId: env.firebase?.messagingSenderId ?? env.firebaseMessagingSenderId,
      storageBucket: env.firebase?.storageBucket ?? env.firebaseStorageBucket,
    };
  }

  private createProvider(providerId: 'google' | 'microsoft'): GoogleAuthProvider | OAuthProvider {
    if (providerId === 'google') {
      const provider = new GoogleAuthProvider();
      provider.addScope('email');
      provider.addScope('profile');
      provider.setCustomParameters({ prompt: 'select_account' });
      return provider;
    }

    const provider = new OAuthProvider('microsoft.com');
    provider.addScope('openid');
    provider.addScope('email');
    provider.addScope('profile');
    provider.setCustomParameters({ prompt: 'select_account' });
    return provider;
  }

  private shouldFallbackToRedirect(error: unknown): boolean {
    const code = this.firebaseErrorCode(error);
    return code === 'auth/popup-blocked' || code === 'auth/operation-not-supported-in-this-environment';
  }

  private firebaseErrorCode(error: unknown): string {
    if (typeof error === 'object' && error !== null && 'code' in error) {
      const code = (error as { code?: unknown }).code;
      return typeof code === 'string' ? code : '';
    }
    return '';
  }
}
