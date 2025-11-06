import { useEffect } from 'react'
import useRealtimeSync from './useRealtimeSync'

const DASHBOARD_TOPICS = ['/topic/dashboard', '/topic/sales', '/topic/people_counts']

const useDashboardSync = ({ onDashboard, onSales, onPeople } = {}) => {
  const { subscribe, isConnected } = useRealtimeSync()

  useEffect(() => {
    const unsubscribers = []

    const attach = async () => {
      if (onDashboard) {
        unsubscribers.push(await subscribe(DASHBOARD_TOPICS[0], onDashboard))
      }
      if (onSales) {
        unsubscribers.push(await subscribe(DASHBOARD_TOPICS[1], onSales))
      }
      if (onPeople) {
        unsubscribers.push(await subscribe(DASHBOARD_TOPICS[2], onPeople))
      }
    }

    attach()

    return () => {
      unsubscribers.forEach((fn) => fn?.())
    }
  }, [onDashboard, onPeople, onSales, subscribe])

  return { isConnected: isConnected() }
}

export default useDashboardSync
