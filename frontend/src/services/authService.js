import api from './apiService'
import { auth } from './firebase'
import { GoogleAuthProvider, OAuthProvider, signInWithPopup } from 'firebase/auth'

const TOKEN_KEY = 'token'
const USER_KEY = 'user'

// ---- Email/Password Login con Backend ----
export const login = async (email, password) => {
  try {
    const { data } = await api.post('/auth/login', { email, password })

    // El backend devuelve: { token, refreshToken, userId, email, fullName, role }
    const { token, refreshToken, userId, email: userEmail, fullName, role } = data

    // Construir objeto user para compatibilidad
    const user = {
      id: userId,
      email: userEmail,
      name: fullName,
      role: role,
    }

    // Guardar token y usuario en localStorage
    localStorage.setItem(TOKEN_KEY, token)
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken)
    }
    api.defaults.headers.common.Authorization = `Bearer ${token}`
    localStorage.setItem(USER_KEY, JSON.stringify({
      ...user,
      timestamp: Date.now(),
    }))

    return user
  } catch (e) {
    const message = e.response?.data?.error || e.response?.data?.message || 'Credenciales inválidas'
    throw new Error(message)
  }
}

// ---- Registro (placeholder - implementar si existe endpoint) ----
export const register = async (email, password, displayName) => {
  try {
    const { data } = await api.post('/auth/register', {
      email,
      password,
      name: displayName
    })
    const { token, user } = data

    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_KEY, JSON.stringify({
      ...user,
      timestamp: Date.now(),
    }))

    return user
  } catch (e) {
    const message = e.response?.data?.error || e.response?.data?.message || 'Error al registrarse'
    throw new Error(message)
  }
}

// ---- Reset Password (placeholder) ----
export const resetPassword = async (email) => {
  try {
    await api.post('/auth/reset-password', { email })
  } catch (e) {
    const message = e.response?.data?.error || 'Error al enviar correo de recuperación'
    throw new Error(message)
  }
}

// ---- Logout ----
export const logout = async () => {
  try {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      // Intentar notificar al backend (opcional)
      await api.post('/auth/logout').catch(() => {
        // Ignorar errores de logout del backend
      })
    }
  } finally {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem('refreshToken')
    delete api.defaults.headers.common.Authorization
  }
}

// ---- Social Login (Google + Microsoft via Firebase) ----
const googleProvider = new GoogleAuthProvider()
const microsoftProvider = new OAuthProvider('microsoft.com')

microsoftProvider.setCustomParameters({
  tenant: 'common',
})

const handleFirebaseLogin = async (provider, errorMessage) => {
  try {
    const result = await signInWithPopup(auth, provider)
    const idToken = await result.user.getIdToken()
    const { data } = await api.post('/auth/firebase', { idToken })
    if (data?.token) {
      localStorage.setItem(TOKEN_KEY, data.token)
      api.defaults.headers.common.Authorization = `Bearer ${data.token}`
    }
    if (data?.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken)
    }
    localStorage.setItem(
      USER_KEY,
      JSON.stringify({
        ...data,
        timestamp: Date.now(),
      })
    )
    return data
  } catch (err) {
    throw new Error(err?.message || errorMessage)
  }
}

export const loginWithGoogle = () => handleFirebaseLogin(googleProvider, 'Error al iniciar sesión con Google')

export const loginWithMicrosoft = () => handleFirebaseLogin(microsoftProvider, 'Error al iniciar sesión con Microsoft')

// ---- Helpers ----
export const getToken = () => {
  return localStorage.getItem(TOKEN_KEY)
}

// Alias para compatibilidad con código existente
export const getIdToken = async () => {
  return getToken()
}

export const getCurrentUser = () => {
  const userStr = localStorage.getItem(USER_KEY)
  if (!userStr) return null

  try {
    return JSON.parse(userStr)
  } catch {
    return null
  }
}

// Verificar si hay sesión válida
export const checkSession = async () => {
  const token = getToken()
  if (!token) return null

  try {
    const { data } = await api.get('/auth/me')
    // Actualizar datos del usuario en localStorage
    localStorage.setItem(USER_KEY, JSON.stringify({
      ...data,
      timestamp: Date.now(),
    }))
    return data
  } catch {
    // Token inválido o expirado
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    return null
  }
}

// Para compatibilidad: simula onAuthChanged pero con verificación inicial
export const onAuthChanged = (callback) => {
  // Verificar sesión al inicio
  const user = getCurrentUser()
  const token = getToken()

  if (user && token) {
    // Simular usuario de Firebase para compatibilidad
    callback({
      uid: user.id?.toString() || user.email,
      email: user.email,
      displayName: user.name,
      photoURL: user.avatarUrl || null,
    })
  } else {
    callback(null)
  }

  // Retornar función de cleanup (no-op)
  return () => { }
}
