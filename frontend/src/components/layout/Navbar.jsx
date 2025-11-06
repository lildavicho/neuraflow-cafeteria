import { useAuth } from '../../hooks/useAuth';
import { logout } from '../../services/authService';
import { useNavigate } from 'react-router-dom';
import { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { useWebSocket, useOrderNotifications } from '../../hooks/useWebSocket';
import { getOrders } from '../../services/orders';

const Navbar = ({ onMenuClick }) => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [showDropdown, setShowDropdown] = useState(false);
  const [notificationCount, setNotificationCount] = useState(0);
  const { status } = useWebSocket();

  const loadNotificationCount = useCallback(async () => {
    if (!user) return;
    try {
      const page = await getOrders({ page: 0, size: 50 });
      const list = Array.isArray(page?.content) ? page.content : (Array.isArray(page) ? page : []);
      if (user.role === 'ADMIN') {
        // Backend statuses: PENDING, ACCEPTED, READY, DELIVERED, CANCELLED
        const pending = list.filter((o) => o.status === 'PENDING').length;
        setNotificationCount(pending);
      } else {
        const ready = list.filter((o) => o.status === 'READY').length;
        setNotificationCount(ready);
      }
    } catch (err) {
      console.error('Error loading notification count:', err);
      setNotificationCount(0);
    }
  }, [user]);

  useEffect(() => {
    loadNotificationCount();
  }, [loadNotificationCount]);

  const handleOrderNotification = useCallback(() => {
    loadNotificationCount();
  }, [loadNotificationCount]);

  useOrderNotifications(handleOrderNotification);

  const handleLogout = async () => {
    try {
      await logout();
      navigate('/login');
    } catch (error) {
      console.error('Error logging out:', error);
    }
  };

  return (
    <nav className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-30 shadow-sm">
      <div className="px-4 py-3">
        <div className="flex items-center justify-between">
          <button
            onClick={onMenuClick}
            className="lg:hidden p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-brand/50"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>

          <div className="flex-1 max-w-md mx-4 hidden md:block">
            <div className="relative">
              <i className="bi bi-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none"></i>
              <input
                type="text"
                placeholder="Buscar..."
                className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-brand/40 transition-all"
              />
            </div>
          </div>

          <div className="flex items-center gap-4">
            {status === 'connected' && (
              <span className="flex items-center gap-1 text-sm text-success-600 dark:text-success-400" title="Conectado">
                <i className="bi bi-wifi"></i>
              </span>
            )}
            {status === 'reconnecting' && (
              <span className="flex items-center gap-1 text-sm text-warning-600 dark:text-warning-400" title="Reconectando">
                <i className="bi bi-arrow-repeat animate-spin"></i>
              </span>
            )}
            {status === 'disconnected' && (
              <span className="flex items-center gap-1 text-sm text-danger-600 dark:text-danger-400" title="Desconectado">
                <i className="bi bi-wifi-off"></i>
              </span>
            )}
            
            <button 
              onClick={() => {
                navigate('/notificaciones');
                setNotificationCount(0);
              }}
              className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors relative focus:outline-none focus-visible:ring-2 focus-visible:ring-brand/50"
            >
              <i className="bi bi-bell text-xl text-gray-600 dark:text-gray-400"></i>
              {notificationCount > 0 && (
                <span className="absolute -top-1 -right-1 w-5 h-5 bg-brand text-white text-xs font-bold rounded-full flex items-center justify-center">
                  {notificationCount > 9 ? '9+' : notificationCount}
                </span>
              )}
            </button>

            <div className="relative">
              <button
                onClick={() => setShowDropdown(!showDropdown)}
                className="flex items-center gap-3 p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-brand/50"
              >
                <div className="w-8 h-8 bg-brand rounded-full flex items-center justify-center shadow-md">
                  <span className="text-white font-semibold text-sm">
                    {user?.displayName?.charAt(0)?.toUpperCase() || user?.email?.charAt(0)?.toUpperCase() || 'U'}
                  </span>
                </div>
                <div className="hidden md:block text-left">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{user?.displayName || 'Usuario'}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">{user?.email}</p>
                </div>
                <svg className="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {showDropdown && (
                <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1">
                  <button
                    onClick={() => {
                      navigate('/perfil');
                      setShowDropdown(false);
                    }}
                    className="w-full px-4 py-2 text-left text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand/50"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                    Mi Perfil
                  </button>
                  <button
                    onClick={() => {
                      navigate('/ajustes');
                      setShowDropdown(false);
                    }}
                    className="w-full px-4 py-2 text-left text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand/50"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                    Ajustes
                  </button>
                  <hr className="my-1" />
                  <button
                    onClick={handleLogout}
                    className="w-full px-4 py-2 text-left text-sm text-brand hover:bg-brand/10 dark:hover:bg-brand/20 flex items-center gap-2 font-medium focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand/50"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                    Cerrar Sesión
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;

Navbar.propTypes = {
  onMenuClick: PropTypes.func,
};
