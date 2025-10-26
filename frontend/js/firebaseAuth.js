/**
 * Firebase Authentication Module
 * Handles email/password and Google Sign-In authentication using Firebase CDN ESM
 */

// Import Firebase modules from CDN
import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js';
import { 
  getAuth, 
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  signOut,
  onAuthStateChanged,
  sendPasswordResetEmail
} from 'https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js';

// Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyBLRnzgRJpkibrEwXUi2qzRYNeV-8nT-3w",
  authDomain: "baru-fe8a3.firebaseapp.com",
  projectId: "baru-fe8a3",
  storageBucket: "baru-fe8a3.firebasestorage.app",
  messagingSenderId: "882339697819",
  appId: "1:882339697819:web:0b596cece88ab322ba623e",
  measurementId: "G-TL3EPYC1MC"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const googleProvider = new GoogleAuthProvider();

// Configure Google Provider
googleProvider.setCustomParameters({
  prompt: 'select_account'
});

/**
 * Sign in with email and password
 * @param {string} email - User email
 * @param {string} password - User password
 * @returns {Promise<Object>} User credential
 */
export async function signInWithEmail(email, password) {
  try {
    const userCredential = await signInWithEmailAndPassword(auth, email, password);
    return {
      success: true,
      user: userCredential.user,
      token: await userCredential.user.getIdToken()
    };
  } catch (error) {
    console.error('Firebase sign in error:', error);
    throw {
      code: error.code,
      message: getErrorMessage(error.code)
    };
  }
}

/**
 * Create new user with email and password
 * @param {string} email - User email
 * @param {string} password - User password
 * @returns {Promise<Object>} User credential
 */
export async function registerWithEmail(email, password) {
  try {
    const userCredential = await createUserWithEmailAndPassword(auth, email, password);
    return {
      success: true,
      user: userCredential.user,
      token: await userCredential.user.getIdToken()
    };
  } catch (error) {
    console.error('Firebase registration error:', error);
    throw {
      code: error.code,
      message: getErrorMessage(error.code)
    };
  }
}

/**
 * Sign in with Google
 * @returns {Promise<Object>} User credential
 */
export async function signInWithGoogle() {
  try {
    const result = await signInWithPopup(auth, googleProvider);
    return {
      success: true,
      user: result.user,
      token: await result.user.getIdToken(),
      credential: GoogleAuthProvider.credentialFromResult(result)
    };
  } catch (error) {
    console.error('Google sign in error:', error);
    throw {
      code: error.code,
      message: getErrorMessage(error.code)
    };
  }
}

/**
 * Sign out current user
 * @returns {Promise<void>}
 */
export async function signOutUser() {
  try {
    await signOut(auth);
    return { success: true };
  } catch (error) {
    console.error('Sign out error:', error);
    throw error;
  }
}

/**
 * Send password reset email
 * @param {string} email - User email
 * @returns {Promise<Object>}
 */
export async function resetPassword(email) {
  try {
    await sendPasswordResetEmail(auth, email);
    return { success: true };
  } catch (error) {
    console.error('Password reset error:', error);
    throw {
      code: error.code,
      message: getErrorMessage(error.code)
    };
  }
}

/**
 * Get current user
 * @returns {Object|null} Current user or null
 */
export function getCurrentUser() {
  return auth.currentUser;
}

/**
 * Listen to auth state changes
 * @param {Function} callback - Callback function
 * @returns {Function} Unsubscribe function
 */
export function onAuthChange(callback) {
  return onAuthStateChanged(auth, callback);
}

/**
 * Get user-friendly error messages
 * @param {string} errorCode - Firebase error code
 * @returns {string} User-friendly error message
 */
function getErrorMessage(errorCode) {
  const errorMessages = {
    'auth/invalid-email': 'Correo electrónico inválido',
    'auth/user-disabled': 'Esta cuenta ha sido deshabilitada',
    'auth/user-not-found': 'No existe una cuenta con este correo',
    'auth/wrong-password': 'Contraseña incorrecta',
    'auth/email-already-in-use': 'Este correo ya está registrado',
    'auth/weak-password': 'La contraseña debe tener al menos 6 caracteres',
    'auth/operation-not-allowed': 'Operación no permitida',
    'auth/invalid-credential': 'Credenciales inválidas',
    'auth/account-exists-with-different-credential': 'Ya existe una cuenta con este correo usando otro método de inicio de sesión',
    'auth/popup-closed-by-user': 'Ventana de inicio de sesión cerrada',
    'auth/popup-blocked': 'Ventana emergente bloqueada por el navegador',
    'auth/network-request-failed': 'Error de conexión. Verifica tu internet',
    'auth/too-many-requests': 'Demasiados intentos. Intenta más tarde'
  };

  return errorMessages[errorCode] || 'Error de autenticación. Intenta nuevamente';
}

// Export auth instance for advanced usage
export { auth };
