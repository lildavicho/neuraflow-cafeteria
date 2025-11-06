import api from './apiService'

export const getCategories = async ({ active = true } = {}) => {
  const { data } = await api.get('/categories', { params: { active } })
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
