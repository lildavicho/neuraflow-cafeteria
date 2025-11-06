import { useCallback, useEffect, useMemo, useState, useRef } from 'react'
import gsap from 'gsap'
import { useAuth } from '../hooks/useAuth'
import { fetchPublicProducts, createOrder, payOrder } from '../services/apiService'
import Loader from '../components/common/Loader'
import PurchaseCard from '../components/common/PurchaseCard'
import Receipt from '../components/common/Receipt'
import { useInventoryUpdates, useWebSocket } from '../hooks/useWebSocket'
import ModalCheckout from '../components/checkout/ModalCheckout'
import { useToast } from '../hooks/useToast'
import Toast from '../components/common/Toast'
import QuantityModal from '../components/common/QuantityModal'

const POSPage = () => {
  const { user } = useAuth()
  const [products, setProducts] = useState([])
  const [search, setSearch] = useState('')
  const [cart, setCart] = useState([])
  const [loading, setLoading] = useState(true)
  const [processing, setProcessing] = useState(false)
  const [showAnimation, setShowAnimation] = useState(false)
  const [showReceipt, setShowReceipt] = useState(false)
  const [currentOrder, setCurrentOrder] = useState(null)
  const [checkoutOpen, setCheckoutOpen] = useState(false)
  const { toasts, success, error, removeToast } = useToast()
  const cartRef = useRef(null)
  const [qtyOpen, setQtyOpen] = useState(false)
  const [qtyItem, setQtyItem] = useState(null)
  const iconRefs = useRef({})

  const iconForCategory = (name = '') => {
    const n = String(name || '').toLowerCase()
    if (n.includes('bebi')) return 'bi-cup-hot'
    if (n.includes('ingre')) return 'bi-box-seam'
    if (n.includes('post') || n.includes('dulce')) return 'bi-cookie'
    if (n.includes('desa') || n.includes('plato') || n.includes('comida')) return 'bi-egg-fried'
    return 'bi-bag'
  }

  useEffect(() => {
    const savedCart = localStorage.getItem('pos-cart')
    if (savedCart) {
      try {
        setCart(JSON.parse(savedCart))
      } catch (e) {
        console.error('Error loading cart:', e)
      }
    }
  }, [])

  useEffect(() => {
    localStorage.setItem('pos-cart', JSON.stringify(cart))
  }, [cart])

  useEffect(() => {
    const loadProducts = async () => {
      setLoading(true)
      try {
        const list = await fetchPublicProducts()
        setProducts(list || [])
      } catch (err) {
        console.error('Error loading products:', err)
      } finally {
        setLoading(false)
      }
    }
    loadProducts()
  }, [])

  const handleInventoryUpdate = useCallback((data) => {
    if (data.entity === 'product' && data.product) {
      setProducts((prev) =>
        prev.map((p) => (p.id === data.product.id ? { ...p, ...data.product } : p))
      )
    }
  }, [])

  useInventoryUpdates(handleInventoryUpdate)

  const { subscribe } = useWebSocket()
  useEffect(() => {
    const unsubscribe = subscribe('/topic/products', (evt) => {
      if (!evt) return
      // On any products change event, refresh the list for buyers
      ;(async () => {
        try {
          const list = await fetchPublicProducts()
          setProducts(list || [])
        } catch (e) {
          console.error('Error refreshing products:', e)
        }
      })()
    })
    return () => { if (unsubscribe) unsubscribe() }
  }, [subscribe])

  useEffect(() => {
    const els = Object.values(iconRefs.current || {})
    if (els.length) {
      gsap.fromTo(els, { opacity: 0, y: -6 }, { opacity: 1, y: 0, duration: 0.4, stagger: 0.03, ease: 'power2.out' })
    }
  }, [products, search])

  // Listen for payment confirmation events and reflect on current order
  useEffect(() => {
    const unsub = subscribe('/topic/notifications', (evt) => {
      if (evt?.type === 'ORDER_PAYMENT_UPDATED' && evt.order && currentOrder && evt.order.id === currentOrder.id) {
        setCurrentOrder(evt.order)
        if (evt.order.paymentStatus === 'PAID') {
          success('Pago confirmado')
        }
      }
    })
    return () => { if (unsub) unsub() }
  }, [subscribe, currentOrder, success])

  const filteredProducts = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return products
    return products.filter(
      (product) =>
        product.name?.toLowerCase().includes(term)
    )
  }, [products, search])

  const totals = useMemo(() => {
    const total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0)
    return { total }
  }, [cart])

  const canPay = useMemo(() => {
    if (cart.length === 0) return false
    return cart.every((it) => it.quantity > 0 && it.quantity <= (it.stock ?? Infinity))
  }, [cart])

  const addToCart = (product, event) => {
    if (product.stock <= 0) {
      error('Sin stock disponible')
      return
    }

    if (event && cartRef.current) {
      const rect = event.currentTarget.getBoundingClientRect()
      const cartRect = cartRef.current.getBoundingClientRect()
      
      const clone = event.currentTarget.cloneNode(true)
      clone.style.position = 'fixed'
      clone.style.left = rect.left + 'px'
      clone.style.top = rect.top + 'px'
      clone.style.width = rect.width + 'px'
      clone.style.zIndex = '9999'
      clone.style.pointerEvents = 'none'
      document.body.appendChild(clone)

      gsap.to(clone, {
        x: cartRect.left - rect.left,
        y: cartRect.top - rect.top,
        scale: 0.1,
        opacity: 0,
        duration: 0.6,
        ease: 'power2.in',
        onComplete: () => {
          clone.remove()
          const existing = cart.find(item => item.id === product.id)
          if (existing) {
            setCart(cart.map(item => 
              item.id === product.id 
                ? { ...item, quantity: Math.min(item.quantity + 1, product.stock) }
                : item
            ))
          } else {
            setCart([...cart, { ...product, quantity: 1 }])
          }
        }
      })
    } else {
      const existing = cart.find(item => item.id === product.id)
      if (existing) {
        setCart(cart.map(item => 
          item.id === product.id 
            ? { ...item, quantity: Math.min(item.quantity + 1, product.stock) }
            : item
        ))
      } else {
        setCart([...cart, { ...product, quantity: 1 }])
      }
    }
  }

  const updateQuantity = (id, quantity) => {
    setCart(cart.map(item => 
      item.id === id 
        ? { ...item, quantity: Math.min(Math.max(quantity, 1), item.stock) }
        : item
    ))
  }

  const removeItem = (id) => {
    setCart(cart.filter(item => item.id !== id))
  }

  const dec = (id) => {
    setCart((prev) => prev.flatMap((it) => {
      if (it.id !== id) return [it]
      const nextQty = it.quantity - 1
      if (nextQty <= 0) return []
      return [{ ...it, quantity: nextQty }]
    }))
  }

  const inc = (id) => {
    setCart((prev) => prev.map((it) => it.id === id ? { ...it, quantity: Math.min(it.quantity + 1, it.stock) } : it))
  }

  const openQtyModal = (item) => {
    setQtyItem(item)
    setQtyOpen(true)
  }

  const saveQtyModal = (value) => {
    if (!qtyItem) return
    updateQuantity(qtyItem.id, value)
    setQtyOpen(false)
    setQtyItem(null)
  }

  const checkout = async () => {
    if (cart.length === 0) {
      error('El carrito está vacío')
      return
    }
    setShowAnimation(true)
  }

  const handleConfirmCheckout = async ({ method, reference }) => {
    setCheckoutOpen(false)
    setProcessing(true)

    try {
      const payload = {
        items: cart.map((item) => ({
          productId: item.id,
          quantity: item.quantity,
          unitPrice: item.price,
        })),
        paymentMethod: method,
      }
      const createdOrder = await createOrder(payload)

      try {
        await payOrder(createdOrder.id, { method, reference })
      } catch (e) {
        console.error('Error registering payment:', e)
      }

      setCurrentOrder({
        ...createdOrder,
        createdAt: createdOrder.createdAt || new Date().toISOString(),
      })

      setTimeout(() => {
        setShowAnimation(false)
        setShowReceipt(true)
        setCart([])
        success('Pago registrado')
      }, 1200)
    } catch (err) {
      console.error('Error creating order:', err)
      setShowAnimation(false)
      error('No se pudo crear la orden')
    } finally {
      setProcessing(false)
    }
  }

  const cartTotalItems = cart.reduce((sum, item) => sum + item.quantity, 0)

  if (loading) return <Loader />

  return (
    <div className="grid grid-cols-1 xl:grid-cols-[2fr_1fr] gap-6 p-6">
      <Toast toasts={toasts} onRemove={removeToast} />

      <section className="space-y-4">
        <header className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
          <div>
            <h1 className="text-3xl font-semibold text-gray-900 dark:text-gray-50">Punto de venta</h1>
            <p className="text-gray-600 dark:text-gray-400">Agrega productos al carrito y crea órdenes instantáneamente</p>
          </div>
          <input
            type="text"
            value={search}
            onChange={(evt) => setSearch(evt.target.value)}
            placeholder="Buscar por nombre o código"
            className="w-full md:w-72 px-4 py-2 rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-brand"
          />
        </header>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredProducts.map((product) => (
            <article 
              key={product.id} 
              className="pos-product-card card-brand flex flex-col cursor-pointer hover:shadow-lg transition-all"
              onClick={(e) => addToCart(product, e)}
            >
              <div className="h-36 w-full rounded-t-lg mb-3 bg-gray-50 dark:bg-gray-800 flex items-center justify-center">
                <i ref={(el) => { if (el) iconRefs.current[product.id] = el }} className={`bi ${iconForCategory(product.categoryName)} text-brand text-5xl`}></i>
              </div>
              <div className="flex-1 flex flex-col gap-2">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{product.name}</h3>
                  <p className="text-sm text-gray-500 dark:text-gray-400">{product.description || 'Sin descripción'}</p>
                </div>
                <div className="flex items-center justify-between mt-auto">
                  <span className="text-xl font-bold text-brand">${Number(product.price ?? 0).toFixed(2)}</span>
                  <span className={`text-sm font-medium px-2 py-1 rounded ${product.stock > 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                    {product.stock > 0 ? `${product.stock} uds` : 'Agotado'}
                  </span>
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>

      <aside className="card-brand flex flex-col h-full">
        <header className="border-b border-gray-200 dark:border-gray-700 pb-3 mb-3">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Carrito</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400">{cartTotalItems} productos seleccionados</p>
        </header>

        <div ref={cartRef} className="flex-1 overflow-y-auto space-y-3 pr-1">
          {cart.length === 0 ? (
            <p className="text-gray-500 dark:text-gray-400 text-sm">Añade productos para comenzar.</p>
          ) : (
            cart.map((item) => (
              <div key={item.id} className="flex items-center gap-3 p-2 bg-gray-50 dark:bg-gray-800 rounded-lg">
                <div className="flex-1">
                  <p className="font-semibold text-gray-900 dark:text-gray-100">{item.name}</p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">${Number(item.price ?? 0).toFixed(2)}</p>
                </div>
                <div className="flex items-center gap-2">
                  <button className="px-2 py-1 rounded bg-gray-200 dark:bg-gray-700" onClick={() => dec(item.id)}>-</button>
                  <button className="px-2 py-1 rounded bg-gray-200 dark:bg-gray-700" onClick={() => inc(item.id)}>+</button>
                  <button className="px-2 py-1 rounded bg-gray-200 dark:bg-gray-700" onClick={() => openQtyModal(item)} title="Editar cantidad">
                    <i className="bi bi-pencil" />
                  </button>
                  <button 
                    onClick={() => removeItem(item.id)}
                    className="p-1 hover:bg-red-100 dark:hover:bg-red-900 text-red-600 rounded transition-colors"
                  >
                    <i className="bi bi-trash"></i>
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        <footer className="pt-4 mt-4 border-t border-gray-200 dark:border-gray-700 space-y-3">
          <div className="flex items-center justify-between text-2xl font-bold">
            <span className="text-gray-900 dark:text-gray-100">Total</span>
            <span className="text-brand">${totals.total.toFixed(2)}</span>
          </div>
          <button
            onClick={checkout}
            disabled={!canPay || processing}
            className="w-full btn-brand disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {processing ? 'Procesando...' : 'Pagar'}
          </button>
          {!canPay && cart.length > 0 && (
            <p className="text-sm text-red-600">Ajusta cantidades: superan el stock disponible.</p>
          )}
        </footer>
      </aside>

      {showAnimation && (
        <PurchaseCard onComplete={() => { setShowAnimation(false); setCheckoutOpen(true) }} />
      )}

      {showReceipt && currentOrder && (
        <Receipt 
          order={currentOrder} 
          onClose={() => {
            setShowReceipt(false)
            setCurrentOrder(null)
          }} 
        />
      )}

      <ModalCheckout
        open={checkoutOpen}
        onClose={() => setCheckoutOpen(false)}
        onConfirm={handleConfirmCheckout}
        total={totals.total}
        items={cart}
      />

      <QuantityModal
        open={qtyOpen}
        value={qtyItem?.quantity || 1}
        max={qtyItem?.stock || 999}
        onCancel={() => { setQtyOpen(false); setQtyItem(null) }}
        onSave={saveQtyModal}
      />
    </div>
  )
}

export default POSPage
