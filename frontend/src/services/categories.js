import api from './apiService'

export const getCategories = async ({ active } = {}) => {
  const params = {}
  if (typeof active === 'boolean') {
    params.active = active
  }
  const { data } = await api.get('/categories', { params })
  return data
}

export const getCategoryById = async (id) => {
  const { data } = await api.get(`/categories/${id}`)
  return data
}

export const seedCategories = async () => {
  const { data } = await api.post('/categories/seed')
  return data
}
