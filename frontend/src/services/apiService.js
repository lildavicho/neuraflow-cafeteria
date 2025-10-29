import axios from 'axios';
import { getIdToken } from './authService';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use(
  async (config) => {
    const token = await getIdToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const getProductos = async ({ search = '', page = 0, size = 20 } = {}) => {
  try {
    const response = await apiClient.get('/productos', {
      params: { search, page, size },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching productos:', error);
    throw error;
  }
};

export const getProductoById = async (id) => {
  try {
    const response = await apiClient.get(`/productos/${id}`);
    return response.data;
  } catch (error) {
    console.error('Error fetching producto:', error);
    throw error;
  }
};

export const createProducto = async (payload) => {
  try {
    const response = await apiClient.post('/productos', payload);
    return response.data;
  } catch (error) {
    console.error('Error creating producto:', error);
    throw error;
  }
};

export const updateProducto = async (id, payload) => {
  try {
    const response = await apiClient.put(`/productos/${id}`, payload);
    return response.data;
  } catch (error) {
    console.error('Error updating producto:', error);
    throw error;
  }
};

export const deleteProducto = async (id) => {
  try {
    await apiClient.delete(`/productos/${id}`);
  } catch (error) {
    console.error('Error deleting producto:', error);
    throw error;
  }
};

export const getVentas = async ({ from, to, page = 0, size = 20 } = {}) => {
  try {
    const response = await apiClient.get('/ventas', {
      params: { from, to, page, size },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching ventas:', error);
    throw error;
  }
};

export const createVenta = async (payload) => {
  try {
    const response = await apiClient.post('/ventas', payload);
    return response.data;
  } catch (error) {
    console.error('Error creating venta:', error);
    throw error;
  }
};

export const getEstadisticas = async ({ from, to } = {}) => {
  try {
    const response = await apiClient.get('/estadisticas', {
      params: { from, to },
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching estadisticas:', error);
    return {
      ventasHoy: 0,
      ordenesHoy: 0,
      totalProductos: 0,
      ticketPromedio: 0,
    };
  }
};

export const getCategorias = async () => {
  try {
    const response = await apiClient.get('/categorias');
    return response.data;
  } catch (error) {
    console.error('Error fetching categorias:', error);
    return [];
  }
};

export default apiClient;
