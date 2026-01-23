import { getMessaging, getToken, onMessage } from 'firebase/messaging'
import { app } from './firebase'
import api from './apiService'

let messaging = null

try {
  if (typeof window !== 'undefined' && 'Notification' in window) {
    messaging = getMessaging(app)
  }
} catch (err) {
  console.warn('Firebase Messaging not available:', err)
}

export const requestNotificationPermission = async () => {
  if (!messaging) {
    console.warn('Messaging not initialized')
    return null
  }

  try {
    const permission = await Notification.requestPermission()
    
    if (permission === 'granted') {
      const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY || import.meta.env.VITE_FB_VAPID_KEY
      if (!vapidKey) {
        console.error('Falta VITE_FIREBASE_VAPID_KEY en el entorno')
        return null
      }
      const token = await getToken(messaging, { vapidKey })
      
      return token
    } else {
      console.log('Notification permission denied')
      return null
    }
  } catch (err) {
    console.error('Error requesting notification permission:', err)
    return null
  }
}

export const registerToken = async (userId, token) => {
  if (!token) return
  
  try {
    const deviceName = typeof navigator !== 'undefined'
      ? String(navigator.userAgent || 'web').slice(0, 255)
      : 'web'
    await api.post('/push/register', {
      token,
      platform: 'web',
      deviceName,
      active: true
    })
  } catch (err) {
    console.error('Error registering FCM token:', err)
  }
}

export const onMessageListener = (callback) => {
  if (!messaging) return () => {}
  
  return onMessage(messaging, (payload) => {
    callback(payload)
  })
}

export const initNotifications = async (userId) => {
  if (!messaging || !userId) return
  
  try {
    const token = await requestNotificationPermission()
    if (token) {
      await registerToken(userId, token)
    }
  } catch (err) {
    console.error('Error initializing notifications:', err)
  }
}
