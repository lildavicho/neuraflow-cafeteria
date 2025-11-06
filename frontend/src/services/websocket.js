import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getIdToken } from './authService'
import { resolveWsEndpoint } from './wsClient'

let client
let connected = false
let connectingPromise
const activeSubscriptions = new Map()
const pendingSubscriptions = []

const resolveEndpoint = () => resolveWsEndpoint()

const buildHeaders = (token) => (token ? { Authorization: `Bearer ${token}` } : {})

const ensureClient = async () => {
  if (connected && client?.connected) return client
  if (connectingPromise) return connectingPromise

  connectingPromise = new Promise((resolve, reject) => {
    const wsEndpoint = resolveEndpoint()
    const instance = new Client({
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
      webSocketFactory: () => new SockJS(wsEndpoint),
      beforeConnect: async () => {
        const token = await getIdToken()
        instance.connectHeaders = buildHeaders(token)
      },
      onConnect: () => {
        connected = true
        pendingSubscriptions.splice(0).forEach(({ topic, handler, headers }) => {
          const subscription = instance.subscribe(topic, (message) => {
            try {
              handler(JSON.parse(message.body))
            } catch {
              handler(message.body)
            }
          }, headers)
          activeSubscriptions.set(handler, subscription)
        })
        resolve(instance)
      },
      onStompError: (frame) => {
        console.warn('STOMP error', frame.headers?.message)
      },
      onWebSocketClose: () => {
        connected = false
        activeSubscriptions.clear()
      },
      onWebSocketError: (err) => {
        if (!connected) reject(err)
      },
    })

    client = instance
    instance.activate()
  })

  return connectingPromise.finally(() => {
    connectingPromise = null
  })
}

export const subscribe = async (topic, handler, headers = {}) => {
  const instance = await ensureClient()
  if (connected && instance.connected) {
    const subscription = instance.subscribe(topic, (message) => {
      try {
        handler(JSON.parse(message.body))
      } catch {
        handler(message.body)
      }
    }, headers)
    activeSubscriptions.set(handler, subscription)
  } else {
    pendingSubscriptions.push({ topic, handler, headers })
  }

  return () => {
    const sub = activeSubscriptions.get(handler)
    if (sub) {
      try {
        sub.unsubscribe()
      } catch (err) {
        console.warn('Error unsubscribing', err)
      }
    }
    activeSubscriptions.delete(handler)
  }
}

export const publish = async (destination, body = {}, headers = {}) => {
  const instance = await ensureClient()
  instance.publish({ destination, body: JSON.stringify(body), headers })
}

export const disconnect = async () => {
  if (client) {
    await client.deactivate()
  }
  connected = false
  activeSubscriptions.clear()
}

export const isConnected = () => connected
