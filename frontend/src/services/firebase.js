import { getApps, initializeApp, getApp } from 'firebase/app'
import { getAuth, setPersistence, browserLocalPersistence } from 'firebase/auth'
import { getFirestore } from 'firebase/firestore'
import { getStorage } from 'firebase/storage'
import { getMessaging, isSupported } from 'firebase/messaging'

const requiredKeys = [
  'VITE_FIREBASE_API_KEY',
  'VITE_FIREBASE_AUTH_DOMAIN',
  'VITE_FIREBASE_PROJECT_ID',
  'VITE_FIREBASE_STORAGE_BUCKET',
  'VITE_FIREBASE_MESSAGING_SENDER_ID',
  'VITE_FIREBASE_APP_ID',
]

const missingKeys = requiredKeys.filter((key) => !import.meta.env[key])
if (missingKeys.length) {
  console.error(
    '[Firebase] Faltan variables de entorno:',
    missingKeys.join(', ')
  )
}

export const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY ?? '',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN ?? '',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID ?? '',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET ?? '',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID ?? '',
  appId: import.meta.env.VITE_FIREBASE_APP_ID ?? '',
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID ?? '',
}

const app = getApps().length ? getApp() : initializeApp(firebaseConfig)
const auth = getAuth(app)
auth.useDeviceLanguage()

setPersistence(auth, browserLocalPersistence).catch((err) => {
  console.warn('Persistencia local no soportada:', err)
})

const db = getFirestore(app)
const storage = getStorage(app)

let messagingInstance = null
let messagingPromise = null

const resolveMessaging = async () => {
  if (!messagingPromise) {
    messagingPromise = (async () => {
      try {
        if (!(await isSupported())) {
          console.info('Firebase Messaging no está soportado en este navegador')
          return null
        }
        messagingInstance = getMessaging(app)
        return messagingInstance
      } catch (err) {
        console.warn('Mensajería no soportada:', err)
        return null
      }
    })()
  }
  return messagingPromise
}

export const getMessagingInstance = async () => {
  if (messagingInstance) return messagingInstance
  return resolveMessaging()
}

export { app, auth, db, storage, messagingInstance as messaging }
export default app
