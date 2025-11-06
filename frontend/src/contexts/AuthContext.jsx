import { useState, useEffect } from 'react';
import { onAuthChanged, getIdToken } from '../services/authService';
import { initNotifications } from '../services/notifications';
import { AuthContext } from './authContextValue';

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState(null);

  useEffect(() => {
    const unsubscribe = onAuthChanged(async (firebaseUser) => {
      if (firebaseUser) {
        const role = firebaseUser.email === 'admin@ucacue.edu.ec' ? 'ADMIN' : 'USER';
        
        setUser({
          uid: firebaseUser.uid,
          email: firebaseUser.email,
          displayName: firebaseUser.displayName,
          photoURL: firebaseUser.photoURL,
          role,
        });

        const idToken = await getIdToken();
        setToken(idToken);

        localStorage.setItem('user', JSON.stringify({
          email: firebaseUser.email,
          displayName: firebaseUser.displayName,
          role,
          timestamp: Date.now(),
        }));

        initNotifications(firebaseUser.uid).catch(console.error);
      } else {
        setUser(null);
        setToken(null);
        localStorage.removeItem('user');
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const value = {
    user,
    token,
    loading,
    isAuthenticated: !!user,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
