import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js'
import { Line } from 'react-chartjs-2'
import gsap from 'gsap'
import StatCard from '../components/common/StatCard'
import Loader from '../components/common/Loader'
import { usePeopleCount, useWebSocket } from '../hooks/useWebSocket'
import { fetchOrders, confirmOrderPayment, fetchPublicProducts, fetchDashboardSnapshot } from '../services/apiService'
import { useToast } from '../hooks/useToast'
import Toast from '../components/common/Toast'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

const DashboardPage = () => {
  const [loading, setLoading] = useState(true)
  const [pendingTransfers, setPendingTransfers] = useState([])
  const { toasts, success, error, removeToast } = useToast()
  const [stats, setStats] = useState({ sales: 0, orders: 0, products: 0, avgTicket: 0 })
  const { currentCount, history } = usePeopleCount()
  const { status, subscribe } = useWebSocket()
  const headerRef = useRef(null)
  const cardsRef = useRef(null)

  useEffect(() => {
    if (headerRef.current) {
      gsap.fromTo(
        headerRef.current,
        { opacity: 0, y: -20 },
        { opacity: 1, y: 0, duration: 0.6, ease: 'power2.out' }
      )
    }

    if (cardsRef.current) {
      gsap.fromTo(
        cardsRef.current.children,
        { opacity: 0, y: 30 },
        { opacity: 1, y: 0, duration: 0.4, stagger: 0.1, delay: 0.2, ease: 'power2.out' }
      )
    }

  }, [])

  const loadSnapshot = useCallback(async () => {
    try {
      const snapshot = await fetchDashboardSnapshot()
      setStats((prev) => ({
        ...prev,
        sales: Number(snapshot?.ventasHoy ?? snapshot?.salesToday ?? 0),
        orders: Number(snapshot?.ordenesHoy ?? snapshot?.ordersToday ?? 0),
        avgTicket: Number(snapshot?.ticketPromedio ?? snapshot?.avgTicket ?? snapshot?.averageTicket ?? 0),
      }))
    } catch (e) {
      console.error('Error loading dashboard snapshot', e)
    }
  }, [])

  const loadProductCount = useCallback(async () => {
    try {
      const list = await fetchPublicProducts()
      const count = Array.isArray(list) ? list.length : (Array.isArray(list?.content) ? list.content.length : 0)
      setStats((prev) => ({ ...prev, products: count }))
    } catch (e) {
      console.error('Error loading product count', e)
    }
  }, [])

  const loadPendingTransfers = useCallback(async () => {
    try {
      const page = await fetchOrders({ page: 0, size: 50 })
      const content = Array.isArray(page?.content) ? page.content : (Array.isArray(page) ? page : [])
      const filtered = content.filter((o) => o.paymentStatus === 'PAYMENT_PENDING_CONF')
      setPendingTransfers(filtered)
    } catch (e) {
      console.error('Error loading pending transfers', e)
    }
  }, [])

  useEffect(() => {
    const run = async () => {
      setLoading(true)
      try {
        await Promise.all([loadSnapshot(), loadProductCount(), loadPendingTransfers()])
      } finally {
        setLoading(false)
      }
    }
    run()
  }, [loadSnapshot, loadProductCount, loadPendingTransfers])

  useEffect(() => {
    const unsubDashboard = subscribe('/topic/dashboard', (payload) => {
      if (!payload) return
      setStats((prev) => ({
        ...prev,
        sales: Number(payload.ventasHoy ?? payload.salesToday ?? prev.sales ?? 0),
        orders: Number(payload.ordenesHoy ?? payload.ordersToday ?? prev.orders ?? 0),
        avgTicket: Number(payload.ticketPromedio ?? payload.avgTicket ?? payload.averageTicket ?? prev.avgTicket ?? 0),
      }))
    })

    const unsubSales = subscribe('/topic/sales', (evt) => {
      if (!evt) return
      const order = evt.order
      if (!order) return
      setPendingTransfers((prev) => {
        const map = new Map(prev.map((o) => [o.id, o]))
        map.set(order.id, order)
        return Array.from(map.values()).filter((o) => o.paymentStatus === 'PAYMENT_PENDING_CONF')
      })
    })

    const unsubInventory = subscribe('/topic/inventory', () => { loadProductCount() })
    const unsubProducts = subscribe('/topic/products', () => { loadProductCount() })

    return () => {
      unsubDashboard?.()
      unsubSales?.()
      unsubInventory?.()
      unsubProducts?.()
    }
  }, [subscribe, loadProductCount])

  const handleConfirmPayment = async (id) => {
    try {
      await confirmOrderPayment(id)
      success('Pago confirmado')
      setPendingTransfers((prev) => prev.filter((o) => o.id !== id))
    } catch (e) {
      console.error(e)
      error('No se pudo confirmar el pago')
    }
  }

  const peopleChartData = useMemo(() => {
    if (history.length === 0) {
      return {
        labels: [],
        datasets: [{
          label: 'Personas',
          data: [],
          fill: true,
          borderColor: '#e11d2e',
          backgroundColor: 'rgba(225, 29, 46, 0.1)',
          tension: 0.4,
        }]
      }
    }

    const labels = history.map((point, index) => `${index}s`)
    const data = history.map((point) => point.count)

    return {
      labels,
      datasets: [{
        label: 'Personas',
        data,
        fill: true,
        borderColor: '#e11d2e',
        backgroundColor: 'rgba(225, 29, 46, 0.1)',
        tension: 0.4,
      }]
    }
  }, [history])

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: (context) => `${context.parsed.y} personas`,
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          color: '#9ca3af'
        }
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(148, 163, 184, 0.15)' },
        ticks: {
          color: '#9ca3af'
        }
      },
    },
  }

  if (loading) {
    return <Loader />
  }

  return (
    <div className="space-y-6 p-6">
      <Toast toasts={toasts} onRemove={removeToast} />
      <header ref={headerRef} className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-50">Panel – Cafetería</h1>
        <p className="text-gray-600 dark:text-gray-400">Sistema de gestión y punto de venta</p>
      </header>

      <div ref={cardsRef} className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          title="Ventas Hoy"
          value={`$${Number(stats.sales || 0).toFixed(2)}`}
          change="Actualizado"
          icon="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          color="green"
          index={0}
        />
        <StatCard
          title="Órdenes Hoy"
          value={String(Number(stats.orders || 0))}
          change="Confirmadas"
          icon="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"
          color="blue"
          index={1}
        />
        <StatCard
          title="Productos"
          value={String(stats.products)}
          change="En inventario"
          icon="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
          color="purple"
          index={2}
        />
        <StatCard
          title="Ticket Promedio"
          value={`$${Number(stats.avgTicket || 0).toFixed(2)}`}
          change="Por venta"
          icon="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"
          color="orange"
          index={3}
        />
      </div>

      <div className="card-brand">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Afluencia en Tiempo Real</h3>
            <p className="text-3xl font-bold text-brand mt-2">Afluencia ahora: {currentCount} personas</p>
          </div>
          <div className="flex items-center gap-2">
            {status === 'connected' && (
              <span className="flex items-center gap-2 text-sm text-success-600 dark:text-success-400">
                <i className="bi bi-wifi"></i> Conectado
              </span>
            )}
            {status === 'reconnecting' && (
              <span className="flex items-center gap-2 text-sm text-warning-600 dark:text-warning-400">
                <i className="bi bi-arrow-repeat animate-spin"></i> Reconectando
              </span>
            )}
            {status === 'disconnected' && (
              <span className="flex items-center gap-2 text-sm text-danger-600 dark:text-danger-400">
                <i className="bi bi-wifi-off"></i> Desconectado
              </span>
            )}
          </div>
        </div>
        <div className="h-80">
          {history.length > 0 ? (
            <Line data={peopleChartData} options={chartOptions} />
          ) : (
            <div className="flex items-center justify-center h-full">
              <div className="text-center">
                <i className="bi bi-graph-up text-4xl text-gray-400 mb-2"></i>
                <p className="text-gray-500 dark:text-gray-400">Esperando datos en tiempo real...</p>
                <button 
                  onClick={() => globalThis.location.reload()} 
                  className="mt-4 btn-brand text-sm"
                >
                  Reintentar
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="card-brand">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Pagos por Transferencia Pendientes de Confirmación</h3>
        </div>
        {pendingTransfers.length === 0 ? (
          <p className="text-sm text-gray-600 dark:text-gray-400">No hay pagos pendientes</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left border-b border-gray-200 dark:border-gray-700">
                  <th className="py-2">Orden</th>
                  <th className="py-2">Cliente</th>
                  <th className="py-2">Total</th>
                  <th className="py-2">Referencia</th>
                  <th className="py-2 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {pendingTransfers.map((o) => (
                  <tr key={o.id} className="border-b border-gray-100 dark:border-gray-800">
                    <td className="py-2">#{o.id}</td>
                    <td className="py-2">{o.userName || o.userEmail || '-'}</td>
                    <td className="py-2 font-medium">${Number(o.total || 0).toFixed(2)}</td>
                    <td className="py-2">{o.paymentReference || '-'}</td>
                    <td className="py-2 text-right">
                      <button onClick={() => handleConfirmPayment(o.id)} className="btn-brand btn-sm">Confirmar pago</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card-brand">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Ventas en Vivo</h3>
            <p className="text-3xl font-bold text-green-600 dark:text-green-400 mt-2">
              Total hoy: ${Number(stats.sales || 0).toFixed(2)}
            </p>
          </div>
          <div className="flex items-center gap-2">
            {status === 'connected' && (
              <span className="flex items-center gap-2 text-sm text-success-600 dark:text-success-400">
                <i className="bi bi-wifi"></i> Conectado
              </span>
            )}
            {status === 'reconnecting' && (
              <span className="flex items-center gap-2 text-sm text-warning-600 dark:text-warning-400">
                <i className="bi bi-arrow-repeat animate-spin"></i> Reconectando
              </span>
            )}
            {status === 'disconnected' && (
              <span className="flex items-center gap-2 text-sm text-danger-600 dark:text-danger-400">
                <i className="bi bi-wifi-off"></i> Desconectado
              </span>
            )}
          </div>
        </div>
        <div className="h-80">
          <div className="flex items-center justify-center h-full">
            <div className="text-center">
              <i className="bi bi-graph-up text-4xl text-gray-400 mb-2"></i>
              <p className="text-gray-500 dark:text-gray-400">Esperando datos de ventas...</p>
              <button 
                onClick={() => globalThis.location.reload()} 
                className="mt-4 btn-brand text-sm"
              >
                Reintentar
              </button>
            </div>
          </div>
        </div>
      </div>

      <section className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Link to="/pos" className="card-brand hover:-translate-y-1 transition-transform">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-lg bg-brand text-white flex items-center justify-center">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 14l2 2 4-4M7 7h10m1 9a2 2 0 11-4 0 2 2 0 014 0zM7 16a2 2 0 114 0 2 2 0 01-4 0z" />
              </svg>
            </div>
            <div>
              <h4 className="text-gray-900 dark:text-gray-100 font-semibold">Punto de Venta</h4>
              <p className="text-sm text-gray-600 dark:text-gray-400">Flujo ágil para ventas en vivo</p>
            </div>
          </div>
        </Link>

        <Link to="/inventario" className="card-brand hover:-translate-y-1 transition-transform">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-lg bg-blue-500/10 text-blue-500 flex items-center justify-center">
              <i className="bi bi-box-seam text-xl"></i>
            </div>
            <div>
              <h4 className="text-gray-900 dark:text-gray-100 font-semibold">Inventario</h4>
              <p className="text-sm text-gray-600 dark:text-gray-400">Stock sincronizado en tiempo real</p>
            </div>
          </div>
        </Link>

        <Link to="/reportes" className="card-brand hover:-translate-y-1 transition-transform">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-lg bg-green-500/10 text-green-500 flex items-center justify-center">
              <i className="bi bi-graph-up text-xl"></i>
            </div>
            <div>
              <h4 className="text-gray-900 dark:text-gray-100 font-semibold">Reportes</h4>
              <p className="text-sm text-gray-600 dark:text-gray-400">Analítica histórica y exportes</p>
            </div>
          </div>
        </Link>
      </section>
    </div>
  )
}

export default DashboardPage
