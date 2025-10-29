import { useState, useEffect } from 'react';
import { useToast } from '../hooks/useToast';
import Button from '../components/common/Button';
import Toast from '../components/common/Toast';

const AjustesPage = () => {
  const [settings, setSettings] = useState({
    darkMode: false,
    notifications: true,
    soundEffects: true,
    autoBackup: true,
    language: 'es',
  });
  
  const { toasts, success, removeToast } = useToast();

  useEffect(() => {
    const savedSettings = localStorage.getItem('settings');
    if (savedSettings) {
      try {
        setSettings(JSON.parse(savedSettings));
      } catch (e) {
        console.error('Error loading settings:', e);
      }
    }
  }, []);

  useEffect(() => {
    if (settings.darkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [settings.darkMode]);

  const handleToggle = (key) => {
    setSettings(prev => {
      const newSettings = { ...prev, [key]: !prev[key] };
      localStorage.setItem('settings', JSON.stringify(newSettings));
      return newSettings;
    });
    success('Configuración actualizada');
  };

  const handleLanguageChange = (e) => {
    const newSettings = { ...settings, language: e.target.value };
    setSettings(newSettings);
    localStorage.setItem('settings', JSON.stringify(newSettings));
    success('Idioma actualizado');
  };

  const resetSettings = () => {
    if (!confirm('¿Estás seguro de restablecer todas las configuraciones?')) return;
    
    const defaultSettings = {
      darkMode: false,
      notifications: true,
      soundEffects: true,
      autoBackup: true,
      language: 'es',
    };
    setSettings(defaultSettings);
    localStorage.setItem('settings', JSON.stringify(defaultSettings));
    success('Configuraciones restablecidas');
  };

  return (
    <div className="p-6 max-w-4xl mx-auto space-y-6">
      <Toast toasts={toasts} onRemove={removeToast} />

      <div>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Ajustes</h1>
        <p className="text-gray-600">Personaliza tu experiencia en el sistema</p>
      </div>

      <div className="bg-white rounded-xl shadow-md overflow-hidden">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-bold text-gray-900">Apariencia</h2>
        </div>
        <div className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-gray-900">Modo Oscuro</h3>
              <p className="text-sm text-gray-600">Activa el tema oscuro para reducir la fatiga visual</p>
            </div>
            <button
              onClick={() => handleToggle('darkMode')}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                settings.darkMode ? 'bg-brand' : 'bg-gray-200'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  settings.darkMode ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-gray-900">Idioma</h3>
              <p className="text-sm text-gray-600">Selecciona el idioma de la interfaz</p>
            </div>
            <select
              value={settings.language}
              onChange={handleLanguageChange}
              className="px-4 py-2 border-2 border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand"
            >
              <option value="es">Español</option>
              <option value="en">English</option>
            </select>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-md overflow-hidden">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-bold text-gray-900">Notificaciones</h2>
        </div>
        <div className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-gray-900">Notificaciones Push</h3>
              <p className="text-sm text-gray-600">Recibe alertas sobre ventas y eventos importantes</p>
            </div>
            <button
              onClick={() => handleToggle('notifications')}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                settings.notifications ? 'bg-brand' : 'bg-gray-200'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  settings.notifications ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-gray-900">Efectos de Sonido</h3>
              <p className="text-sm text-gray-600">Reproduce sonidos al completar acciones</p>
            </div>
            <button
              onClick={() => handleToggle('soundEffects')}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                settings.soundEffects ? 'bg-brand' : 'bg-gray-200'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  settings.soundEffects ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-md overflow-hidden">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-bold text-gray-900">Sistema</h2>
        </div>
        <div className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-gray-900">Respaldo Automático</h3>
              <p className="text-sm text-gray-600">Guarda automáticamente los datos en la nube</p>
            </div>
            <button
              onClick={() => handleToggle('autoBackup')}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                settings.autoBackup ? 'bg-brand' : 'bg-gray-200'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  settings.autoBackup ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-md p-6">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Información del Sistema</h2>
        <div className="space-y-3">
          <div className="flex justify-between py-3 border-b border-gray-200">
            <span className="text-gray-600">Versión</span>
            <span className="font-semibold text-gray-900">1.0.0</span>
          </div>
          <div className="flex justify-between py-3 border-b border-gray-200">
            <span className="text-gray-600">Última actualización</span>
            <span className="font-semibold text-gray-900">{new Date().toLocaleDateString()}</span>
          </div>
          <div className="flex justify-between py-3">
            <span className="text-gray-600">Entorno</span>
            <span className="font-semibold text-gray-900">Producción</span>
          </div>
        </div>
      </div>

      <div className="flex gap-4">
        <Button onClick={resetSettings} variant="danger">
          Restablecer Configuraciones
        </Button>
      </div>
    </div>
  );
};

export default AjustesPage;
