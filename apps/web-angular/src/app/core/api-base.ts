export const resolveApiBaseUrl = (): string => {
  const globalWindow = globalThis as typeof globalThis & { __env?: { apiBaseUrl?: string } };
  const explicitBase = globalWindow.__env?.apiBaseUrl?.trim();
  if (explicitBase) {
    return explicitBase.replace(/\/$/, '');
  }

  if (typeof window !== 'undefined') {
    const devPorts = new Set(['4200']);
    const localHosts = new Set(['localhost', '127.0.0.1', '::1', '[::1]']);
    if (localHosts.has(window.location.hostname) && devPorts.has(window.location.port)) {
      return window.location.origin.replace(/:\d+$/, ':8081') + '/api';
    }
  }

  return '/api';
};

export const API_BASE_URL = resolveApiBaseUrl().replace(/\/+$/, '');
