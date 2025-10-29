import { useState, useEffect } from 'react';
import { messaging } from '../services/firebase';
import { getToken, onMessage } from 'firebase/messaging';
import { useToast } from '../hooks/useToast';
import Button from '../components/common/Button';
import Toast from '../components/common/Toast';

const NotificacionesPage = () => {
  const [notifications, setNotifications] = useState([
    { id: 1, title: 'Bienvenido', message: 'Bienvenido al sistema UCACUE Bar', read: false, timestamp: new Date() },
    { id: 2, title: 'Sistema actualizado', message: 'El sistema ha sido actualizado con nuevas funcionalidades', read: false, timestamp: new Date(Date.now() - 3600000) },
  ]);
  const [fcmToken, setFcmToken] = useState(null);
  const [permission, setPermission] = useState(Notification.permission);
  
  const { toasts, success, error, info, removeToast } = useToast();

  useEffect(() => {
    if (messaging && permission === 'granted') {
      requestNotificationPermission();
    }

    if (messaging) {
      const unsubscribe = onMessage(messaging, (payload) => {
        const newNotification = {
          id: Date.now(),
          title: payload.notification.title,
          message: payload.notification.body,
          read: false,
          timestamp: new Date(),
        };
        setNotifications(prev => [newNotification, ...prev]);
        info(`${payload.notification.title}: ${payload.notification.body}`);
      });

      return () => unsubscribe();
    }
  }, [permission]);

  const requestNotificationPermission = async () => {
    try {
      if (!messaging) {
        error('Las notificaciones no están disponibles en este navegador');
        return;
      }

      const permission = await Notification.requestPermission();
      setPermission(permission);

      if (permission === 'granted') {
        const token = await getToken(messaging, {
          vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
        });
        setFcmToken(token);
        success('Notificaciones activadas correctamente');
      } else {
        error('Permiso de notificaciones denegado');
      }
    } catch (err) {
      console.error('Error requesting notification permission:', err);
      error('Error al activar las notificaciones');
    }
  };

  const markAsRead = (id) => {
    setNotifications(prev =>
      prev.map(notif => notif.id === id ? { ...notif, read: true } : notif)
    );
  };

  const markAllAsRead = () => {
    setNotifications(prev => prev.map(notif => ({ ...notif, read: true })));
    success('Todas las notificaciones marcadas como leídas');
  };

  const deleteNotification = (id) => {
    setNotifications(prev => prev.filter(notif => notif.id !== id));
    success('Notificación eliminada');
  };

  const unreadCount = notifications.filter(n => !n.read).length;

  return (
    <div className="p-6 max-w-4xl mx-auto space-y-6">
      <Toast toasts={toasts} onRemove={removeToast} />

      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Notificaciones</h1>
          <p className="text-gray-600">
            {unreadCount > 0 ? `Tienes ${unreadCount} notificación${unreadCount > 1 ? 'es' : ''} sin leer` : 'No tienes notificaciones sin leer'}
          </p>
        </div>
        {unreadCount > 0 && (
          <Button onClick={markAllAsRead} variant="secondary">
            Marcar todas como leídas
          </Button>
        )}
      </div>

      {permission !== 'granted' && (
        <div className="bg-blue-50 border-2 border-blue-200 rounded-xl p-6">
          <div className="flex items-start gap-4">
            <div className="w-12 h-12 bg-blue-500 rounded-full flex items-center justify-center flex-shrink-0">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
            </div>
            <div className="flex-1">
              <h3 className="text-lg font-bold text-gray-900 mb-2">Activa las notificaciones</h3>
              <p className="text-gray-600 mb-4">
                Recibe alertas en tiempo real sobre ventas, inventario bajo y más.
              </p>
              <Button onClick={requestNotificationPermission}>
                Activar Notificaciones
              </Button>
            </div>
          </div>
        </div>
      )}

      {fcmToken && (
        <div className="bg-green-50 border-2 border-green-200 rounded-xl p-4">
          <div className="flex items-center gap-2 text-green-800">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
            </svg>
            <span className="font-semibold">Notificaciones activadas correctamente</span>
          </div>
        </div>
      )}

      <div className="space-y-3">
        {notifications.length === 0 ? (
          <div className="bg-white rounded-xl shadow-md p-12 text-center">
            <div className="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
            </div>
            <h3 className="text-xl font-bold text-gray-900 mb-2">No hay notificaciones</h3>
            <p className="text-gray-600">Cuando recibas notificaciones, aparecerán aquí</p>
          </div>
        ) : (
          notifications.map((notification) => (
            <div
              key={notification.id}
              className={`bg-white rounded-xl shadow-md p-6 transition-all ${
                notification.read ? 'opacity-60' : 'border-l-4 border-brand'
              }`}
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="font-bold text-gray-900">{notification.title}</h3>
                    {!notification.read && (
                      <span className="w-2 h-2 bg-brand rounded-full"></span>
                    )}
                  </div>
                  <p className="text-gray-600 mb-3">{notification.message}</p>
                  <p className="text-xs text-gray-500">
                    {notification.timestamp.toLocaleString()}
                  </p>
                </div>
                <div className="flex gap-2">
                  {!notification.read && (
                    <button
                      onClick={() => markAsRead(notification.id)}
                      className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                      title="Marcar como leída"
                    >
                      <svg className="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                      </svg>
                    </button>
                  )}
                  <button
                    onClick={() => deleteNotification(notification.id)}
                    className="p-2 hover:bg-red-100 text-red-600 rounded-lg transition-colors"
                    title="Eliminar"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default NotificacionesPage;
