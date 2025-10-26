// Axios-based API client with JWT and auto-refresh
class ApiClient {
  constructor(axiosInstance) {
    this.axios = axiosInstance;
    this.baseURL = '/api';
  }

  // Generic request compatible with previous fetch-based API
  async request(url, options = {}) {
    const method = (options.method || 'GET').toLowerCase();
    const headers = options.headers || { 'Content-Type': 'application/json' };
    const data = options.body ? (typeof options.body === 'string' ? options.body : JSON.stringify(options.body)) : undefined;

    // Idempotency for sales
    if (method === 'post' && url.includes('/sales')) {
      headers['Idempotency-Key'] = this.generateIdempotencyKey();
    }

    const cfg = { url: this.baseURL + url, method, headers };
    if (data) cfg.data = data;

    const res = await this.axios.request(cfg);
    return res.data;
  }

  generateIdempotencyKey() {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  handleLogout() {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    window.location.href = '/pages/login.html';
  }

  // Auth helpers (preserve API)
  login(email, password, require2FA = false) {
    return this.request('/auth/login', { method: 'POST', body: { email, password, require2FA } });
  }
  register(userData) {
    return this.request('/auth/register', { method: 'POST', body: userData });
  }
  firebaseAuth(idToken) {
    return this.request('/auth/firebase', { method: 'POST', body: { idToken } });
  }
  verify2FA(email, code) {
    return this.request('/auth/2fa/verify', { method: 'POST', body: { email, code } });
  }

  // Products
  getProducts(page = 0, size = 20, search = '') {
    const params = new URLSearchParams({ page, size, search });
    return this.request(`/products?${params}`);
  }
  getProductById(id) { return this.request(`/products/${id}`); }
  getPublicProducts() { return this.request('/products/public'); }
  createProduct(product) { return this.request('/products', { method: 'POST', body: product }); }
  updateProduct(id, product) { return this.request(`/products/${id}`, { method: 'PUT', body: product }); }
  getLowStock() { return this.request('/products/low-stock'); }

  // Sales
  createSale(saleData) { return this.request('/sales', { method: 'POST', body: saleData }); }
  getSales(params = {}) { const q = new URLSearchParams(params); return this.request(`/sales?${q}`); }
  getMySales() { return this.request('/sales/mine'); }
  getSaleById(id) { return this.request(`/sales/${id}`); }
  getSalesSummary(period = 'daily') { return this.request(`/sales/summary?period=${period}`); }

  // Inventory
  getInventoryMovements(productId, page = 0) { return this.request(`/inventory/movements?productId=${productId}&page=${page}`); }
  updateStock(productId, quantity, type, reason) { return this.request('/inventory/update', { method: 'POST', body: { productId, quantity, type, reason } }); }

  // Users
  getUsers(page = 0, size = 20) { const p = new URLSearchParams({ page, size }); return this.request(`/users?${p}`); }
  getUserById(id) { return this.request(`/users/${id}`); }
  updateUser(id, userData) { return this.request(`/users/${id}`, { method: 'PUT', body: userData }); }
  updateProfile(profileData) { return this.request('/profile', { method: 'PUT', body: profileData }); }

  // Reports
  generateReport(type, format, params = {}) { const q = new URLSearchParams({ ...params, format }); return this.request(`/reports/${type}?${q}`); }

  // Search
  searchProducts(query) { return this.request(`/search/products?q=${encodeURIComponent(query)}`); }

  // Payments
  processPayment(paymentData) { const { method, ...data } = paymentData; return this.request(`/payments/${method.toLowerCase()}`, { method: 'POST', body: data }); }

  // Camera
  getCameraList() { return this.request('/camera/list'); }
  getCameraStream(cameraId) { return this.request(`/camera/webrtc/source?cameraId=${cameraId}`); }

  // Algorithms (best-effort)
  getRecommendations(productId) { return this.request(`/algo/recommendations?productId=${productId}`); }
  getForecast(productId, period = 'daily') { return this.request(`/algo/forecast?productId=${productId}&period=${period}`); }
  getAnomalies(from, to) { return this.request(`/algo/anomalies?from=${from}&to=${to}`); }
  getClusters() { return this.request('/algo/clusters'); }
  analyzeSentiment(text) { return this.request('/algo/sentiment', { method: 'POST', body: { text } }); }
}

// Axios global instance with interceptors
// Expect axios loaded via CDN on pages
const axiosInstance = window.axios ? window.axios.create({ baseURL: '/api' }) : null;

if (axiosInstance) {
  // Attach token
  axiosInstance.interceptors.request.use(cfg => {
    const t = localStorage.getItem('token');
    if (t) cfg.headers['Authorization'] = `Bearer ${t}`;
    return cfg;
  });

  let refreshing = null;
  axiosInstance.interceptors.response.use(
    r => r,
    async err => {
      if (err?.response?.status === 401) {
        const rt = localStorage.getItem('refreshToken');
        if (!rt) { localStorage.clear(); location.href = '/pages/login.html'; return Promise.reject(err); }
        refreshing ??= axiosInstance.post('/auth/refresh', { refreshToken: rt })
          .then(({ data }) => {
            localStorage.setItem('token', data.token);
            localStorage.setItem('refreshToken', data.refreshToken);
          })
          .finally(() => { refreshing = null; });
        await refreshing;
        err.config.headers['Authorization'] = `Bearer ${localStorage.getItem('token')}`;
        return axiosInstance.request(err.config);
      }
      return Promise.reject(err);
    }
  );
}

// Lightweight wrapper helpers
const api = {
  get: (path, opts={}) => axiosInstance.get(path, opts),
  post: (path, body, opts={}) => axiosInstance.post(path, body, opts),
  put: (path, body, opts={}) => axiosInstance.put(path, body, opts),
  del: (path, opts={}) => axiosInstance.delete(path, opts)
};

// Auth facade
export const auth = {
  async login(email, password, require2FA=false){
    const { data } = await api.post('/auth/login', { email, password, require2FA });
    if (!data.requires2FA){
      localStorage.setItem('token', data.token);
      localStorage.setItem('refreshToken', data.refreshToken);
      localStorage.setItem('user', JSON.stringify({ id:data.userId, email:data.email, name:data.fullName, role:data.role }));
    }
    return data;
  },
  async register(payload){
    const { data } = await api.post('/auth/register', payload);
    return data;
  },
  me(){
    try { return JSON.parse(localStorage.getItem('user')||'null'); } catch { return null; }
  },
  logout(){
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    window.location.href = '/pages/login.html';
  }
};

// Expose global axios API and default export for modules
if (axiosInstance) {
  window.api = api;
  window.apiAuth = auth;
}
const apiClient = new ApiClient(axiosInstance || { request: () => Promise.reject('Axios not loaded') });
export default apiClient;
