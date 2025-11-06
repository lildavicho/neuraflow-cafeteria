import { useCallback, useEffect, useRef } from 'react'
import { isConnected, publish, subscribe } from '../services/websocket'

export const useRealtimeSync = () => {
  const subscriptionsRef = useRef([])

  useEffect(() => {
    return () => {
      subscriptionsRef.current.forEach((unsubscribe) => unsubscribe?.())
      subscriptionsRef.current = []
    }
  }, [])

  const subscribeTopic = useCallback(async (topic, handler, headers) => {
    const unsubscribe = await subscribe(topic, handler, headers)
    subscriptionsRef.current.push(unsubscribe)
    return () => {
      unsubscribe?.()
      subscriptionsRef.current = subscriptionsRef.current.filter((fn) => fn !== unsubscribe)
    }
  }, [])

  return {
    subscribe: subscribeTopic,
    publish,
    isConnected,
  }
}

export default useRealtimeSync
