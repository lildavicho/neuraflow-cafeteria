/**
 * Authentication Integration Module
 * Bridges Firebase Auth with Spring Boot backend
 */

import * as firebaseAuth from './firebaseAuth.js';
import { auth as backendAuth } from './api.js';

/**
 * Unified login with email/password
 * Uses Firebase for authentication, then syncs with backend
 * @param {string} email 
 * @param {string} password 
 * @param {boolean} rememberMe 
 * @returns {Promise<Object>}
 */
export async function login(email, password, rememberMe = false) {
  try {
    // Step 1: Authenticate with Firebase
    console.log('[AuthIntegration] Authenticating with Firebase...');
    const firebaseResult = await firebaseAuth.signInWithEmail(email, password);
    
    if (!firebaseResult.success) {
      throw new Error('Firebase authentication failed');
    }

    console.log('[AuthIntegration] Firebase auth successful, syncing with backend...');
    
    // Step 2: Send Firebase ID token to backend for validation and JWT generation
    const backendResult = await syncWithBackend(firebaseResult.token);
    
    // Step 3: Store authentication data
    storeAuthData(backendResult, rememberMe);
    
    return {
      success: true,
      user: backendResult.user,
      role: backendResult.role,
      requiresRedirect: true
    };
    
  } catch (error) {
    console.error('[AuthIntegration] Login error:', error);
    throw {
      message: error.message || 'Error al iniciar sesión',
      code: error.code
    };
  }
}

/**
 * Unified registration with email/password
 * Creates Firebase user, then registers in backend
 * @param {Object} userData - {fullName, email, password}
 * @returns {Promise<Object>}
 */
export async function register(userData) {
  try {
    const { fullName, email, password } = userData;
    
    // Step 1: Create Firebase user
    console.log('[AuthIntegration] Creating Firebase user...');
    const firebaseResult = await firebaseAuth.registerWithEmail(email, password);
    
    if (!firebaseResult.success) {
      throw new Error('Firebase registration failed');
    }

    console.log('[AuthIntegration] Firebase user created, registering in backend...');
    
    // Step 2: Register in backend with Firebase ID token
    const backendResult = await registerInBackend({
      fullName,
      email,
      firebaseUid: firebaseResult.user.uid,
      firebaseToken: firebaseResult.token
    });
    
    return {
      success: true,
      message: 'Cuenta creada exitosamente',
      user: backendResult.user
    };
    
  } catch (error) {
    console.error('[AuthIntegration] Registration error:', error);
    
    // If backend registration fails, we should ideally delete the Firebase user
    // For now, we'll just throw the error
    throw {
      message: error.message || 'Error al crear la cuenta',
      code: error.code
    };
  }
}

/**
 * Google Sign-In
 * Authenticates with Google via Firebase, then syncs with backend
 * @returns {Promise<Object>}
 */
export async function loginWithGoogle() {
  try {
    // Step 1: Authenticate with Google via Firebase
    console.log('[AuthIntegration] Authenticating with Google...');
    const firebaseResult = await firebaseAuth.signInWithGoogle();
    
    if (!firebaseResult.success) {
      throw new Error('Google authentication failed');
    }

    console.log('[AuthIntegration] Google auth successful, syncing with backend...');
    
    // Step 2: Sync with backend
    const backendResult = await syncWithBackend(firebaseResult.token, {
      isGoogleAuth: true,
      fullName: firebaseResult.user.displayName,
      email: firebaseResult.user.email,
      photoURL: firebaseResult.user.photoURL
    });
    
    // Step 3: Store authentication data
    storeAuthData(backendResult, true);
    
    return {
      success: true,
      user: backendResult.user,
      role: backendResult.role,
      requiresRedirect: true
    };
    
  } catch (error) {
    console.error('[AuthIntegration] Google login error:', error);
    throw {
      message: error.message || 'Error al iniciar sesión con Google',
      code: error.code
    };
  }
}

/**
 * Logout from both Firebase and backend
 * @returns {Promise<void>}
 */
export async function logout() {
  try {
    // Sign out from Firebase
    await firebaseAuth.signOutUser();
    
    // Clear backend session
    backendAuth.logout();
    
  } catch (error) {
    console.error('[AuthIntegration] Logout error:', error);
    // Force logout even if there's an error
    backendAuth.logout();
  }
}

/**
 * Send password reset email
 * @param {string} email 
 * @returns {Promise<Object>}
 */
export async function resetPassword(email) {
  try {
    await firebaseAuth.resetPassword(email);
    return {
      success: true,
      message: 'Se ha enviado un correo para restablecer tu contraseña'
    };
  } catch (error) {
    console.error('[AuthIntegration] Password reset error:', error);
    throw {
      message: error.message || 'Error al enviar el correo de restablecimiento',
      code: error.code
    };
  }
}

/**
 * Get current authenticated user
 * @returns {Object|null}
 */
export function getCurrentUser() {
  return backendAuth.me();
}

/**
 * Check if user is authenticated
 * @returns {boolean}
 */
export function isAuthenticated() {
  const user = getCurrentUser();
  const token = localStorage.getItem('token');
  return !!(user && token);
}

// ============ PRIVATE HELPER FUNCTIONS ============

/**
 * Sync Firebase authentication with backend
 * @private
 */
async function syncWithBackend(firebaseToken, additionalData = {}) {
  try {
    const response = await fetch('/api/auth/firebase', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        idToken: firebaseToken,
        ...additionalData
      })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Backend sync failed');
    }

    return await response.json();
    
  } catch (error) {
    console.error('[AuthIntegration] Backend sync error:', error);
    throw error;
  }
}

/**
 * Register user in backend
 * @private
 */
async function registerInBackend(userData) {
  try {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(userData)
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Backend registration failed');
    }

    return await response.json();
    
  } catch (error) {
    console.error('[AuthIntegration] Backend registration error:', error);
    throw error;
  }
}

/**
 * Store authentication data in localStorage
 * @private
 */
function storeAuthData(backendResult, rememberMe) {
  localStorage.setItem('token', backendResult.token);
  localStorage.setItem('refreshToken', backendResult.refreshToken);
  localStorage.setItem('user', JSON.stringify({
    id: backendResult.userId,
    email: backendResult.email,
    name: backendResult.fullName,
    role: backendResult.role
  }));
  
  if (rememberMe) {
    localStorage.setItem('rememberMe', 'true');
  }
}

/**
 * Initialize auth state listener
 * Monitors Firebase auth state and keeps backend in sync
 */
export function initAuthStateListener() {
  firebaseAuth.onAuthChange(async (user) => {
    console.log('[AuthIntegration] Auth state changed:', user ? 'signed in' : 'signed out');
    
    if (!user) {
      // User signed out from Firebase, clear backend session
      const token = localStorage.getItem('token');
      if (token) {
        console.log('[AuthIntegration] Clearing backend session...');
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
      }
    }
  });
}

// Export Firebase auth functions for direct access if needed
export { firebaseAuth };
