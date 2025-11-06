import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import ErrorBoundary from './components/common/ErrorBoundary.jsx'
import './index.css'

document.title = 'Cafetería – Sistema de Gestión'

if (typeof window !== 'undefined') {
  window.addEventListener('error', (event) => {
    console.error('window.error:', event?.error || event?.message || event)
  })
  window.addEventListener('unhandledrejection', (event) => {
    console.error('unhandledrejection:', event?.reason ?? event)
  })
}

const container = document.getElementById('root')
if (!container) {
  throw new Error('No se encontró el elemento root')
}

createRoot(container).render(
  <StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </StrictMode>
)
