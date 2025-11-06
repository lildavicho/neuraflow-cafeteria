/* eslint-env serviceworker */
/* global firebase, importScripts */

importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-app-compat.js')
importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-messaging-compat.js')

const fallbackConfig = {
  apiKey: self.VITE_FIREBASE_API_KEY,
  authDomain: self.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: self.VITE_FIREBASE_PROJECT_ID,
  storageBucket: self.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: self.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: self.VITE_FIREBASE_APP_ID,
  measurementId: self.VITE_FIREBASE_MEASUREMENT_ID,
}

const decodeConfigFromUrl = () => {
  try {
    const url = new URL(self.location.href)
    const encoded = url.searchParams.get('config')
    if (encoded) {
      return JSON.parse(atob(encoded))
    }
  } catch (err) {
    console.warn('[Firebase SW] No se pudo decodificar la configuración:', err)
  }
  return null
}

let messaging

const initFirebase = (config) => {
  if (messaging) return messaging
  const resolvedConfig = config || decodeConfigFromUrl() || self.__FIREBASE_CONFIG || fallbackConfig

  if (!resolvedConfig || !resolvedConfig.apiKey) {
    console.error('[Firebase SW] Configuración inválida, no se puede inicializar Firebase')
    return null
  }

  try {
    if (firebase.apps?.length === 0) {
      firebase.initializeApp(resolvedConfig)
    } else {
      firebase.app()
    }
    messaging = firebase.messaging()
  } catch (err) {
    console.error('[Firebase SW] Error inicializando Firebase:', err)
    messaging = null
  }
  return messaging
}

initFirebase()

self.addEventListener('message', (event) => {
  if (event?.data?.type === 'firebase-init') {
    initFirebase(event.data.config)
  }
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const targetUrl = event.notification?.data?.url || '/'
  event.waitUntil(self.clients.openWindow(targetUrl))
})

const handleBackgroundMessage = (payload) => {
  const notificationTitle = payload?.notification?.title || 'UCACUE Cafetería'
  const notificationOptions = {
    body: payload?.notification?.body,
    icon: '/icons/icon-192.png',
    data: payload?.data,
  }
  self.registration.showNotification(notificationTitle, notificationOptions)
}

const messagingInstance = initFirebase()
if (messagingInstance?.onBackgroundMessage) {
  messagingInstance.onBackgroundMessage(handleBackgroundMessage)
}
