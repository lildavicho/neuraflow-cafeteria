import { useEffect, useState } from 'react'
import { getToken } from 'firebase/messaging'
import { useToast } from '../hooks/useToast'
import Button from '../components/common/Button'
import Toast from '../components/common/Toast'
import { useTheme } from '../hooks/useTheme'
import { useLanguage } from '../hooks/useLanguage'
import { firebaseConfig, getMessagingInstance } from '../services/firebase'
import { registerPushToken } from '../services/apiService'

const registerFirebaseServiceWorker = async () => {
  if (!('serviceWorker' in navigator)) {
    throw new Error('Service Workers no soportados')
  }

  const encodedConfig = encodeURIComponent(btoa(JSON.stringify(firebaseConfig)))
  const swUrl = `/firebase-messaging-sw.js?config=${encodedConfig}`
  const registration = await navigator.serviceWorker.register(swUrl)

  const sendInitMessage = (reg) => {
    const worker = reg.active ?? reg.waiting ?? reg.installing
    worker?.postMessage({
      type: 'firebase-init',
      config: firebaseConfig,
    })
  }

  sendInitMessage(registration)
  const readyRegistration = await navigator.serviceWorker.ready
  sendInitMessage(readyRegistration)

  return readyRegistration
}

const AjustesPage = () => {
  const { theme, setTheme, toggleTheme } = useTheme()
  const { language, setLanguage, t } = useLanguage()
  const { toasts, success, error, removeToast } = useToast()
  const [pushEnabled, setPushEnabled] = useState(() => localStorage.getItem('cafeteria-push') === 'true')
  const [processingPush, setProcessingPush] = useState(false)

  useEffect(() => {
    localStorage.setItem('cafeteria-lang', language)
  }, [language])

  const requestPushPermission = async () => {
    if (!('Notification' in globalThis)) {
      throw new Error('Las notificaciones no son compatibles con este navegador')
    }
    const permission = await Notification.requestPermission()
    if (permission !== 'granted') {
      throw new Error('Permiso de notificaciones denegado')
    }
    const messagingInstance = await getMessagingInstance()
    if (!messagingInstance) {
      throw new Error('Firebase Messaging no está disponible')
    }

    const swRegistration = await registerFirebaseServiceWorker()
    const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY
    if (!vapidKey) {
      throw new Error('Falta VITE_FIREBASE_VAPID_KEY en el entorno')
    }
    const token = await getToken(messagingInstance, {
      vapidKey,
      serviceWorkerRegistration: swRegistration,
    })
    if (!token) {
      throw new Error('No se pudo obtener el token de notificaciones')
    }
    await registerPushToken({ token, platform: 'web' })
    localStorage.setItem('cafeteria-push', 'true')
    setPushEnabled(true)
    success('Notificaciones push activadas')
  }

  const disablePush = async () => {
    localStorage.removeItem('cafeteria-push')
    setPushEnabled(false)
    success('Notificaciones push desactivadas')
  }

  const handlePushToggle = async () => {
    try {
      setProcessingPush(true)
      if (pushEnabled) {
        await disablePush()
      } else {
        await requestPushPermission()
      }
    } catch (err) {
      console.error(err)
      error(err.message || 'No se pudo actualizar la configuración de push')
    } finally {
      setProcessingPush(false)
    }
  }

  const handleLanguageChange = (evt) => {
    setLanguage(evt.target.value)
    success('Idioma actualizado')
  }

  const resetSettings = () => {
    setTheme('light')
    setLanguage('es')
    disablePush()
    success('Configuraciones restablecidas')
  }

  return (
    <div className="p-6 max-w-4xl mx-auto space-y-6">
      <Toast toasts={toasts} onRemove={removeToast} />

      <header>
        <h1 className="text-3xl font-semibold text-gray-900 dark:text-gray-50">{t('settings')}</h1>
        <p className="text-gray-600 dark:text-gray-400">Personaliza tu experiencia de trabajo</p>
      </header>

      <section className="card-brand">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">{t('theme')}</h2>
            <p className="text-sm text-gray-600 dark:text-gray-400">{theme === 'dark' ? t('dark') : t('light')}</p>
          </div>
          <button
            onClick={toggleTheme}
            className={`relative inline-flex h-6 w-12 items-center rounded-full transition-colors ${theme === 'dark' ? 'bg-brand' : 'bg-gray-300'}`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${theme === 'dark' ? 'translate-x-6' : 'translate-x-1'}`}
            />
          </button>
        </div>
      </section>

      <section className="card-brand space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">{t('language')}</h2>
            <p className="text-sm text-gray-600 dark:text-gray-400">{language === 'es' ? t('spanish') : t('english')}</p>
          </div>
          <select
            value={language}
            onChange={handleLanguageChange}
            className="px-4 py-2 rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-brand"
          >
            <option value="es">{t('spanish')}</option>
            <option value="en">{t('english')}</option>
          </select>
        </div>
      </section>

      <section className="card-brand space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">{t('enablePush')}</h2>
            <p className="text-sm text-gray-600 dark:text-gray-400">Recibe alertas de pedidos y ventas desde cualquier pestaña</p>
          </div>
          <button
            onClick={handlePushToggle}
            disabled={processingPush}
            className={`relative inline-flex h-6 w-12 items-center rounded-full transition-colors ${pushEnabled ? 'bg-success-500' : 'bg-gray-300'} ${processingPush ? 'opacity-60 cursor-not-allowed' : ''}`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${pushEnabled ? 'translate-x-6' : 'translate-x-1'}`}
            />
          </button>
        </div>
      </section>

      <footer className="flex items-center justify-end gap-3">
        <Button variant="secondary" onClick={resetSettings}>
          Restablecer
        </Button>
      </footer>
    </div>
  )
}

export default AjustesPage
