import { Injectable, effect, signal } from '@angular/core';

export type AppTheme = 'dark' | 'light';

type ViewTransitionCapableDocument = Document & {
  startViewTransition?: (updateCallback: () => void | Promise<void>) => {
    finished: Promise<void>;
  };
};

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<AppTheme>(this.resolveInitialTheme());
  private transitionTimer: number | null = null;

  constructor() {
    effect(() => {
      const t = this.theme();
      document.documentElement.setAttribute('data-theme', t);
      localStorage.setItem('erp-theme', t);
    });
    document.documentElement.setAttribute('data-theme', this.theme());
  }

  toggle(): void {
    const nextTheme: AppTheme = this.theme() === 'dark' ? 'light' : 'dark';
    this.setTheme(nextTheme);
  }

  setTheme(nextTheme: AppTheme): void {
    if (nextTheme === this.theme()) {
      return;
    }

    const doc = document as ViewTransitionCapableDocument;
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    this.startThemeTransition();

    if (doc.startViewTransition && !reducedMotion) {
      doc
        .startViewTransition(() => {
          this.theme.set(nextTheme);
        })
        .finished.finally(() => this.finishThemeTransition());
      return;
    }

    this.theme.set(nextTheme);
    window.setTimeout(() => this.finishThemeTransition(), 360);
  }

  get isDark(): boolean {
    return this.theme() === 'dark';
  }

  private resolveInitialTheme(): AppTheme {
    const storedTheme = localStorage.getItem('erp-theme');
    if (storedTheme === 'dark' || storedTheme === 'light') {
      return storedTheme;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  private startThemeTransition(): void {
    const root = document.documentElement;
    root.setAttribute('data-theme-transitioning', '');
    if (this.transitionTimer !== null) {
      window.clearTimeout(this.transitionTimer);
    }
  }

  private finishThemeTransition(): void {
    const root = document.documentElement;
    if (this.transitionTimer !== null) {
      window.clearTimeout(this.transitionTimer);
    }
    this.transitionTimer = window.setTimeout(() => {
      root.removeAttribute('data-theme-transitioning');
      this.transitionTimer = null;
    }, 40);
  }
}
