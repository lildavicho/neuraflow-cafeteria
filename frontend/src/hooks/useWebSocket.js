import { useCallback, useEffect, useRef, useState } from 'react'
import wsClient from '../services/wsClient'

export const useWebSocket = () => {
  const [status, setStatus] = useState('disconnected')

  useEffect(() => {
    const unsubscribeStatus = wsClient.onStatusChange(setStatus)
    wsClient.connect().catch(console.error)

    return () => {
      unsubscribeStatus()
    }
  }, [])

  const subscribe = useCallback((topic, callback) => {
    return wsClient.subscribe(topic, callback)
  }, [])

  return { status, subscribe }
}

export const usePeopleCount = () => {
  const [currentCount, setCurrentCount] = useState(0)
  const [history, setHistory] = useState([])
  const { subscribe } = useWebSocket()

  useEffect(() => {
    const unsubscribe = subscribe('/topic/people_counts', (data) => {
      if (data.event === 'people.count') {
        const { count, timestamp } = data.payload
        setCurrentCount(count)
        setHistory(prev => {
          const newHistory = [...prev, { count, timestamp: new Date(timestamp) }]
          return newHistory.slice(-60)
        })
      }
    })

    return () => {
      if (unsubscribe) unsubscribe()
    }
  }, [subscribe])

  return { currentCount, history }
}

export const useInventoryUpdates = (onUpdate) => {
  const { subscribe } = useWebSocket()
  const callbackRef = useRef(onUpdate)

  useEffect(() => {
    callbackRef.current = onUpdate
  }, [onUpdate])

  useEffect(() => {
    const unsubscribe = subscribe('/topic/inventory', (data) => {
      // Forward any inventory message shape. Keep compat with older payloads.
      if (data?.entity === 'product' || data?.product || data?.type || typeof data === 'string') {
        callbackRef.current?.(data)
      }
    })

    return () => {
      if (unsubscribe) unsubscribe()
    }
  }, [subscribe])
}

export const useOrderNotifications = (onNotification) => {
  const { subscribe } = useWebSocket()
  const callbackRef = useRef(onNotification)

  useEffect(() => {
    callbackRef.current = onNotification
  }, [onNotification])

  useEffect(() => {
    const unsubscribeCreated = subscribe('/topic/orders/created', (data) => {
      if (data?.type === 'order.created') {
        callbackRef.current?.(data)
      }
    })

    const unsubscribeReady = subscribe('/topic/orders/ready', (data) => {
      if (data?.type === 'order.ready') {
        callbackRef.current?.(data)
      }
    })

    const unsubscribeAccepted = subscribe('/topic/orders/accepted', (data) => {
      if (data?.type === 'order.accepted') {
        callbackRef.current?.(data)
      }
    })

    const unsubscribeDelivered = subscribe('/topic/orders/delivered', (data) => {
      if (data?.type === 'order.delivered') {
        callbackRef.current?.(data)
      }
    })

    const unsubscribeCancelled = subscribe('/topic/orders/cancelled', (data) => {
      if (data?.type === 'order.cancelled') {
        callbackRef.current?.(data)
      }
    })

    const unsubscribeNotifications = subscribe('/topic/notifications', (evt) => {
      if (evt?.type) {
        callbackRef.current?.(evt)
      }
    })

    return () => {
      if (unsubscribeCreated) unsubscribeCreated()
      if (unsubscribeReady) unsubscribeReady()
      if (unsubscribeAccepted) unsubscribeAccepted()
      if (unsubscribeDelivered) unsubscribeDelivered()
      if (unsubscribeCancelled) unsubscribeCancelled()
      if (unsubscribeNotifications) unsubscribeNotifications()
    }
  }, [subscribe])
}
