import { useState, useEffect, useReducer, useRef } from 'react';
import { getProductos, createVenta } from '../services/apiService';
import { formatCurrency } from '../utils/formatters';
import { useToast } from '../hooks/useToast';
import Button from '../components/common/Button';
import Toast from '../components/common/Toast';
import gsap from 'gsap';

const cartReducer = (state, action) => {
  switch (action.type) {
    case 'ADD_ITEM':
      const existingIndex = state.findIndex(item => item.id === action.payload.id);
      if (existingIndex >= 0) {
        const newState = [...state];
        newState[existingIndex].cantidad += 1;
        return newState;
      }
      return [...state, { ...action.payload, cantidad: 1 }];
    
    case 'REMOVE_ITEM':
      return state.filter(item => item.id !== action.payload);
    
    case 'UPDATE_QUANTITY':
      return state.map(item =>
        item.id === action.payload.id
          ? { ...item, cantidad: Math.max(1, action.payload.cantidad) }
          : item
      );
    
    case 'CLEAR_CART':
      return [];
    
    default:
      return state;
  }
};

const POSPage = () => {
  const [productos, setProductos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [cart, dispatch] = useReducer(cartReducer, []);
  const [processingPayment, setProcessingPayment] = useState(false);
  
  const { toasts, success, error, removeToast } = useToast();
  const productsRef = useRef(null);

  useEffect(() => {
    loadProductos();
  }, [search]);

  useEffect(() => {
    const savedCart = localStorage.getItem('cart');
    if (savedCart) {
      try {
        const parsed = JSON.parse(savedCart);
        parsed.forEach(item => dispatch({ type: 'ADD_ITEM', payload: item }));
      } catch (e) {
        console.error('Error loading cart:', e);
      }
    }
  }, []);

  useEffect(() => {
    localStorage.setItem('cart', JSON.stringify(cart));
  }, [cart]);

  useEffect(() => {
    if (productsRef.current?.children) {
      gsap.fromTo(
        Array.from(productsRef.current.children),
        { opacity: 0, scale: 0.9 },
        { opacity: 1, scale: 1, stagger: 0.03, duration: 0.3, ease: 'back.out(1.2)' }
      );
    }
  }, [productos]);

  const loadProductos = async () => {
    try {
      setLoading(true);
      const data = await getProductos({ search, size: 100 });
      setProductos(Array.isArray(data) ? data : data.content || []);
    } catch (err) {
      error('Error al cargar productos');
      setProductos([]);
    } finally {
      setLoading(false);
    }
  };

  const addToCart = (producto) => {
    if (producto.stock <= 0) {
      error('Producto sin stock');
      return;
    }
    dispatch({ type: 'ADD_ITEM', payload: producto });
    success(`${producto.nombre} agregado al carrito`);
  };

  const removeFromCart = (id) => {
    dispatch({ type: 'REMOVE_ITEM', payload: id });
  };

  const updateQuantity = (id, cantidad) => {
    dispatch({ type: 'UPDATE_QUANTITY', payload: { id, cantidad } });
  };

  const calculateTotal = () => {
    return cart.reduce((sum, item) => sum + (item.precio * item.cantidad), 0);
  };

  const handleCheckout = async () => {
    if (cart.length === 0) {
      error('El carrito está vacío');
      return;
    }

    setProcessingPayment(true);

    try {
      const ventaPayload = {
        items: cart.map(item => ({
          productoId: item.id,
          cantidad: item.cantidad,
          precioUnitario: item.precio,
        })),
        total: calculateTotal(),
        fecha: new Date().toISOString(),
      };

      await createVenta(ventaPayload);
      success('Venta procesada exitosamente');
      dispatch({ type: 'CLEAR_CART' });
      printTicket();
    } catch (err) {
      error('Error al procesar la venta. Intenta nuevamente.');
    } finally {
      setProcessingPayment(false);
    }
  };

  const printTicket = () => {
    const ticketWindow = window.open('', '', 'width=300,height=600');
    const ticketContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>Ticket de Venta</title>
        <style>
          body { font-family: monospace; width: 80mm; margin: 0; padding: 10px; }
          h1 { text-align: center; font-size: 16px; margin: 10px 0; }
          .line { border-top: 1px dashed #000; margin: 10px 0; }
          .item { display: flex; justify-content: space-between; margin: 5px 0; }
          .total { font-weight: bold; font-size: 14px; }
        </style>
      </head>
      <body>
        <h1>UCACUE BAR</h1>
        <p style="text-align: center;">Ticket de Venta</p>
        <p style="text-align: center;">${new Date().toLocaleString()}</p>
        <div class="line"></div>
        ${cart.map(item => `
          <div class="item">
            <span>${item.nombre} x${item.cantidad}</span>
            <span>${formatCurrency(item.precio * item.cantidad)}</span>
          </div>
        `).join('')}
        <div class="line"></div>
        <div class="item total">
          <span>TOTAL</span>
          <span>${formatCurrency(calculateTotal())}</span>
        </div>
        <div class="line"></div>
        <p style="text-align: center; margin-top: 20px;">¡Gracias por su compra!</p>
      </body>
      </html>
    `;
    ticketWindow.document.write(ticketContent);
    ticketWindow.document.close();
    setTimeout(() => {
      ticketWindow.print();
      ticketWindow.close();
    }, 250);
  };

  return (
    <div className="h-full flex flex-col lg:flex-row gap-6 p-6">
      <Toast toasts={toasts} onRemove={removeToast} />

      <div className="flex-1 flex flex-col space-y-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Punto de Venta</h1>
          <p className="text-gray-600">Selecciona productos para agregar al carrito</p>
        </div>

        <div className="bg-white rounded-xl shadow-md p-4">
          <div className="relative">
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Buscar productos..."
              className="w-full pl-10 pr-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand"
            />
            <svg className="w-5 h-5 text-gray-400 absolute left-3 top-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto">
          {loading ? (
            <div className="flex items-center justify-center h-64">
              <div className="text-center">
                <div className="w-16 h-16 border-4 border-brand border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                <p className="text-gray-600">Cargando productos...</p>
              </div>
            </div>
          ) : (
            <div ref={productsRef} className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {productos.map((producto) => (
                <button
                  key={producto.id}
                  onClick={() => addToCart(producto)}
                  disabled={producto.stock <= 0}
                  className="bg-white rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-all hover:-translate-y-1 disabled:opacity-50 disabled:cursor-not-allowed text-left"
                >
                  <div className="h-32 bg-gray-200 relative">
                    {producto.imagenUrl ? (
                      <img src={producto.imagenUrl} alt={producto.nombre} className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center">
                        <svg className="w-12 h-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                      </div>
                    )}
                    {producto.stock <= 0 && (
                      <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                        <span className="text-white font-bold">Sin Stock</span>
                      </div>
                    )}
                  </div>
                  <div className="p-3">
                    <h3 className="font-bold text-gray-900 text-sm mb-1 truncate">{producto.nombre}</h3>
                    <p className="text-lg font-bold text-brand">{formatCurrency(producto.precio)}</p>
                    <p className="text-xs text-gray-500">Stock: {producto.stock}</p>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="lg:w-96 bg-white rounded-xl shadow-lg p-6 flex flex-col">
        <h2 className="text-2xl font-bold text-gray-900 mb-4">Carrito</h2>

        <div className="flex-1 overflow-y-auto space-y-3 mb-4">
          {cart.length === 0 ? (
            <div className="text-center py-12">
              <svg className="w-16 h-16 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              <p className="text-gray-500">Carrito vacío</p>
            </div>
          ) : (
            cart.map((item) => (
              <div key={item.id} className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
                <div className="flex-1">
                  <h3 className="font-semibold text-gray-900 text-sm">{item.nombre}</h3>
                  <p className="text-sm text-gray-600">{formatCurrency(item.precio)}</p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => updateQuantity(item.id, item.cantidad - 1)}
                    className="w-8 h-8 bg-gray-200 rounded-lg hover:bg-gray-300 flex items-center justify-center"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 12H4" />
                    </svg>
                  </button>
                  <span className="w-8 text-center font-semibold">{item.cantidad}</span>
                  <button
                    onClick={() => updateQuantity(item.id, item.cantidad + 1)}
                    className="w-8 h-8 bg-gray-200 rounded-lg hover:bg-gray-300 flex items-center justify-center"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                    </svg>
                  </button>
                </div>
                <button
                  onClick={() => removeFromCart(item.id)}
                  className="w-8 h-8 bg-red-100 text-red-600 rounded-lg hover:bg-red-200 flex items-center justify-center"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            ))
          )}
        </div>

        <div className="border-t pt-4 space-y-3">
          <div className="flex justify-between text-lg">
            <span className="font-semibold">Subtotal:</span>
            <span>{formatCurrency(calculateTotal())}</span>
          </div>
          <div className="flex justify-between text-2xl font-bold text-brand">
            <span>Total:</span>
            <span>{formatCurrency(calculateTotal())}</span>
          </div>

          <div className="space-y-2">
            <Button
              onClick={handleCheckout}
              disabled={cart.length === 0 || processingPayment}
              loading={processingPayment}
              className="w-full"
            >
              Procesar Venta
            </Button>
            <Button
              onClick={() => dispatch({ type: 'CLEAR_CART' })}
              variant="secondary"
              disabled={cart.length === 0}
              className="w-full"
            >
              Limpiar Carrito
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default POSPage;
