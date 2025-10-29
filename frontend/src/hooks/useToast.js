import { useState, useCallback } from 'react';

export const useToast = () => {
  const [toasts, setToasts] = useState([]);

  const showToast = useCallback((message, type = 'info') => {
    const id = Date.now();
    const newToast = { id, message, type };

    setToasts((prev) => [...prev, newToast]);

    setTimeout(() => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, 4000);
  }, []);

  const success = useCallback((message) => showToast(message, 'success'), [showToast]);
  const error = useCallback((message) => showToast(message, 'error'), [showToast]);
  const info = useCallback((message) => showToast(message, 'info'), [showToast]);
  const warning = useCallback((message) => showToast(message, 'warning'), [showToast]);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  return {
    toasts,
    showToast,
    success,
    error,
    info,
    warning,
    removeToast,
  };
};
