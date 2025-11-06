import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  sendPasswordResetEmail,
  updateProfile,
  onAuthStateChanged,
  GoogleAuthProvider,
  OAuthProvider,
  signInWithPopup,
} from 'firebase/auth'
import { doc, setDoc, serverTimestamp } from 'firebase/firestore'
import { auth, db } from './firebase'

// ---- Email/Password ----
export const login = async (email, password) => {
  try {
    const { user } = await signInWithEmailAndPassword(auth, email, password)
    return user
  } catch (e) {
    throw new Error(mapError(e.code))
  }
}

export const register = async (email, password, displayName) => {
  try {
    const { user } = await createUserWithEmailAndPassword(auth, email, password)
    await updateProfile(user, { displayName })

    await setDoc(
      doc(db, 'users', user.uid),
      {
        email,
        displayName,
        createdAt: serverTimestamp(),
        role: 'BUYER',
      },
      { merge: true }
    )

    return user
  } catch (e) {
    throw new Error(mapError(e.code))
  }
}

export const resetPassword = async (email) => {
  try {
    await sendPasswordResetEmail(auth, email)
  } catch (e) {
    throw new Error(mapError(e.code))
  }
}

export const logout = async () => {
  try {
    await signOut(auth)
    localStorage.removeItem('user')
  } catch {
    throw new Error('Error al cerrar sesión')
  }
}

// ---- Google Sign-In ----
export const loginWithGoogle = async () => {
  try {
    const provider = new GoogleAuthProvider()
    provider.setCustomParameters({
      prompt: 'select_account',
      client_id:
        '882339697819-67gpv1o72hir43iavq64fjfu53rteguu.apps.googleusercontent.com',
    })
    provider.addScope('email')

    const { user } = await signInWithPopup(auth, provider)

    await setDoc(
      doc(db, 'users', user.uid),
      {
        email: user.email,
        displayName: user.displayName,
        photoURL: user.photoURL || null,
        createdAt: serverTimestamp(),
        role: 'BUYER',
      },
      { merge: true }
    )

    return user
  } catch (e) {
    throw new Error(mapError(e.code))
  }
}

// ---- Microsoft Sign-In ----
export const loginWithMicrosoft = async () => {
  try {
    const provider = new OAuthProvider('microsoft.com');
    provider.addScope('email');
    provider.addScope('profile');
    provider.addScope('openid');
    const params = { prompt: 'select_account' };
    const tenant = import.meta.env.VITE_MS_TENANT?.trim();
    if (tenant) params.tenant = tenant;
    const domainHint = import.meta.env.VITE_MS_DOMAIN_HINT?.trim();
    if (domainHint) params.domain_hint = domainHint;
    provider.setCustomParameters(params);

    const { user } = await signInWithPopup(auth, provider);

    await setDoc(
      doc(db, 'users', user.uid),
      {
        email: user.email,
        displayName: user.displayName,
        photoURL: user.photoURL || null,
        createdAt: serverTimestamp(),
        role: 'BUYER',
      },
      { merge: true }
    );

    return user;
  } catch (e) {
    throw new Error(mapError(e.code));
  }
};

// ---- Helpers ----
export const getIdToken = async () => {
  const user = auth.currentUser
  return user ? await user.getIdToken() : null
}

export const onAuthChanged = (cb) => onAuthStateChanged(auth, cb)

const mapError = (code) => {
  const messages = {
    'auth/email-already-in-use': 'Este correo ya está registrado',
    'auth/invalid-email': 'Correo electrónico inválido',
    'auth/operation-not-allowed': 'Operación no permitida',
    'auth/weak-password': 'La contraseña es muy débil (mínimo 6 caracteres)',
    'auth/user-disabled': 'Usuario deshabilitado',
    'auth/user-not-found': 'Usuario no encontrado',
    'auth/wrong-password': 'Contraseña incorrecta',
    'auth/too-many-requests': 'Demasiados intentos. Intenta más tarde',
    'auth/network-request-failed': 'Error de conexión',
    'auth/invalid-credential': 'Credenciales inválidas',
    'auth/popup-closed-by-user':
      'Popup cerrado antes de completar. Intenta nuevamente.',
    'auth/cancelled-popup-request':
      'Se canceló la solicitud previa. Intenta de nuevo.',
    'auth/popup-blocked':
      'El navegador bloqueó el popup. Habilítalo e intenta otra vez.',
    'auth/unauthorized-domain':
      'Dominio no autorizado en Firebase Auth. Agrega localhost:5173 y 5174.',
    'auth/idpiframe_initialization_failed':
      'Origen no autorizado para Google Sign-In. Agrega http://localhost:5173 y http://localhost:5174 en dominios autorizados.',
  }
  return messages[code] || 'Error desconocido. Intenta nuevamente.'
}

