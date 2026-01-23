import { useState, useEffect } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { checkSession, getToken, getCurrentUser, logout as authLogout, login as authLogin } from '../services/authService';
import { initNotifications } from '../services/notifications';
import { AuthContext } from './authContextValue';
import { auth } from '../services/firebase';
import api from '../services/apiService';

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState(() => getToken());

  // Verificar sesión al cargar la app
  useEffect(() => {
    const initAuth = async () => {
      try {
        const savedToken = getToken();
        if (savedToken) {
          api.defaults.headers.common['Authorization'] = `Bearer ${savedToken}`;
          const userData = await checkSession();
          if (userData) {
            const userPayload = {
              id: userData.id,
              uid: userData.id?.toString(),
              email: userData.email,
              displayName: userData.name,
              name: userData.name,
              photoURL: userData.avatarUrl || null,
              role: userData.role,
              active: userData.active,
            };
            setUser(userPayload);
            setToken(savedToken);
            initNotifications(userData.id?.toString()).catch(console.error);
          }
        } else {
          delete api.defaults.headers.common['Authorization'];
        }
      } catch (error) {
        console.error('Error verificando sesión:', error);
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      if (firebaseUser) {
        try {
          const firebaseToken = await firebaseUser.getIdToken();
          const { data } = await api.post('/auth/firebase', { idToken: firebaseToken });
          if (data?.token) {
            localStorage.setItem('token', data.token);
            localStorage.setItem('refreshToken', data.refreshToken);
            api.defaults.headers.common['Authorization'] = `Bearer ${data.token}`;
            setToken(data.token);
            const userPayload = {
              id: data.userId,
              uid: data.userId?.toString(),
              email: data.email,
              displayName: data.fullName,
              name: data.fullName,
              photoURL: firebaseUser.photoURL || null,
              role: data.role,
              active: true,
            };
            setUser(userPayload);
            initNotifications(data.userId?.toString()).catch(console.error);
          }
        } catch (error) {
          console.error('Firebase auth error:', error);
        }
      } else {
        const savedToken = getToken();
        if (!savedToken) {
          setUser(null);
          setToken(null);
          delete api.defaults.headers.common['Authorization'];
          localStorage.removeItem('token');
          localStorage.removeItem('refreshToken');
        }
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  useEffect(() => {
    const interval = setInterval(async () => {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const { data } = await api.post('/auth/refresh', { refreshToken });
          if (data?.token) {
            localStorage.setItem('token', data.token);
            api.defaults.headers.common['Authorization'] = `Bearer ${data.token}`;
            if (data?.refreshToken) {
              localStorage.setItem('refreshToken', data.refreshToken);
            }
            setToken(data.token);
          }
        } catch (error) {
          console.error('Refresh failed:', error);
        }
      }
    }, 50 * 60 * 1000);

    return () => clearInterval(interval);
  }, []);

  // Función de login
  const login = async (email, password) => {
    const userData = await authLogin(email, password);
    const savedToken = getToken();
    if (savedToken) {
      api.defaults.headers.common['Authorization'] = `Bearer ${savedToken}`;
    }

    setUser({
      id: userData.id,
      uid: userData.id?.toString(),
      email: userData.email,
      displayName: userData.name,
      name: userData.name,
      photoURL: userData.avatarUrl || null,
      role: userData.role,
      active: userData.active,
    });
    setToken(savedToken);

    // Inicializar notificaciones
    initNotifications(userData.id?.toString()).catch(console.error);

    return userData;
  };

  // Función de logout
  const logout = async () => {
    await authLogout();
    setUser(null);
    setToken(null);
    delete api.defaults.headers.common['Authorization'];
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
  };

  const value = {
    user,
    token,
    loading,
    isAuthenticated: !!user && !!token,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
