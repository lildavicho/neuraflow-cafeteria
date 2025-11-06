import { useCallback, useEffect, useRef, useState } from 'react'
import gsap from 'gsap'
import { getProducts, createProduct, updateProduct, deleteProduct } from '../services/products'
import { updateProductStock as updateStock } from '../services/apiService'
import { getCategories, seedCategories } from '../services/categories'
import { useInventoryUpdates } from '../hooks/useWebSocket'
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

const InventarioPage = () => {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  const { toasts, success, error, removeToast } = useToast()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmId, setConfirmId] = useState(null)
  const iconRefs = useRef({})

  const iconForCategory = (name = '') => {
    const n = String(name || '').toLowerCase()
    if (n.includes('bebi')) return 'bi-cup-hot'
    if (n.includes('ingre')) return 'bi-box-seam'
    if (n.includes('post') || n.includes('dulce')) return 'bi-cookie'
    if (n.includes('desa') || n.includes('plato') || n.includes('comida')) return 'bi-egg-fried'
    return 'bi-bag'
  }

  const loadData = useCallback(async () => {
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
          const seeded = await seedCategories()
          cats = Array.isArray(seeded) ? seeded : []
          if (cats.length) success('Se crearon categorías por defecto')
        } catch (e) {
          console.error('No se pudo sembrar categorías', e)
          // Mantener vacío, mostrará el aviso en el UI
        }
      }
      setCategories(cats)
    } catch (err) {
      console.error('Error loading inventory:', err)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleInventorySync = useCallback((data) => {
    if (data) {
      loadData()
    }
  }, [loadData])

  useInventoryUpdates(handleInventorySync)

  const safeProducts = Array.isArray(products) ? products : []
  const safeCategories = Array.isArray(categories) ? categories : []

  const filteredProducts = safeProducts
    .filter((p) => p?.status !== 'DISCONTINUED')
    .filter((p) => {
      const term = search.trim().toLowerCase()
      if (!term) return true
      return p?.name?.toLowerCase().includes(term)
    })

  const openDrawer = async (product = null) => {
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
    // Si no hay categorías cargadas intenta recargar/sembrar al abrir
    if (!Array.isArray(categories) || categories.length === 0) {
      try {
        const list = await getCategories({ active: true })
        if (Array.isArray(list) && list.length) {
          setCategories(list)
        } else {
          const seeded = await seedCategories()
          if (Array.isArray(seeded) && seeded.length) {
            setCategories(seeded)
            success('Se crearon categorías por defecto')
          }
        }
      } catch (e) {
        console.error('Error reloading categories on drawer open', e)
      }
    }
    setDrawerOpen(true)
  }

  const closeDrawer = () => {
    setDrawerOpen(false)
    setForm({ ...emptyForm })
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
      } else {
        await createProduct(payload)
        success('Producto agregado')
      }

      closeDrawer()
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
    setConfirmOpen(false)
    setConfirmId(null)
    try {
      await deleteProduct(id)
      success('Producto eliminado')
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
  }, [products, search])

  if (loading) return <Loader />

  return (
    <div className="p-6 space-y-6">
      <Toast toasts={toasts} onRemove={removeToast} />
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-50">Inventario</h1>
          <p className="text-gray-600 dark:text-gray-400">Gestión de productos y stock</p>
        </div>
        <button
          type="button"
          onClick={(event) => {
            event.preventDefault()
            event.stopPropagation()
            openDrawer()
          }}
          className="btn-brand"
        >
          <i className="bi bi-plus-circle"></i> Nuevo Producto
        </button>
      </header>

      <div className="flex items-center gap-4">
        <div className="flex-1 relative">
          <i className="bi bi-search absolute left-3 top-3 text-gray-400"></i>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar productos..."
            className="w-full pl-10 input-brand"
          />
        </div>
      </div>

      <div className="card-brand overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 dark:text-gray-100">Nombre</th>
              <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 dark:text-gray-100">Descripción</th>
              <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 dark:text-gray-100">Precio</th>
              <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 dark:text-gray-100">Stock</th>
              <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 dark:text-gray-100">POS</th>
              <th className="px-4 py-3 text-right text-sm font-semibold text-gray-900 dark:text-gray-100">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {filteredProducts.length === 0 ? (
              <tr>
                <td colSpan="6" className="px-4 py-8 text-center text-gray-500 dark:text-gray-400">
                  No hay productos
                </td>
              </tr>
            ) : (
              filteredProducts.map((product) => {
                const safePrice = Number.isFinite(Number(product?.price)) ? Number(product.price) : 0
                const safeStock = Number.isFinite(Number(product?.stock)) ? Number(product.stock) : 0
                return (
                  <tr key={product.id} className="border-b border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900 dark:text-gray-100">
                    <div className="flex items-center gap-2">
                      <i ref={(el) => { if (el) iconRefs.current[product.id] = el }} className={`bi ${iconForCategory(product.categoryName)} text-brand text-lg`}></i>
                      <span>{product.name}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{product.description || '-'}</td>
                  <td className="px-4 py-3 text-sm text-gray-900 dark:text-gray-100">${safePrice.toFixed(2)}</td>
                  <td className="px-4 py-3 text-sm text-gray-900 dark:text-gray-100">{safeStock}</td>
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
                      onClick={(event) => {
                        event.preventDefault()
                        event.stopPropagation()
                        openDrawer(product)
                      }}
                      className="text-blue-600 hover:text-blue-800 dark:text-blue-400"
                      title="Editar"
                    >
                      <i className="bi bi-pencil"></i>
                    </button>
                    <button
                      type="button"
                      onClick={(event) => {
                        event.preventDefault()
                        event.stopPropagation()
                        handleDelete(product.id)
                      }}
                      className="text-red-600 hover:text-red-800 dark:text-red-400"
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

      {drawerOpen && (
        <div className="fixed inset-0 z-50 overflow-hidden">
          <div className="absolute inset-0 bg-black bg-opacity-50" onClick={closeDrawer}></div>
          
          <div className="absolute right-0 top-0 h-full w-full max-w-md bg-white dark:bg-gray-800 shadow-xl flex flex-col">
            <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
              <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">
                {form.id ? 'Editar Producto' : 'Nuevo Producto'}
              </h2>
              <button onClick={closeDrawer} className="text-gray-500 hover:text-gray-700 dark:hover:text-gray-300">
                <i className="bi bi-x-lg text-xl"></i>
              </button>
            </div>

            <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
                  Nombre <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="input-brand"
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
                  rows="3"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
                  Precio <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  step="0.01"
                  value={form.price}
                  onChange={(e) => setForm({ ...form, price: e.target.value })}
                  className="input-brand"
                  required
                />
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
                />
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
                  <option value="">Seleccionar...</option>
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

              <div>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.prepared}
                    onChange={(e) => setForm({ ...form, prepared: e.target.checked })}
                    className="w-4 h-4 text-brand border-gray-300 rounded focus:ring-brand"
                  />
                  <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    Mostrar en POS
                  </span>
                </label>
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={closeDrawer}
                  className="flex-1 btn-secondary"
                  disabled={saving}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="flex-1 btn-brand"
                  disabled={saving}
                >
                  {saving ? 'Guardando...' : 'Guardar'}
                </button>
              </div>
            </form>
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
