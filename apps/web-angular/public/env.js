(function () {
  var runtimeEnv = window.__env || {};
  var devPorts = new Set(['4200']);
  var hostname = window.location.hostname;
  var isLocalHost = hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1' || hostname === '[::1]';

  if (!runtimeEnv.apiBaseUrl) {
    runtimeEnv.apiBaseUrl =
      isLocalHost && devPorts.has(window.location.port)
        ? window.location.origin.replace(/:\d+$/, ':8081') + '/api'
        : '/api';
  }

  window.__env = runtimeEnv;
})();
