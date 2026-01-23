import { useCallback, useMemo, useState } from 'react'
import Button from '../components/common/Button'
import Toast from '../components/common/Toast'
import { useToast } from '../hooks/useToast'
import { useLanguage } from '../hooks/useLanguage'
import { useOrderNotifications } from '../hooks/useWebSocket'
import { formatDateTime } from '../utils/formatters'

const NotificacionesPage = () => {
  const { language } = useLanguage()
  const [notifications, setNotifications] = useState([])
  const [readIds, setReadIds] = useState(() => new Set())
  const { toasts, info, removeToast } = useToast()

  const handleNotification = useCallback(
    (event) => {
      if (!event) return

      const entry = mapEventToNotification(event)
      setNotifications((prev) => {
        const deduped = prev.filter((item) => item.id !== entry.id)
        return [entry, ...deduped].slice(0, 50)
      })
      setReadIds((prev) => {
        const next = new Set(prev)
        next.delete(entry.id)
        return next
      })
      if (entry.toastMessage) {
        info(entry.toastMessage)
      }
    },
    [info]
  )

  useOrderNotifications(handleNotification)

  const unread = useMemo(
    () => notifications.filter((notification) => !readIds.has(notification.id)),
    [notifications, readIds]
  )

  const markAsRead = useCallback((id) => {
    setReadIds((prev) => {
      const next = new Set(prev)
      next.add(id)
      return next
    })
  }, [])

  const markAll = useCallback(() => {
    setReadIds(new Set(notifications.map((notification) => notification.id)))
  }, [notifications])

  const clearNotifications = useCallback(() => {
    setNotifications([])
    setReadIds(new Set())
  }, [])

  return (
    <div className="p-6 space-y-6 max-w-4xl mx-auto">
      <Toast toasts={toasts} onRemove={removeToast} />

      <header className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold text-gray-900 dark:text-gray-50">Notificaciones</h1>
          <p className="text-gray-600 dark:text-gray-400">
            {unread.length > 0 ? `Tienes ${unread.length} alertas sin leer` : 'Todas las alertas están al día'}
          </p>
        </div>
        <div className="flex gap-2">
          {notifications.length > 0 && (
            <Button variant="ghost" onClick={clearNotifications}>
              Vaciar
            </Button>
          )}
          {unread.length > 0 && (
            <Button variant="secondary" onClick={markAll}>
              Marcar todas como leídas
            </Button>
          )}
        </div>
      </header>

      <section className="space-y-3">
        {notifications.length === 0 ? (
          <div className="card-brand text-center py-12">
            <p className="text-gray-600 dark:text-gray-400">Aún no hay notificaciones en esta sesión</p>
          </div>
        ) : (
          notifications.map((notification) => {
            const isRead = readIds.has(notification.id)
            return (
              <article
                key={notification.id}
                className={`card-brand transition-all ${isRead ? 'opacity-60' : 'border-l-4 border-brand'}`}
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                      {notification.title || 'Actualización'}
                    </h3>
                    <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">{notification.message}</p>
                  </div>
                  <div className="text-right text-xs text-gray-500 dark:text-gray-400">
                    {formatDateTime(notification.receivedAt, language)}
                  </div>
                </div>
                {!isRead && (
                  <div className="mt-3 flex items-center gap-2">
                    <Button variant="ghost" size="sm" onClick={() => markAsRead(notification.id)}>
                      Marcar como leída
                    </Button>
                  </div>
                )}
              </article>
            )
          })
        )}
      </section>
    </div>
  )
}

const mapEventToNotification = (event = {}) => {
  const { type = 'order.event', payload = {}, timestamp, order, orderId, status } = event
  const effectiveStatus = payload.status || status || order?.paymentStatus || order?.status || type
  const normalizedStatus = String(effectiveStatus || '')
  const code = payload.orderCode || payload.code || order?.code
  const idSource = payload.orderId || payload.id || order?.id || orderId

  let title = payload.title
  let message = payload.message

  if (!title) {
    if (type === 'ORDER_PAYMENT_UPDATED' || normalizedStatus.includes('PAYMENT')) {
      title = 'Pago actualizado'
    } else if (normalizedStatus.toLowerCase().includes('created')) {
      title = 'Nuevo pedido'
    } else if (normalizedStatus.toLowerCase().includes('ready')) {
      title = 'Pedido listo'
    } else {
      title = 'Actualización'
    }
  }

  if (!message) {
    const statusLabel = formatStatus(normalizedStatus)
    const ref = idSource || code
    const eta = payload.estimatedMinutes ?? event.estimatedMinutes
    const pickup = payload.pickupLocation ?? event.pickupLocation
    let extra = ''
    if (eta) extra += ` | ETA ${eta} min`
    if (pickup) extra += ` | ${pickup}`
    message = ref ? `Orden ${ref}: ${statusLabel}${extra}` : `Pedido ${statusLabel}${extra}`
  }

  const receivedAt = payload.timestamp || timestamp || Date.now()

  return {
    id: payload.notificationId || `${type}-${idSource || receivedAt}`,
    title,
    message,
    receivedAt,
    toastMessage: payload.toastMessage || title,
  }
}

const formatStatus = (status = '') =>
  status
    .toString()
    .split('.')
    .pop()
    .replace(/[_-]/g, ' ')
    .replace(/\b\w/g, (char) => char.toUpperCase())

export default NotificacionesPage
