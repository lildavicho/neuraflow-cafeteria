import api from './apiService'

export const createOrder = async (orderData) => {
  const { data } = await api.post('/orders', orderData)
  return data
}

export const getOrders = async (params = {}) => {
  const { data } = await api.get('/orders', { params })
  return data
}

export const getOrderById = async (id) => {
  const { data } = await api.get(`/orders/${id}`)
  return data
}

export const updateOrderStatus = async (id, status) => {
  const { data } = await api.patch(`/orders/${id}/status`, { status })
  return data
}

export const acceptOrder = async (id) => {
  return updateOrderStatus(id, 'READY')
}

export const rejectOrder = async (id) => {
  return updateOrderStatus(id, 'REJECTED')
}
