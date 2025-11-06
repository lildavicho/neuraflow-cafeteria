import api from './apiService'

export const getProducts = async ({ active = true, prepared = null } = {}) => {
  const params = { active }
  if (prepared !== null) {
    params.prepared = prepared
  }
  const { data } = await api.get('/products', { params })
  return Array.isArray(data) ? data : (data?.content ?? [])
}

export const getProductById = async (id) => {
  const { data } = await api.get(`/products/${id}`)
  return data
}

export const createProduct = async (product) => {
  const { data } = await api.post('/products', {
    name: product.name,
    categoryId: parseInt(product.categoryId, 10),
    price: parseFloat(product.price),
    stock: product.stock != null ? parseInt(product.stock, 10) : 0,
    description: product.description || '',
    prepared: Boolean(product.prepared),
  })
  return data
}

export const updateProduct = async (id, product) => {
  const body = {
    categoryId: product.categoryId != null ? parseInt(product.categoryId, 10) : undefined,
    name: product.name,
    description: product.description ?? undefined,
    price: product.price != null ? parseFloat(product.price) : undefined,
    prepared: typeof product.prepared === 'boolean' ? product.prepared : undefined,
    status: product.status ?? undefined,
  }
  // Clean undefineds so we don't send them
  Object.keys(body).forEach((k) => body[k] === undefined && delete body[k])
  const { data } = await api.put(`/products/${id}`, body)
  return data
}

export const deleteProduct = async (id) => {
  await api.delete(`/products/${id}`)
}

