import { useCallback, useEffect, useState } from 'react'
import { acceptOrder, cancelOrder, deliverOrder, fetchOrders, readyOrder } from '../services/apiService'
import useRealtimeSync from './useRealtimeSync'

const SALES_TOPIC = '/topic/sales'

const useAdminOrdersSync = ({ statusFilter } = {}) => {
  const { subscribe } = useRealtimeSync()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(false)

  const loadOrders = useCallback(async () => {
    setLoading(true)
    try {
      const response = await fetchOrders({ status: statusFilter, size: 50 })
      const content = Array.isArray(response?.content) ? response.content : response
      setOrders(content ?? [])
    } catch (error) {
      console.error('Error cargando órdenes', error)
    } finally {
      setLoading(false)
    }
  }, [statusFilter])

  useEffect(() => {
    loadOrders()
  }, [loadOrders])

  useEffect(() => {
    const init = async () => {
      await subscribe(SALES_TOPIC, (payload) => {
        const order = payload?.order
        if (!order?.id) return
        setOrders((prev) => {
          const map = new Map(prev.map((o) => [o.id, o]))
          map.set(order.id, order)
          return Array.from(map.values()).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
        })
      })
    }
    init()
  }, [subscribe])

  const transition = async (id, action, meta) => {
    switch (action) {
      case 'accept':
        await acceptOrder(id)
        break
      case 'ready':
        await readyOrder(id)
        break
      case 'deliver':
        await deliverOrder(id, meta?.paymentReference)
        break
      case 'cancel':
        await cancelOrder(id, meta?.reason)
        break
      default:
        break
    }
    await loadOrders()
  }

  return { orders, loading, reload: loadOrders, transition }
}

export default useAdminOrdersSync
