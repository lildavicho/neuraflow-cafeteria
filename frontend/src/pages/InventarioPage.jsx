import { useCallback, useEffect, useRef, useState } from 'react'
import gsap from 'gsap'
import { getProducts, createProduct, updateProduct, deleteProduct } from '../services/products'
import { updateProductStock as updateStock } from '../services/apiService'
import { getCategories, seedCategories } from '../services/categories'
import { useInventoryUpdates } from '../hooks/useWebSocket'
import { useAuth } from '../hooks/useAuth'
import Loader from '../components/common/Loader'
import { useToast } from '../hooks/useToast'
import Toast from '../components/common/Toast'
import ConfirmModal from '../components/common/ConfirmModal'

const emptyForm = {
  id: null,
  categoryId: '',
  name: '',
  description: '',
  price: '',
  stock: '',
  originalStock: null,
  prepared: true,
}

// ============ ESTRUCTURAS DE DATOS ============

// Cola (Queue) - FIFO para historial de operaciones
class OperationsQueue {
  constructor() {
    this.items = []
    this.maxSize = 10
  }

  enqueue(operation) {
    if (this.items.length >= this.maxSize) {
      this.items.shift() // Eliminar el más antiguo
    }
    this.items.push({
      ...operation,
      timestamp: new Date().toLocaleTimeString()
    })
  }

  getAll() {
    return [...this.items]
  }

  clear() {
    this.items = []
  }
}

// Pila (Stack) - LIFO para acciones deshacer
class ActionsStack {
  constructor() {
    this.items = []
    this.maxSize = 5
  }

  push(action) {
    if (this.items.length >= this.maxSize) {
      this.items.shift() // Eliminar el más antiguo del fondo
    }
    this.items.push({
      ...action,
      timestamp: new Date().toLocaleTimeString()
    })
  }

  pop() {
    return this.items.pop()
  }

  peek() {
    return this.items[this.items.length - 1]
  }

  getAll() {
    return [...this.items].reverse() // Mostrar más reciente primero
  }

  isEmpty() {
    return this.items.length === 0
  }
}

// Árbol de Categorías
class CategoryTreeNode {
  constructor(name, id = null) {
    this.name = name
    this.id = id
    this.children = []
    this.productCount = 0
  }

  addChild(node) {
    this.children.push(node)
  }
}

// ============ COMPONENTE PRINCIPAL ============

const InventarioPage = () => {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  const { toasts, success, error, removeToast } = useToast()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmId, setConfirmId] = useState(null)
  const iconRefs = useRef({})
  const loadingRef = useRef(false)
  const { token, loading: authLoading } = useAuth()

  // Estado de ordenamiento
  const [sortBy, setSortBy] = useState('name')
  const [sortOrder, setSortOrder] = useState('asc')

  // Estructuras de datos
  const operationsQueueRef = useRef(new OperationsQueue())
  const actionsStackRef = useRef(new ActionsStack())
  const [structuresView, setStructuresView] = useState(false)
  const [, forceUpdate] = useState({}) // Para forzar re-render

  const iconForCategory = (name = '') => {
    const n = String(name || '').toLowerCase()
    if (n.includes('bebi')) return 'bi-cup-hot'
    if (n.includes('ingre')) return 'bi-box-seam'
    if (n.includes('post') || n.includes('dulce')) return 'bi-cookie'
    if (n.includes('desa') || n.includes('plato') || n.includes('comida')) return 'bi-egg-fried'
    return 'bi-bag'
  }

  // Construir árbol de categorías
  const buildCategoryTree = useCallback(() => {
    const root = new CategoryTreeNode('Todas las Categorías')
    const safeCategories = Array.isArray(categories) ? categories : []
    const safeProducts = Array.isArray(products) ? products : []

    safeCategories.forEach(cat => {
      const node = new CategoryTreeNode(cat.name, cat.id)
      node.productCount = safeProducts.filter(p => p.categoryId === cat.id).length
      root.addChild(node)
    })

    return root
  }, [categories, products])

  const loadData = useCallback(async () => {
    if (authLoading) return
    if (!token) {
      setLoading(false)
      setLoadError('Inicia sesi\u00f3n para ver el inventario.')
      return
    }
    if (loadingRef.current) return
    loadingRef.current = true
    setLoading(true)
    setLoadError(null)
    try {
      const [productList, initialCats] = await Promise.all([
        getProducts({ active: true }),
        getCategories({ active: true }),
      ])
      iconRefs.current = {}
      setProducts(Array.isArray(productList) ? productList : [])
      let cats = Array.isArray(initialCats) ? initialCats : []
      if (!cats.length) {
        try {
          const fallback = await getCategories()
          if (Array.isArray(fallback) && fallback.length) {
            cats = fallback
          }
        } catch (e) {
          console.error('Error loading all categories', e)
        }
      }
      if (!cats.length) {
        try {
          const seeded = await seedCategories()
          cats = Array.isArray(seeded) ? seeded : []
          if (cats.length) success('Se crearon categorias por defecto')
        } catch (e) {
          console.error('No se pudo sembrar categorias', e)
        }
      }
      setCategories(cats)

      // Registrar operación en cola
      operationsQueueRef.current.enqueue({
        type: 'LOAD',
        description: `Cargados ${productList?.length || 0} productos`
      })
      forceUpdate({})
    } catch (err) {
      console.error('Error loading inventory:', err)
      const status = err?.response?.status
      const message = status === 401 || status === 403
        ? 'Sesión expirada. Inicia sesión nuevamente.'
        : 'Error al cargar el inventario. Intenta nuevamente.'
      setLoadError(message)
      error(message)
    } finally {
      setLoading(false)
      loadingRef.current = false
    }
  }, [authLoading, token, success, error])

  useEffect(() => {
    if (!authLoading) {
      loadData()
    }
  }, [authLoading, loadData])

  const handleInventorySync = useCallback((data) => {
    if (data && !authLoading && token && !loadingRef.current && !loadError) {
      loadData()
    }
  }, [authLoading, token, loadData, loadError])

  useInventoryUpdates(handleInventorySync)

  const safeProducts = Array.isArray(products) ? products : []
  const safeCategories = Array.isArray(categories) ? categories : []

  // Filtrar y ordenar productos
  const getSortedProducts = () => {
    let filtered = safeProducts
      .filter((p) => p?.status !== 'DISCONTINUED')
      .filter((p) => {
        const term = search.trim().toLowerCase()
        if (!term) return true
        return p?.name?.toLowerCase().includes(term) ||
          p?.description?.toLowerCase().includes(term)
      })

    // Ordenamiento
    filtered.sort((a, b) => {
      let valueA, valueB

      switch (sortBy) {
        case 'name':
          valueA = (a.name || '').toLowerCase()
          valueB = (b.name || '').toLowerCase()
          break
        case 'description':
          valueA = (a.description || '').toLowerCase()
          valueB = (b.description || '').toLowerCase()
          break
        case 'price':
          valueA = Number(a.price) || 0
          valueB = Number(b.price) || 0
          break
        case 'stock':
          valueA = Number(a.stock) || 0
          valueB = Number(b.stock) || 0
          break
        default:
          valueA = (a.name || '').toLowerCase()
          valueB = (b.name || '').toLowerCase()
      }

      if (sortOrder === 'asc') {
        return valueA > valueB ? 1 : valueA < valueB ? -1 : 0
      } else {
        return valueA < valueB ? 1 : valueA > valueB ? -1 : 0
      }
    })

    return filtered
  }

  const filteredProducts = getSortedProducts()

  const openModal = async (product = null) => {
    if (product) {
      const nextForm = {
        id: product.id ?? null,
        categoryId: product.categoryId != null ? String(product.categoryId) : '',
        name: product.name ?? '',
        description: product.description ?? '',
        price: Number.isFinite(Number(product.price)) ? Number(product.price).toFixed(2) : '',
        stock: Number.isFinite(Number(product.stock)) ? Number(product.stock) : 0,
        originalStock: Number.isFinite(Number(product.stock)) ? Number(product.stock) : 0,
        prepared: Boolean(product.prepared ?? true),
      }
      setForm(nextForm)
    } else {
      setForm({ ...emptyForm })
    }

    if (!Array.isArray(categories) || categories.length === 0) {
      try {
        const list = await getCategories({ active: true })
        if (Array.isArray(list) && list.length) {
          setCategories(list)
        } else {
          let fallback = null
          try {
            fallback = await getCategories()
          } catch (err) {
            console.error('Error loading all categories on modal open', err)
          }
          if (Array.isArray(fallback) && fallback.length) {
            setCategories(fallback)
          } else {
            const seeded = await seedCategories()
            if (Array.isArray(seeded) && seeded.length) {
              setCategories(seeded)
              success('Se crearon categorias por defecto')
            }
          }
        }
      } catch (e) {
        console.error('Error reloading categories on modal open', e)
      }
    }
    setModalOpen(true)
  }

  const closeModal = () => {
    setModalOpen(false)
    setForm({ ...emptyForm })
    setStructuresView(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    const trimmedName = String(form.name ?? '').trim()
    const parsedCategoryId = Number.parseInt(form.categoryId, 10)
    const parsedPrice = Number.parseFloat(form.price)
    const parsedStock = Number.parseInt(form.stock, 10)

    if (!trimmedName) {
      error('El nombre es obligatorio')
      return
    }
    if (!Number.isFinite(parsedCategoryId) || parsedCategoryId <= 0) {
      error('Selecciona una categoría válida')
      return
    }
    if (!Number.isFinite(parsedPrice) || parsedPrice < 0) {
      error('Ingresa un precio válido')
      return
    }
    if (!form.id && (!Number.isFinite(parsedStock) || parsedStock < 0)) {
      error('El stock inicial es inválido')
      return
    }

    setSaving(true)
    try {
      const payload = {
        categoryId: parsedCategoryId,
        name: trimmedName,
        description: String(form.description ?? '').trim(),
        price: parsedPrice,
        prepared: Boolean(form.prepared),
        stock: form.id ? undefined : Math.max(parsedStock, 0),
      }

      if (form.id) {
        await updateProduct(form.id, payload)
        const originalStockNumber = Number.isFinite(Number(form.originalStock))
          ? Number(form.originalStock)
          : 0
        const desiredStock = Number.isFinite(parsedStock) ? parsedStock : originalStockNumber
        if (Number.isFinite(desiredStock) && desiredStock !== originalStockNumber) {
          await updateStock(form.id, {
            quantity: Math.max(desiredStock, 0),
            type: 'ADJUST',
            reason: 'Ajuste manual',
          })
        }
        success('Producto actualizado')

        // Registrar en estructuras
        operationsQueueRef.current.enqueue({
          type: 'UPDATE',
          description: `Actualizado: ${trimmedName}`
        })
        actionsStackRef.current.push({
          action: 'UPDATE',
          productId: form.id,
          productName: trimmedName
        })
      } else {
        await createProduct(payload)
        success('Producto agregado')

        // Registrar en estructuras
        operationsQueueRef.current.enqueue({
          type: 'CREATE',
          description: `Creado: ${trimmedName}`
        })
        actionsStackRef.current.push({
          action: 'CREATE',
          productName: trimmedName
        })
      }

      forceUpdate({})
      closeModal()
      loadData()
    } catch (err) {
      console.error('Error saving product:', err)
      error(err?.response?.data?.message || err?.message || 'Error al guardar')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = (id) => {
    if (!id) return
    setConfirmId(id)
    setConfirmOpen(true)
  }

  const confirmDelete = async () => {
    const id = confirmId
    if (!id) {
      setConfirmOpen(false)
      return
    }

    const productToDelete = safeProducts.find(p => p.id === id)

    setConfirmOpen(false)
    setConfirmId(null)
    try {
      await deleteProduct(id)
      success('Producto eliminado')

      // Registrar en estructuras
      operationsQueueRef.current.enqueue({
        type: 'DELETE',
        description: `Eliminado: ${productToDelete?.name || 'Producto'}`
      })
      actionsStackRef.current.push({
        action: 'DELETE',
        productId: id,
        productName: productToDelete?.name
      })
      forceUpdate({})

      loadData()
    } catch (err) {
      console.error('Error deleting product:', err)
      error('Error al eliminar')
    }
  }

  useEffect(() => {
    const els = Object.values(iconRefs.current || {})
    if (els.length) {
      gsap.fromTo(els, { opacity: 0, y: -6 }, { opacity: 1, y: 0, duration: 0.4, stagger: 0.03, ease: 'power2.out' })
    }
  }, [products, search, sortBy, sortOrder])

  if (loading) return <Loader />

  const categoryTree = buildCategoryTree()

  return (
    <div className="p-6 space-y-6">
      <Toast toasts={toasts} onRemove={removeToast} />

      {/* Header */}
      <header className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-50">Inventario</h1>
          <p className="text-gray-600 dark:text-gray-400">Gestión de productos y stock</p>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setStructuresView(true)}
            className="btn-secondary"
          >
            <i className="bi bi-diagram-3"></i> Ver Estructuras
          </button>
          <button
            type="button"
            onClick={() => openModal()}
            className="btn-brand"
          >
            <i className="bi bi-plus-circle"></i> Nuevo Producto
          </button>
        </div>
      </header>

      {loadError && (
        <div className="card-brand border border-red-200 dark:border-red-700/60 bg-red-50 dark:bg-red-900/20">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <p className="text-sm text-red-700 dark:text-red-200">{loadError}</p>
            <button type="button" onClick={loadData} className="btn-secondary">
              Reintentar
            </button>
          </div>
        </div>
      )}

      {/* Filtros y Ordenamiento */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4">
        <div className="flex-1 relative">
          <i className="bi bi-search absolute left-3 top-3 text-gray-400"></i>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar por nombre o descripción..."
            className="w-full pl-10 input-brand"
          />
        </div>

        {/* Ordenamiento */}
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300 whitespace-nowrap">
            Ordenar por:
          </label>
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="input-brand min-w-[140px]"
          >
            <option value="name">Nombre</option>
            <option value="description">Descripción</option>
            <option value="price">Precio</option>
            <option value="stock">Stock</option>
          </select>
          <button
            type="button"
            onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
            className="p-2 rounded-lg border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-700"
            title={sortOrder === 'asc' ? 'Ascendente' : 'Descendente'}
          >
            <i className={`bi ${sortOrder === 'asc' ? 'bi-sort-up' : 'bi-sort-down'}`}></i>
          </button>
        </div>
      </div>

      {/* Tabla de Productos */}
      <div className="card-brand overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 dark:text-gray-100 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700" onClick={() => { setSortBy('name'); setSortOrder(sortBy === 'name' && sortOrder === 'asc' ? 'desc' : 'asc') }}>
                Nombre {sortBy === 'name' && <i className={`bi ${sortOrder === 'asc' ? 'bi-caret-up-fill' : 'bi-caret-down-fill'}`}></i>}
              </th>
              <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 dark:text-gray-100 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700" onClick={() => { setSortBy('description'); setSortOrder(sortBy === 'description' && sortOrder === 'asc' ? 'desc' : 'asc') }}>
                Descripción {sortBy === 'description' && <i className={`bi ${sortOrder === 'asc' ? 'bi-caret-up-fill' : 'bi-caret-down-fill'}`}></i>}
              </th>
              <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 dark:text-gray-100 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700" onClick={() => { setSortBy('price'); setSortOrder(sortBy === 'price' && sortOrder === 'asc' ? 'desc' : 'asc') }}>
                Precio {sortBy === 'price' && <i className={`bi ${sortOrder === 'asc' ? 'bi-caret-up-fill' : 'bi-caret-down-fill'}`}></i>}
              </th>
              <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 dark:text-gray-100 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700" onClick={() => { setSortBy('stock'); setSortOrder(sortBy === 'stock' && sortOrder === 'asc' ? 'desc' : 'asc') }}>
                Stock {sortBy === 'stock' && <i className={`bi ${sortOrder === 'asc' ? 'bi-caret-up-fill' : 'bi-caret-down-fill'}`}></i>}
              </th>
              <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 dark:text-gray-100">POS</th>
              <th className="px-4 py-3 text-right text-sm font-semibold text-gray-900 dark:text-gray-100">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {filteredProducts.length === 0 ? (
              <tr>
                <td colSpan="6" className="px-4 py-8 text-center text-gray-500 dark:text-gray-400">
                  <div className="flex flex-col items-center gap-2">
                    <i className="bi bi-inbox text-4xl text-gray-300"></i>
                    <p>No hay productos</p>
                    <button onClick={() => openModal()} className="text-brand hover:underline">
                      Agregar el primero
                    </button>
                  </div>
                </td>
              </tr>
            ) : (
              filteredProducts.map((product) => {
                const safePrice = Number.isFinite(Number(product?.price)) ? Number(product.price) : 0
                const safeStock = Number.isFinite(Number(product?.stock)) ? Number(product.stock) : 0
                return (
                  <tr key={product.id} className="border-b border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors">
                    <td className="px-4 py-3 text-sm font-medium text-gray-900 dark:text-gray-100">
                      <div className="flex items-center gap-2">
                        <i ref={(el) => { if (el) iconRefs.current[product.id] = el }} className={`bi ${iconForCategory(product.categoryName)} text-brand text-lg`}></i>
                        <span>{product.name}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400 max-w-xs truncate">{product.description || '-'}</td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-gray-100 font-mono">${safePrice.toFixed(2)}</td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`px-2 py-1 rounded text-xs font-medium ${safeStock <= 0 ? 'bg-red-100 text-red-700' :
                          safeStock <= 10 ? 'bg-yellow-100 text-yellow-700' :
                            'bg-green-100 text-green-700'
                        }`}>
                        {safeStock}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      {product.prepared ? (
                        <span className="px-2 py-1 bg-green-100 text-green-700 rounded text-xs">Sí</span>
                      ) : (
                        <span className="px-2 py-1 bg-gray-100 text-gray-700 rounded text-xs">No</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right space-x-2">
                      <button
                        type="button"
                        onClick={() => openModal(product)}
                        className="text-blue-600 hover:text-blue-800 dark:text-blue-400 p-1"
                        title="Editar"
                      >
                        <i className="bi bi-pencil"></i>
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDelete(product.id)}
                        className="text-red-600 hover:text-red-800 dark:text-red-400 p-1"
                        title="Eliminar"
                      >
                        <i className="bi bi-trash"></i>
                      </button>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Modal Centrado de Producto */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black bg-opacity-50" onClick={closeModal}></div>

          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-hidden animate-fade-in">
            {/* Header del Modal */}
            <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-brand/10 to-transparent">
              <div>
                <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">
                  {form.id ? 'Editar Producto' : 'Nuevo Producto'}
                </h2>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  {form.id ? 'Modifica los datos del producto' : 'Completa la información del nuevo producto'}
                </p>
              </div>
              <button
                onClick={closeModal}
                className="p-2 text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
              >
                <i className="bi bi-x-lg text-xl"></i>
              </button>
            </div>

            {/* Formulario */}
            <form onSubmit={handleSubmit} className="overflow-y-auto max-h-[60vh] p-6 space-y-4">
              <div className="grid grid-cols-1 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
                    Nombre <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    className="input-brand"
                    placeholder="Ej: Café Americano"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
                    Descripción
                  </label>
                  <textarea
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    className="input-brand"
                    rows="2"
                    placeholder="Descripción opcional del producto"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
                      Precio <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <span className="absolute left-3 top-2.5 text-gray-500">$</span>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        value={form.price}
                        onChange={(e) => setForm({ ...form, price: e.target.value })}
                        className="input-brand pl-7"
                        placeholder="0.00"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
                      Stock
                    </label>
                    <input
                      type="number"
                      min="0"
                      value={form.stock}
                      onChange={(e) => setForm({ ...form, stock: e.target.value })}
                      className="input-brand"
                      placeholder="0"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
                    Categoría <span className="text-red-500">*</span>
                  </label>
                  <select
                    value={form.categoryId}
                    onChange={(e) => {
                      const value = e.target.value
                      const selected = safeCategories.find((cat) => String(cat.id) === String(value))
                      const isIngredientes = selected?.name?.toLowerCase() === 'ingredientes'
                      setForm({ ...form, categoryId: value, prepared: isIngredientes ? false : form.prepared })
                    }}
                    className="input-brand"
                    required
                  >
                    <option value="">Seleccionar categoría...</option>
                    {safeCategories.map((cat) => (
                      <option key={cat.id} value={cat.id}>
                        {cat.name}
                      </option>
                    ))}
                  </select>
                  {safeCategories.length === 0 && (
                    <div className="mt-2 text-sm text-gray-600 dark:text-gray-400 flex items-center justify-between">
                      <span>No hay categorías.</span>
                      <button type="button" className="link-brand" onClick={loadData}>Recargar</button>
                    </div>
                  )}
                </div>

                <div className="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
                  <label className="flex items-center gap-3 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={form.prepared}
                      onChange={(e) => setForm({ ...form, prepared: e.target.checked })}
                      className="w-5 h-5 text-brand border-gray-300 rounded focus:ring-brand"
                    />
                    <div>
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        Mostrar en POS
                      </span>
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        El producto aparecerá en el punto de venta
                      </p>
                    </div>
                  </label>
                </div>
              </div>
            </form>

            {/* Footer del Modal */}
            <div className="flex gap-3 p-6 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
              <button
                type="button"
                onClick={closeModal}
                className="flex-1 btn-secondary"
                disabled={saving}
              >
                Cancelar
              </button>
              <button
                type="submit"
                onClick={handleSubmit}
                className="flex-1 btn-brand"
                disabled={saving}
              >
                {saving ? (
                  <span className="flex items-center justify-center gap-2">
                    <i className="bi bi-arrow-repeat animate-spin"></i>
                    Guardando...
                  </span>
                ) : (
                  <span className="flex items-center justify-center gap-2">
                    <i className="bi bi-check-lg"></i>
                    {form.id ? 'Actualizar' : 'Crear Producto'}
                  </span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Estructuras de Datos */}
      {structuresView && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black bg-opacity-50" onClick={() => setStructuresView(false)}></div>

          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden">
            <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
              <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">
                <i className="bi bi-diagram-3 mr-2"></i>
                Estructuras de Datos
              </h2>
              <button
                onClick={() => setStructuresView(false)}
                className="p-2 text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
              >
                <i className="bi bi-x-lg text-xl"></i>
              </button>
            </div>

            <div className="overflow-y-auto max-h-[70vh] p-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

                {/* Cola (Queue) */}
                <div className="bg-blue-50 dark:bg-blue-900/20 rounded-xl p-4 border-2 border-blue-200 dark:border-blue-800">
                  <h3 className="text-lg font-bold text-blue-800 dark:text-blue-300 mb-3 flex items-center gap-2">
                    <i className="bi bi-arrow-right-circle"></i>
                    Cola (Queue) - FIFO
                  </h3>
                  <p className="text-xs text-blue-600 dark:text-blue-400 mb-3">
                    Historial de operaciones (First In, First Out)
                  </p>
                  <div className="space-y-2 max-h-48 overflow-y-auto">
                    {operationsQueueRef.current.getAll().length === 0 ? (
                      <p className="text-sm text-gray-500 italic">Sin operaciones</p>
                    ) : (
                      operationsQueueRef.current.getAll().map((op, idx) => (
                        <div key={idx} className="bg-white dark:bg-gray-800 rounded-lg p-2 text-sm flex items-center gap-2">
                          <span className={`w-2 h-2 rounded-full ${op.type === 'CREATE' ? 'bg-green-500' :
                              op.type === 'UPDATE' ? 'bg-yellow-500' :
                                op.type === 'DELETE' ? 'bg-red-500' : 'bg-blue-500'
                            }`}></span>
                          <span className="flex-1 text-gray-700 dark:text-gray-300">{op.description}</span>
                          <span className="text-xs text-gray-400">{op.timestamp}</span>
                        </div>
                      ))
                    )}
                  </div>
                </div>

                {/* Pila (Stack) */}
                <div className="bg-purple-50 dark:bg-purple-900/20 rounded-xl p-4 border-2 border-purple-200 dark:border-purple-800">
                  <h3 className="text-lg font-bold text-purple-800 dark:text-purple-300 mb-3 flex items-center gap-2">
                    <i className="bi bi-stack"></i>
                    Pila (Stack) - LIFO
                  </h3>
                  <p className="text-xs text-purple-600 dark:text-purple-400 mb-3">
                    Últimas acciones (Last In, First Out)
                  </p>
                  <div className="space-y-2 max-h-48 overflow-y-auto">
                    {actionsStackRef.current.getAll().length === 0 ? (
                      <p className="text-sm text-gray-500 italic">Sin acciones</p>
                    ) : (
                      actionsStackRef.current.getAll().map((action, idx) => (
                        <div key={idx} className="bg-white dark:bg-gray-800 rounded-lg p-2 text-sm">
                          <div className="flex items-center gap-2">
                            <span className={`px-2 py-0.5 rounded text-xs font-medium ${action.action === 'CREATE' ? 'bg-green-100 text-green-700' :
                                action.action === 'UPDATE' ? 'bg-yellow-100 text-yellow-700' :
                                  'bg-red-100 text-red-700'
                              }`}>{action.action}</span>
                            <span className="text-gray-700 dark:text-gray-300">{action.productName}</span>
                          </div>
                          <span className="text-xs text-gray-400">{action.timestamp}</span>
                        </div>
                      ))
                    )}
                  </div>
                </div>

                {/* Árbol de Categorías */}
                <div className="bg-green-50 dark:bg-green-900/20 rounded-xl p-4 border-2 border-green-200 dark:border-green-800">
                  <h3 className="text-lg font-bold text-green-800 dark:text-green-300 mb-3 flex items-center gap-2">
                    <i className="bi bi-folder-tree"></i>
                    Árbol de Categorías
                  </h3>
                  <p className="text-xs text-green-600 dark:text-green-400 mb-3">
                    Estructura jerárquica de categorías
                  </p>
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                      <i className="bi bi-folder2 text-green-600"></i>
                      {categoryTree.name}
                    </div>
                    {categoryTree.children.map((child, idx) => (
                      <div key={idx} className="ml-6 flex items-center gap-2 text-sm bg-white dark:bg-gray-800 rounded p-2">
                        <i className="bi bi-folder text-green-500"></i>
                        <span className="text-gray-700 dark:text-gray-300">{child.name}</span>
                        <span className="ml-auto bg-green-100 dark:bg-green-800 text-green-700 dark:text-green-300 px-2 py-0.5 rounded-full text-xs">
                          {child.productCount} productos
                        </span>
                      </div>
                    ))}
                    {categoryTree.children.length === 0 && (
                      <p className="ml-6 text-sm text-gray-500 italic">Sin categorías</p>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      <ConfirmModal
        open={confirmOpen}
        title="Eliminar producto"
        message="¿Estás seguro de eliminar este producto? Esta acción no se puede deshacer."
        confirmText="Eliminar"
        cancelText="Cancelar"
        onConfirm={confirmDelete}
        onCancel={() => { setConfirmOpen(false); setConfirmId(null) }}
      />
    </div>
  )
}

export default InventarioPage


