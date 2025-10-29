import { initializeApp } from 'firebase/app'
import { getAuth, setPersistence, browserLocalPersistence } from 'firebase/auth'
import { getFirestore } from 'firebase/firestore'
import { getStorage } from 'firebase/storage'
import { getMessaging, isSupported } from 'firebase/messaging'

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID
}

const app = initializeApp(firebaseConfig)
const auth = getAuth(app)
auth.useDeviceLanguage()

// ✅ Corrige el error: mueve los awaits dentro de una función asincrónica
;(async () => {
  try {
    await setPersistence(auth, browserLocalPersistence)
  } catch (err) {
    console.warn('Persistencia local no soportada:', err)
  }
})()

const db = getFirestore(app)
const storage = getStorage(app)

let messaging = null
;(async () => {
  try {
    if (await isSupported()) messaging = getMessaging(app)
  } catch (err) {
    console.warn('Mensajería no soportada:', err)
  }
})()

export { app, auth, db, storage, messaging }
export default app

