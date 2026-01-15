import { getToken, onMessage } from 'firebase/messaging'
import { firebaseConfig, getMessagingInstance } from './firebase'
import api from './apiService'

let messaging = null

const registerFirebaseServiceWorker = async () => {
  if (!('serviceWorker' in navigator)) {
    throw new Error('Service Workers no soportados')
  }

  const encodedConfig = encodeURIComponent(btoa(JSON.stringify(firebaseConfig)))
  const swUrl = `/firebase-messaging-sw.js?config=${encodedConfig}`
  const registration = await navigator.serviceWorker.register(swUrl)

  const sendInitMessage = (reg) => {
    const worker = reg.active ?? reg.waiting ?? reg.installing
    worker?.postMessage({ type: 'firebase-init', config: firebaseConfig })
  }

  sendInitMessage(registration)
  const ready = await navigator.serviceWorker.ready
  sendInitMessage(ready)
  return ready
}

export const requestNotificationPermission = async () => {
  try {
    const permission = await Notification.requestPermission()
    if (permission !== 'granted') return null

    const messagingInstance = await getMessagingInstance()
    if (!messagingInstance) return null
    messaging = messagingInstance

    const swRegistration = await registerFirebaseServiceWorker()
    const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY || import.meta.env.VITE_FB_VAPID_KEY
    if (!vapidKey) return null

    const token = await getToken(messagingInstance, {
      vapidKey,
      serviceWorkerRegistration: swRegistration,
    })
    return token
  } catch (err) {
    console.error('Error requesting notification permission:', err)
    return null
  }
}

export const registerToken = async (userId, token) => {
  if (!token) return
  
  try {
    await api.post('/push/register', {
      token,
      platform: 'web'
    })
  } catch (err) {
    console.error('Error registering FCM token:', err)
  }
}

export const onMessageListener = (callback) => {
  const attach = async () => {
    try {
      const instance = await getMessagingInstance()
      if (!instance) return () => {}
      messaging = instance
      return onMessage(instance, (payload) => callback(payload))
    } catch {
      return () => {}
    }
  }
  return attach()
}

export const initNotifications = async (userId) => {
  if (!userId) return
  try {
    const token = await requestNotificationPermission()
    if (token) await registerToken(userId, token)
  } catch (err) {
    console.error('Error initializing notifications:', err)
  }
}
