import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getIdToken } from './authService'
import { API_BASE_URL } from './apiService'

const stripTrailingSlash = (value) => value.replace(/\/+$/, '')

// Resolve full SockJS endpoint URL
export const resolveWsEndpoint = () => {
  const explicitWs = import.meta.env.VITE_WS_URL?.trim()
  if (explicitWs) {
    // If user provided full endpoint (e.g. http://host:8090/ws or /ws), use it as-is
    const val = stripTrailingSlash(explicitWs)
    if (/\/ws$/i.test(val)) return val
    // If only origin/base was provided, append /ws
    return `${stripTrailingSlash(val)}/api/ws`
  }

  const explicitApiBase = import.meta.env.VITE_API_BASE_URL?.trim()
  if (explicitApiBase) {
    return `${stripTrailingSlash(explicitApiBase)}/ws`
  }

  if (API_BASE_URL) {
    return `${stripTrailingSlash(API_BASE_URL)}/ws`
  }

  if (typeof window !== 'undefined' && window.location?.origin) {
    const { protocol, hostname, port } = window.location
    const devPorts = new Set(['5173', '5174', '4173', '4174'])
    const desiredPort = devPorts.has(port)
      ? (import.meta.env.VITE_WS_DEV_PORT?.trim() || import.meta.env.VITE_API_PORT?.trim() || '80')
      : port
    const effectiveOrigin = desiredPort
      ? `${protocol}//${hostname}${desiredPort === port ? `:${port}` : `:${desiredPort}`}`
      : `${protocol}//${hostname}`
    return `${stripTrailingSlash(effectiveOrigin)}/api/ws`
  }

  return 'http://localhost/api/ws'
}

class WSClient {
  constructor() {
    this.client = null
    this.connected = false
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 10
    this.reconnectDelay = 1000
    this.subscriptions = new Map()
    this.pendingSubscriptions = []
    this.connectionPromise = null
    this.statusCallbacks = []
  }

  connect() {
    if (this.connectionPromise) {
      return this.connectionPromise
    }

    this.connectionPromise = new Promise((resolve, reject) => {
      const wsEndpoint = resolveWsEndpoint()
      
      this.client = new Client({
        webSocketFactory: () => new SockJS(wsEndpoint),
        reconnectDelay: this.calculateReconnectDelay(),
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        beforeConnect: async () => {
          const token = await getIdToken()
          if (token) {
            this.client.connectHeaders = {
              ...(this.client.connectHeaders || {}),
              Authorization: `Bearer ${token}`,
            }
          }
        },
        debug: (str) => {
          if (import.meta.env.DEV) {
            console.log('[WS]', str)
          }
        },
        onConnect: () => {
          this.connected = true
          this.reconnectAttempts = 0
          this.notifyStatus('connected')
          
          this.pendingSubscriptions.forEach(({ topic, callback }) => {
            this.doSubscribe(topic, callback)
          })
          this.pendingSubscriptions = []
          
          resolve()
        },
        onDisconnect: () => {
          this.connected = false
          this.notifyStatus('disconnected')
        },
        onStompError: (frame) => {
          console.error('[WS] STOMP error:', frame.headers['message'])
          this.notifyStatus('error')
          reject(new Error(frame.headers['message']))
        },
        onWebSocketError: (error) => {
          console.error('[WS] WebSocket error:', error)
          this.notifyStatus('error')
          if (!this.connected) {
            reject(error)
          }
        },
        onWebSocketClose: () => {
          this.connected = false
          this.subscriptions.clear()
          this.notifyStatus('reconnecting')
          
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++
            setTimeout(() => {
              this.connectionPromise = null
              this.connect()
            }, this.calculateReconnectDelay())
          }
        }
      })

      this.client.activate()
    })

    return this.connectionPromise
  }

  calculateReconnectDelay() {
    return Math.min(this.reconnectDelay * Math.pow(2, this.reconnectAttempts), 30000)
  }

  doSubscribe(topic, callback) {
    if (!this.client || !this.connected) {
      return null
    }

    const subscription = this.client.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch {
        callback(message.body)
      }
    })

    this.subscriptions.set(topic, subscription)
    return subscription
  }

  subscribe(topic, callback) {
    if (!this.connected) {
      this.pendingSubscriptions.push({ topic, callback })
      this.connect().catch(() => {})
      return () => {
        const index = this.pendingSubscriptions.findIndex(sub => sub.topic === topic && sub.callback === callback)
        if (index !== -1) {
          this.pendingSubscriptions.splice(index, 1)
        }
      }
    }

    this.doSubscribe(topic, callback)

    return () => {
      const subscription = this.subscriptions.get(topic)
      if (subscription) {
        subscription.unsubscribe()
        this.subscriptions.delete(topic)
      }
    }
  }

  unsubscribe(topic) {
    const subscription = this.subscriptions.get(topic)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(topic)
    }
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate()
      this.client = null
      this.connected = false
      this.subscriptions.clear()
      this.pendingSubscriptions = []
      this.connectionPromise = null
      this.notifyStatus('disconnected')
    }
  }

  onStatusChange(callback) {
    this.statusCallbacks.push(callback)
    return () => {
      const index = this.statusCallbacks.indexOf(callback)
      if (index !== -1) {
        this.statusCallbacks.splice(index, 1)
      }
    }
  }

  notifyStatus(status) {
    this.statusCallbacks.forEach(callback => callback(status))
  }

  getStatus() {
    if (!this.client) return 'disconnected'
    if (this.connected) return 'connected'
    if (this.reconnectAttempts > 0) return 'reconnecting'
    return 'disconnected'
  }
}

export default new WSClient()
