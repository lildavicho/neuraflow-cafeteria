import { useEffect, useState } from 'react'
import useRealtimeSync from './useRealtimeSync'

const NOTIFICATIONS_TOPIC = '/topic/notifications'

const useNotificationsSync = () => {
  const { subscribe } = useRealtimeSync()
  const [notifications, setNotifications] = useState([])

  useEffect(() => {
    let mounted = true
    const setup = async () => {
      await subscribe(NOTIFICATIONS_TOPIC, (payload) => {
        if (!mounted) return
        const id = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2)
        setNotifications((prev) => [{ ...payload, id, receivedAt: new Date() }, ...prev].slice(0, 50))
      })
    }
    setup()
    return () => {
      mounted = false
    }
  }, [subscribe])

  const clearNotifications = () => setNotifications([])

  return { notifications, clearNotifications }
}

export default useNotificationsSync
