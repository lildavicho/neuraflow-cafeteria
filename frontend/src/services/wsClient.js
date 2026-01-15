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
    this.stompSubscriptions = new Map()
    this.listeners = new Map()
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

          if (this.pendingSubscriptions.length) {
            this.pendingSubscriptions.forEach(({ topic, callback }) => {
              this.addListener(topic, callback)
            })
            this.pendingSubscriptions = []
          }

          for (const topic of this.listeners.keys()) {
            this.ensureStompSubscription(topic)
          }

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
          this.stompSubscriptions.clear()
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

  ensureStompSubscription(topic) {
    if (!this.client || !this.connected) return null
    if (this.stompSubscriptions.has(topic)) return this.stompSubscriptions.get(topic)

    const sub = this.client.subscribe(topic, (message) => {
      let data
      try { data = JSON.parse(message.body) } catch { data = message.body }
      const cbs = this.listeners.get(topic)
      if (cbs && cbs.size) {
        for (const cb of cbs) {
          try { cb(data) } catch (err) { console.error('[WS] listener error', err) }
        }
      }
    })

    this.stompSubscriptions.set(topic, sub)
    return sub
  }

  addListener(topic, callback) {
    if (!this.listeners.has(topic)) this.listeners.set(topic, new Set())
    this.listeners.get(topic).add(callback)
  }

  subscribe(topic, callback) {
    if (!this.connected) {
      this.pendingSubscriptions.push({ topic, callback })
      this.addListener(topic, callback)
      this.connect().catch(() => {})
      return () => this.removeListener(topic, callback)
    }

    this.addListener(topic, callback)
    this.ensureStompSubscription(topic)

    return () => this.removeListener(topic, callback)
  }

  removeListener(topic, callback) {
    const set = this.listeners.get(topic)
    if (!set) return
    set.delete(callback)
    if (set.size === 0) {
      const sub = this.stompSubscriptions.get(topic)
      if (sub) sub.unsubscribe()
      this.stompSubscriptions.delete(topic)
      this.listeners.delete(topic)
    }
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate()
      this.client = null
      this.connected = false
      this.stompSubscriptions.clear()
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
