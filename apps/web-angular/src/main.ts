import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app';

const normalizeLegacyFragmentRoute = (): void => {
  if (typeof window === 'undefined') {
    return;
  }

  const legacyRoutes: Record<string, string> = {
    landing: '/',
    login: '/login',
    portal: '/pos',
    unauthorized: '/login',
  };
  const fragment = window.location.hash.replace(/^#/, '').toLowerCase();
  const targetPath = legacyRoutes[fragment];

  if (!targetPath) {
    return;
  }

  window.history.replaceState(null, '', `${targetPath}${window.location.search}`);
};

normalizeLegacyFragmentRoute();

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
