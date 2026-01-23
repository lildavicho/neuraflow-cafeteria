import axios from 'axios'

const resolveApiBaseUrl = () => {
  const explicitBase = import.meta.env.VITE_API_BASE_URL?.trim()
  if (explicitBase) return explicitBase.replace(/\/$/, '')

  if (typeof globalThis !== 'undefined' && globalThis.location?.origin) {
    try {
      const url = new URL(globalThis.location.href)
      const devPorts = new Set(['5173', '5174', '4173', '4174'])
      if (devPorts.has(url.port)) {
        const desiredPort =
          (import.meta.env.VITE_API_DEV_PORT?.trim()) ||
          (import.meta.env.VITE_API_PORT?.trim()) ||
          '8081'
        const effectiveOrigin = `${url.protocol}//${url.hostname}${desiredPort ? `:${desiredPort}` : ''}`
        return `${effectiveOrigin}/api`
      }
      return `${url.origin}/api`
    } catch {
      return '/api'
    }
  }
  return '/api'
}

export const API_BASE_URL = resolveApiBaseUrl().replace(/\/+$/, '')

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json; charset=UTF-8',
  },
})

// Interceptor de request - agregar token JWT
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Interceptor de response - refrescar token automático y limpiar sesión si no se puede renovar
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status
    const isAuthError = status === 401 || status === 403

    if (isAuthError && !originalRequest?._retry) {
      originalRequest._retry = true
      const refreshToken = localStorage.getItem('refreshToken')

      if (refreshToken) {
        try {
          const { data } = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
          if (data?.token) {
            localStorage.setItem('token', data.token)
            api.defaults.headers.common.Authorization = 'Bearer ' + data.token
            if (data?.refreshToken) {
              localStorage.setItem('refreshToken', data.refreshToken)
            }
            originalRequest.headers = originalRequest.headers || {}
            originalRequest.headers.Authorization = 'Bearer ' + data.token
            return api(originalRequest)
          }
        } catch (refreshError) {
          console.error('Refresh token failed', refreshError)
        }
      }
    }

    if (isAuthError) {
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      if (!globalThis.location?.pathname?.includes('/login')) {
        globalThis.location.href = '/login'
      }
    }

    return Promise.reject(error)
  }
)
// ---- Dashboard ----
export const fetchDashboardSnapshot = async () => {
  const { data } = await api.get('/dashboard/snapshot')
  return data
}

// ---- Products ----
export const fetchProducts = async ({ search = '', page = 0, size = 20 } = {}) => {
  const params = { page, size }
  if (search) params.search = search
  const { data } = await api.get('/products', { params })
  return data
}

export const fetchPublicProducts = async () => {
  const { data } = await api.get('/products/public')
  return data
}

export const fetchCategories = async () => {
  const { data } = await api.get('/categories')
  return data
}

export const createProduct = async (payload) => {
  const body = {
    categoryId: payload.categoryId,
    code: payload.code,
    name: payload.name,
    description: payload.description,
    imageUrl: payload.imageUrl,
    unit: payload.unit,
    price: payload.price,
    purchasePrice: payload.purchasePrice,
    stock: payload.stock,
    minStock: payload.minStock,
  }
  const { data } = await api.post('/products', body)
  return data
}

export const updateProduct = async (id, payload) => {
  const body = {
    categoryId: payload.categoryId,
    code: payload.code,
    name: payload.name,
    description: payload.description,
    imageUrl: payload.imageUrl,
    unit: payload.unit,
    price: payload.price,
    purchasePrice: payload.purchasePrice,
    minStock: payload.minStock,
    status: payload.status,
  }
  const { data } = await api.put(`/products/${id}`, body)
  return data
}

export const removeProduct = async (id) => api.delete(`/products/${id}`)

export const updateProductStock = async (id, { quantity, type, reason }) => {
  const { data } = await api.post(`/products/${id}/update-stock`, null, {
    params: { quantity, type, reason },
  })
  return data
}

// ---- Orders ----
export const fetchOrders = async ({ status, page = 0, size = 20 } = {}) => {
  const params = { page, size }
  if (status) params.status = status
  const { data } = await api.get('/orders', { params })
  return data
}

export const createOrder = async (payload) => {
  const { data } = await api.post('/orders', payload)
  return data
}

export const previewOrderTotals = async (items) => {
  const { data } = await api.post('/orders/prices', { items })
  return data
}

const postTransition = async (id, action, body) => {
  const { data } = await api.post(`/orders/${id}/${action}`, body)
  return data
}

export const acceptOrder = (id) => postTransition(id, 'accept')
export const readyOrder = (id) => postTransition(id, 'ready')
export const deliverOrder = (id, paymentReference) =>
  postTransition(id, 'deliver', paymentReference ? { paymentReference } : null)
export const cancelOrder = (id, reason) =>
  postTransition(id, 'cancel', reason ? { reason } : null)

export const payOrder = async (id, { method, reference }) => {
  const { data } = await api.patch(`/orders/${id}/pay`, { method, reference })
  return data
}

export const confirmOrderPayment = async (id, paymentReference) => {
  const body = paymentReference ? { paymentReference } : null
  const { data } = await api.post(`/orders/${id}/confirm-payment`, body)
  return data
}

// ---- Loyalty ----
export const fetchMyLoyalty = async () => {
  const { data } = await api.get('/loyalty/me')
  return data
}

export const fetchLoyaltyLedger = async (userId) => {
  const { data } = await api.get('/loyalty/ledger', { params: { userId } })
  return data
}

// ---- Analytics ----
export const fetchSalesAnalytics = async (params) => {
  const { data } = await api.get('/analytics/sales', { params })
  return data
}

export const fetchPeopleAnalytics = async (params) => {
  const { data } = await api.get('/analytics/people', { params })
  return data
}

export const fetchSalesVsPeople = async (params) => {
  const { data } = await api.get('/analytics/sales-vs-people', { params })
  return data
}

export const exportAnalytics = async (format, params) => {
  const { data } = await api.get('/analytics/export', {
    params: { ...params, format },
    responseType: 'blob',
  })
  return data
}

// ---- Push Notifications ----
export const registerPushToken = async ({ token, platform, deviceName, active }) => {
  await api.post('/push/register', { token, platform, deviceName, active })
}

export default api

